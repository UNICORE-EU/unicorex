#!/bin/bash
# Client script for executing a UNICORE UFTP data transfer

dir=`dirname $0`
if [ "$dir" != "." ]
then
  INST=`dirname $dir`
  else
  pwd | grep -e 'bin$' > /dev/null
  if [ $? = 0 ]
then
    INST=".."
  else
    INST=`dirname $dir`
  fi
fi

INST=${INST:-.}

JAVA=java

CP=$(cat $INST/../../../target/uftp.classpath)
CLASSPATH=$CP; export CLASSPATH

#
# go
#
$JAVA -Xmx128m  eu.unicore.uftp.client.UFTPClient ${1+"$@"}
