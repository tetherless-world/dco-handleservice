# dco-handleservice

In order to get the handle library to be included I had to do the following:

mvn install:install-file -Dfile=/Users/westp/dco/handle/hcj7.3.1/handle-client.jar -DgroupId=net.handle -DartifactId=hdllib -Dversion=7.3.1 -Dpackaging=jar
