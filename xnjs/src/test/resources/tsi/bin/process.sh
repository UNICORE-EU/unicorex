#!/bin/sh

# Read basic settings
. src/test/resources/tsi/conf/startup.properties

PARAM=$*
if [ "$PARAM" = "" ]
then
  PARAM=src/test/resources/tsi/conf/tsi.properties
fi

export PYTHONPATH=${PY}
$PYTHON $PY/Runner.py $PARAM

