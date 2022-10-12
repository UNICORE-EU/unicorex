#!/bin/bash

#
# Startup script for UNICORE/X
#
@cdInstall@
#
# Read basic configuration parameters
#
. @etc@/startup.properties


#
# check whether the server might be already running
#
if [ -e $PID ] 
 then 
  if [ -d /proc/$(cat $PID) ]
   then
     echo "A UNICORE/X instance may be already running with process id "$(cat $PID)
     echo "If this is not the case, delete the file $PID and re-run this script"
     exit 1
   fi
fi

#
# setup classpath
#
CP=.$(@cdRoot@find "$LIB" -name "*.jar" -exec printf ":{}" \;)

if [ "$PARAM" = "" ]
then
  PARAM=${CONF}/unicorex.config
fi
SERVERNAME=${SERVERNAME:-"UNICOREX"}

#
# go
#

CLASSPATH=$CP; export CLASSPATH

nohup $JAVA ${MEM} ${OPTS} ${DEFS} de.fzj.unicore.uas.UAS ${PARAM} ${SERVERNAME} > ${STARTLOG} 2>&1  & echo $! > ${PID}

echo "UNICORE/X starting"

