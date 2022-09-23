#!/bin/bash

#
# Startup script for UNICORE Registry

#
@cdInstall@
#
# Read basic configuration parameters
#
. @etc@/startup.properties


# check whether the server might be already running
#
if [ -e $PID ] 
 then 
  if [ -d /proc/$(cat $PID) ]
   then
     echo "A UNICORE Registry instance may be already running with process id "$(cat $PID)
     echo "If this is not the case, delete the file $INST/$PID and re-run this script"
     exit 1
   fi
fi

#
# setup classpath
#
CP=.$(@cdRoot@find "${LIB}" -name "*.jar" -exec printf ":{}" \;)

echo $CP | grep jar > /dev/null
if [ $? != 0 ] 
then
  echo "ERROR: empty classpath, please check that the LIB variable is properly defined."
  exit 1
fi

PARAM=$*
if [ "$PARAM" = "" ]
then
  PARAM=@etc@/registry.config
fi

#
# go
#

CLASSPATH=$CP; export CLASSPATH

nohup ${JAVA} ${MEM} ${OPTS} ${DEFS} eu.unicore.services.USEContainer ${PARAM} REGISTRY > ${STARTLOG} 2>&1  & echo $! > ${PID}

echo "UNICORE shared Registry server starting"
