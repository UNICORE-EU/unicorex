#!/bin/bash

#
# Check status of UNICORE/X
#

@cdInstall@
#
# Read basic configuration parameters
#
. @etc@/startup.properties

# service name
SERVICE=unicorex

# PID file
PID=/var/run/unicore/unicorex.pid

if [ ! -e $PID ]
then
 echo "UNICORE/X not running (no PID file)"
 exit 7
fi

PIDV=$(cat $PID)

if ps axww | grep -v grep | grep $PIDV > /dev/null 2>&1 ; then
 echo "UNICORE/X running with PID ${PIDV}"
 exit 0
fi

#else not running, but PID found
echo "warn: UNICORE/X not running, but PID file $PID found"
exit 3

