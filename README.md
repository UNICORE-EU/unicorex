# UNICORE/X

[![Unit tests](https://github.com/UNICORE-EU/unicorex/actions/workflows/maven.yml/badge.svg)](https://github.com/UNICORE-EU/unicorex/actions/workflows/maven.yml)

This repository contains the source code for UNICORE/X, which is the
central component of a typical UNICORE installation.
UNICORE/X provides REST APIs for job management and data access
services for a single compute cluster (or just a file system).

## Download

UNICORE/X is distributed as part of the "Core Server" bundle and can be
[downloaded from GitHub](https://github.com/UNICORE-EU/server-bundle/releases)

## Documentation

See the [UNICORE/X manual](https://unicore-docs.readthedocs.io/en/latest/admin-docs/unicorex/index.html)

## Building from source

You need Java and Apache Maven.

The Java code is built and unit tested using

    mvn install

To skip unit testing

    mvn install -DskipTests

The following commands create distribution packages
in tgz, deb and rpm formats

Do a `cd unicorex-dist` or `cd registry-dist` for UNICORE/X or
Registry.


 * tgz

    mvn package -DskipTests -Ppackman -Dpackage.type=bin.tar.gz

 * deb

    mvn package -DskipTests -Ppackman -Dpackage.type=deb -Ddistribution=Debian

 * rpm

    mvn package -DskipTests -Ppackman -Dpackage.type=rpm -Ddistribution=RedHat

