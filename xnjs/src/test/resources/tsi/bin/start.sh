#!/bin/sh

# Read basic settings
. src/test/resources/tsi/conf/startup.properties

#
# check whether the server might be already running
#
if [ -e $PID ] 
 then 
  if [ -d /proc/$(cat $PID) ]
   then
     echo "A UNICORE TSI instance may be already running with process id "$(cat $PID)
     echo "If this is not the case, delete the file $PID and re-run this script"
     exit 1
   fi
fi

PARAM=$*
if [ "$PARAM" = "" ]
then
  PARAM=src/test/resources/tsi/conf/tsi.properties
fi

export PYTHONPATH=${PY}
echo "Logging to ${STARTLOG}"
nohup $PYTHON $PY/TSI.py $PARAM > ${STARTLOG}-startup.log 2>&1  & echo $! > ${PID}

echo "UNICORE TSI starting"
