Amazon S3/Google Developer Storage Maven Wagon Support

You can use Amazon S3 or Google Developer Storage as a maven repository with this plugin.

THIS PROJECT IS A FORKED VERSION OF THE SPRING PROJECT git://git.springsource.org/spring-build/aws-maven.git

The main works is come from Spring version. All the credit is theirs. Thank you.

Master branch is the original Spring branch from git://git.springsource.org/spring-build/aws-maven.git

Anzix branch is my developmebt branch.

For usage see: http://blog.anzix.net/2010/12/07/using-amazon-s3-as-a-maven-repository/ (it's about the Spring version)


CHANGE LOG:

3.2

+ Google Developer Storage support (use s3://bucket_name@commondatastorage.googleapis.com/prefix)
+ support for root repository (s3://bucket_name@provider without any prefix)
+ remove spring repositories (jets3t is in the centreal)
+ update jets3t version to 8.0
+ support both passphrase and password tag in settings.xml / server
+ improved error messages