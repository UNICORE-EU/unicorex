#
# Building UNICORE/X and Registry
#

You need Java and Apache Maven.


The Java code is built and unit tested using

  mvn install

To skip unit testing and save lots of time:

  mvn install -DskipTests

#
# Creating documentation
#

For UNICORE/X, do

  cd unicorex-dist

and check that the versions in pom.xml are OK. The manual sources
are asciidoc txt files in src/doc, some parts are included from
the general USE doumentation.

To build the docs:

  mvn site

You can check them by pointing a web browser at 
"target/site/index.html"

To upload the docs to the unicore-dev documentation server:

  mvn site:deploy

For the registry it is the same, only in the "registry-dist" folder.

#
# Creating distribution packages
#

The following commands create the distribution packages
in tgz, deb and rpm formats

Do a "cd unicorex-dist" or "cd registry-dist" for UNICORE/X or
Registry.

The versions are again defined in the pom.xml file!

#tgz
  mvn package -DskipTests -Ppackman -Dpackage.type=bin.tar.gz

#deb
  mvn package -DskipTests -Ppackman -Dpackage.type=deb -Ddistribution=Debian

#rpm
  mvn package -DskipTests -Ppackman -Dpackage.type=rpm -Ddistribution=RedHat



