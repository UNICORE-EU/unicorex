#Check status of UNICORE Registry
#
# service name
SERVICE=REGISTRY

@cdInstall@
#
# Read basic settings
#
. @etc@/startup.properties


if [ ! -e $PID ]
then
 echo "UNICORE ${SERVICE} not running (no PID file)"
 exit 7
fi

PIDV=$(cat $PID)

if ps axww | grep -v grep | grep $PIDV | grep $SERVICE > /dev/null 2>&1 ; then
 echo "UNICORE service ${SERVICE} running with PID ${PIDV}"
 exit 0
fi

#else not running, but PID file found
echo "warn: UNICORE service ${SERVICE} not running, but PID file $PID found"
exit 3

