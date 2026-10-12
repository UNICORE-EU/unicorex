"""
Reservation (dummy for testing)

Check the manual for advice on how to create a custom version.
"""
import Utils

def get_variant():
    return "dummy"


def init(config, LOG):
    pass


def make_reservation(message, connector, config, LOG):
    """ Make a reservation """
    connector.ok("1234")


def query_reservation(message, connector, config, LOG):
    """ Query a reservation """
    id = Utils.extract_parameter(message, "RESERVATION_REFERENCE")
    if "1234"==id:
        connector.ok("WAITING")
    else:
        connector.failed("No such reservation!")


def cancel_reservation(message, connector, config, LOG):
    """ Cancel a reservation """
    id = Utils.extract_parameter(message, "RESERVATION_REFERENCE")
    if "1234"==id:
        connector.ok()
    else:
        connector.failed("No such reservation!")
