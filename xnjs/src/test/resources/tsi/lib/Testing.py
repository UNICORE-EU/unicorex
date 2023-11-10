"""
Allows to set various error conditions to test the
XNJS/TSI protocol under bad conditions
"""

import time

def fail_io(configuration, LOG):
    settings = configuration.get("settings", {})
    fail = int(settings.get("fail_io", "0"))
    LOG.info("IO Fail = '%s'" % fail)
    if int(fail)>0:
        raise OSError("IO failure")
