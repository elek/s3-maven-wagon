/*
 * Copyright 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.anzix.aws.maven;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.jets3t.service.ServiceException;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.impl.rest.httpclient.RestStorageService;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.security.GSCredentials;


/**
 * An implementation of the Maven Wagon interface that allows you to access the
 * Amazon S3 service. URLs that reference the S3 service should be in the form
 * of <code>s3://bucket.name</code>. As an example
 * <code>s3://static.springframework.org</code> would put files into the
 * <code>static.springframework.org</code> bucket on the S3 service. <p/> This
 * implementation uses the <code>username</code> and <code>passphrase</code>
 * portions of the server authentication metadata for credentials.
 * 
 * @author Ben Hale
 */
public class SimpleStorageServiceWagon extends AbstractWagon {

    private String AMAZON_URL = "s3.amazonaws.com";

    private String GOOGLE_URL = "commondatastorage.googleapis.com";

    private RestStorageService service;

    private String bucket;

    private String basedir;

    public SimpleStorageServiceWagon() {
        super(false);
    }

    protected void connectToRepository(Repository source, AuthenticationInfo authenticationInfo, ProxyInfoProvider proxyInfoProvider)
            throws AuthenticationException {
        try {
            String provider;
            String protocol = source.getProtocol();
            bucket = source.getUsername();
            if (bucket == null || "".equals(bucket)) {
                bucket = source.getHost();
                if (source != null && protocol.equals("s3")) {
                    provider = AMAZON_URL;
                } else if (source != null && protocol.equals("gs")) {
                    provider = GOOGLE_URL;
                } else {
                    throw new IllegalArgumentException("Internal error. The protocol should be s3: or gs:. Not " + protocol);
                }
            } else {
                provider = source.getHost();
            }

            Credentials c = getCredentials(authenticationInfo);
            if (AMAZON_URL.equals(provider)) {
                service = new RestS3Service(new AWSCredentials(c.access, c.secret));
            } else if (GOOGLE_URL.equals(provider)) {
                service = new GoogleStorageService(new GSCredentials(c.access, c.secret));
            } else {
                throw new IllegalArgumentException("Private Clouds not supported yet. Use s3://bucketname@" + AMAZON_URL + " or gs://bucketname@" + GOOGLE_URL);
            }

        } catch (ServiceException e) {
            throw new AuthenticationException("Cannot authenticate with current credentials", e);
        }
        basedir = getBaseDir(source);
    }

    protected boolean doesRemoteResourceExist(String resourceName) {
        try {
            service.getObjectDetails(bucket, basedir + resourceName);
        } catch (ServiceException e) {
            return false;
        }
        return true;
    }

    protected void disconnectFromRepository() {
        // Nothing to do for S3
    }

    protected void getResource(String resourceName, File destination, TransferProgress progress)
            throws ResourceDoesNotExistException, ServiceException, IOException {
        StorageObject object;
        try {
            object = service.getObject(bucket, basedir + resourceName);
        } catch (ServiceException e) {
            throw new ResourceDoesNotExistException("Resource " + resourceName + " does not exist in the repository", e);
        }

        if (!destination.getParentFile().exists()) {
            destination.getParentFile().mkdirs();
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = object.getDataInputStream();
            out = new TransferProgressFileOutputStream(destination, progress);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Nothing possible at this point
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Nothing possible at this point
                }
            }
        }
    }

    protected boolean isRemoteResourceNewer(String resourceName, long timestamp) throws ServiceException {
        StorageObject object = service.getObjectDetails(bucket, basedir + resourceName);
        return object.getLastModifiedDate().compareTo(new Date(timestamp)) < 0;
    }

    protected List<String> listDirectory(String directory) throws Exception {
        StorageObject[] objects = service.listObjects(bucket, basedir + directory, "");
        List<String> fileNames = new ArrayList<String>(objects.length);
        for (StorageObject object : objects) {
            fileNames.add(object.getKey());
        }
        return fileNames;
    }

    protected void putResource(File source, String destination, TransferProgress progress) throws ServiceException,
            IOException {
        buildDestinationPath(getDestinationPath(destination));
        StorageObject object = new StorageObject(basedir + destination);
        object.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
        object.setDataInputFile(source);
        object.setContentLength(source.length());

        InputStream in = null;
        try {
            service.putObject(bucket, object);

            in = new FileInputStream(source);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                progress.notify(buffer, length);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Nothing possible at this point
                }
            }
        }
    }

    private void buildDestinationPath(String destination) throws ServiceException {
        StorageObject object = new StorageObject(basedir + destination + "/");
        object.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
        object.setContentLength(0);
        service.putObject(bucket, object);
        int index = destination.lastIndexOf('/');
        if (index != -1) {
            buildDestinationPath(destination.substring(0, index));
        }
    }

    private String getDestinationPath(String destination) {
        return destination.substring(0, destination.lastIndexOf('/'));
    }

    private String getBaseDir(Repository source) {
        StringBuilder sb = new StringBuilder(source.getBasedir());
        sb.deleteCharAt(0);
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '/') {
            sb.append('/');
        }
        return sb.toString();
    }

    private Credentials getCredentials(AuthenticationInfo authenticationInfo) throws AuthenticationException {
        if (authenticationInfo == null) {
            String example = "<server>\n"
                    + "   <id>repo_key</id>\n"
                    + "   <username>access_key</username>\n"
                    + "   <password>secret_key</password>\n"
                    + "</server>";
            throw new AuthenticationException("Missing authentication info. Add a \n" + example + "\n to your settings.xml!");
        }
        String accessKey = authenticationInfo.getUserName();
        String secretKey = authenticationInfo.getPassphrase();
        if ("".equals(secretKey)) {
            throw new AuthenticationException("With maven3 you should encrypt the secretKey (see http://maven.apache.org/guides/mini/guide-encryption.html) or use the password field.");
        }
        if (secretKey == null) {
            secretKey = authenticationInfo.getPassword();
        }

        if (accessKey == null || secretKey == null) {
            throw new AuthenticationException("S3 requires a username and passphrase to be set.");
        }
        return new Credentials(accessKey, secretKey);
    }

    private class Credentials {

        public String access;

        public String secret;

        public Credentials(String access, String secret) {
            this.access = access;
            this.secret = secret;
        }
    }
}
