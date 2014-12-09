# Amazon S3/Google Developer Storage Maven Wagon Support

You can use Amazon S3 or Google Developer Storage as a maven repository with this plugin.

THIS PROJECT IS A FORKED VERSION OF THE SPRING PROJECT git://git.springsource.org/spring-build/aws-maven.git

The main works comes from the Spring version. All the credit is theirs. Thank you.

Master branch is the original Spring branch from git://git.springsource.org/spring-build/aws-maven.git

Anzix branch is my developmebt branch.

## Usage

### Add the plugin as extension to the pom.xml


```
<build>
   <extensions>
      <extension>
         <groupId>net.anzix.aws</groupId>
         <artifactId>s3-maven-wagon</artifactId>
         <version>3.3</version>
      </extension>
  </extensions>
</build>

```

### Define the distributionManagement url in the pom.xml

```
<distributionManagement>
   <repository>
      <id>s3</id>
      <url>s3://anzix.net@commondatastorage.googleapis.com</url>
   </repository>
</distributionManagement>
```
Format of the URL: `protocol://bucket_name/prefix` or
`protocol://bucket_name@api_endpoint/prefix`

* Protocol is s3 (Amazon AWS S3) or gs (for Google Cloud Storage)
* The /prefix part is optional
* The storage type (Amazon or Google) could be forced by defining the
  server part (`@s3.amazonaws.com` or `@commondatastorage.googleapis.com`)

### define shared and secret key

Shared and secret key could be defined in the ~/.m2/settings.xml as username
and password:

```
<server>
   <id>s3</id>
   <username>qui2ohWo4cip</username>
   <password>thi7ooLeeCu8xuiquavo6pad</password>
</server>


```

## CHANGE LOG

3.3

+ google cloud storage could be used as gs://bucket_name

3.2

+ Google Developer Storage support (use s3://bucket_name@commondatastorage.googleapis.com/prefix)
+ support for root repository (s3://bucket_name@provider without any prefix)
+ remove spring repositories (jets3t is in the centreal)
+ update jets3t version to 8.0
+ support both passphrase and password tag in settings.xml / server
+ improved error messages
