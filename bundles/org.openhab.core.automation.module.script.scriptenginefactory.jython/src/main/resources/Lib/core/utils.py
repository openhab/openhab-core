# pylint: disable=invalid-name
"""
This module provides miscellaneous utility functions that are used across the core packages and modules.
"""
__all__ = [
    "kw", "iround", "getItemValue", "getLastUpdate", "sendCommand",
    "postUpdate", "post_update_if_different", "postUpdateCheckFirst",
    "send_command_if_different", "sendCommandCheckFirst",
    "validate_channel_uid", "validate_uid"
]

import re
import uuid

try:
    from org.eclipse.smarthome.core.types import TypeParser
except:
    from org.openhab.core.types import TypeParser

try:
    from org.openhab.core.thing import ChannelUID
except:
    from org.eclipse.smarthome.core.thing import ChannelUID

from org.joda.time import DateTime

from core.log import logging, LOG_PREFIX
from core.jsr223.scope import itemRegistry, NULL, UNDEF, ON, OFF, OPEN, CLOSED, events, things
from core.actions import PersistenceExtensions

LOG = logging.getLogger("{}.core.utils".format(LOG_PREFIX))

def kw(dictionary, value):
    """
    In a given dictionary, get the first key that has a value matching the one provided.

    Args:
        dict (dict): the dictionary to search
        value (str): the value to match to a key

    Returns:
        str or None: string representing the first key with a matching vlaue, or
            None if the value is not found
    """
    for key, value in dictionary.iteritems():
        if value == value:
            return key
    return None

def iround(float_value):
    """
    Round a float to the nearest integer.

    Args:
        x (float): the float to round

    Returns:
        integer: integer value of float
    """
    rounded = round(float_value) - .5
    return int(rounded) + (rounded > 0)

def getItemValue(item_or_item_name, default_value):
    """
    Returns the Item's value if the Item exists and is initialized, otherwise
    returns the default value. ``itemRegistry.getItem`` will return an object
    for uninitialized items, but it has less methods. ``itemRegistry.getItem``
    will throw an ItemNotFoundException if the Item is not in the registry.

    Args:
        item_or_item_name (Item or str): name of the Item
        default_value (int, float, ON, OFF, OPEN, CLOSED, str, DateTime): the default
            value

    Returns:
        int, float, ON, OFF, OPEN, CLOSED, str, DateTime, or None: the state if
            the Item converted to the type of default value, or the default
            value if the Item's state is NULL or UNDEF
    """
    item = itemRegistry.getItem(item_or_item_name) if isinstance(item_or_item_name, basestring) else item_or_item_name
    if isinstance(default_value, int):
        return item.state.intValue() if item.state not in [NULL, UNDEF] else default_value
    elif isinstance(default_value, float):
        return item.state.floatValue() if item.state not in [NULL, UNDEF] else default_value
    elif default_value in [ON, OFF, OPEN, CLOSED]:
        return item.state if item.state not in [NULL, UNDEF] else default_value
    elif isinstance(default_value, str):
        return item.state.toFullString() if item.state not in [NULL, UNDEF] else default_value
    elif isinstance(default_value, DateTime):
        # We return a org.joda.time.DateTime from a org.eclipse.smarthome.core.library.types.DateTimeType
        return DateTime(item.state.calendar.timeInMillis) if item.state not in [NULL, UNDEF] else default_value
    else:
        LOG.warn("The type of the passed default value is not handled")
        return None

def getLastUpdate(item_or_item_name):
    """
    Returns the Item's last update datetime as an 'org.joda.time.DateTime <http://joda-time.sourceforge.net/apidocs/org/joda/time/DateTime.html>`_.

    Args:
        item_or_item_name (Item or str): name of the Item

    Returns:
        DateTime: DateTime representing the time of the Item's last update
    """
    try:
        item = itemRegistry.getItem(item_or_item_name) if isinstance(item_or_item_name, basestring) else item_or_item_name
        last_update = PersistenceExtensions.lastUpdate(item)
        if last_update is None:
            LOG.warning("No existing lastUpdate data for item: [{}], so returning 1970-01-01T00:00:00Z".format(item.name))
            return DateTime(0)
        return last_update.toDateTime()
    except:
        # There is an issue using the StartupTrigger and saving scripts over SMB, where changes are detected before the file
        # is completely written. The first read breaks because of a partial file write and the second read succeeds.
        LOG.warning("Exception when getting lastUpdate data for item: [{}], so returning 1970-01-01T00:00:00Z".format(item.name))
        return DateTime(0)

def sendCommand(item_or_item_name, new_value):
    """
    Sends a command to an item regardless of its current state.

    Args:
        item_or_item_name (Item or str): name of the Item
        new_value (Command): Command to send to the Item
    """
    item = itemRegistry.getItem(item_or_item_name) if isinstance(item_or_item_name, basestring) else item_or_item_name
    events.sendCommand(item, new_value)

def postUpdate(item_or_item_name, new_value):
    """
    Posts an update to an item regardless of its current state.

    Args:
        item_name (Item or str): Item or name of the Item
        new_value (State): state to update the Item with
    """
    item = itemRegistry.getItem(item_or_item_name) if isinstance(item_or_item_name, basestring) else item_or_item_name
    events.postUpdate(item, new_value)

def post_update_if_different(item_or_item_name, new_value, sendACommand=False, floatPrecision=None):
    """
    Checks if the current state of the item is different than the desired new
    state. If the target state is the same, no update is posted.

    sendCommand vs postUpdate:
    If you want to tell something to change (turn a light on, change the
    thermostat to a new temperature, start raising the blinds, etc.), then you
    want to send a command to an Item using sendCommand. If your Items' states
    are not being updated by a binding, the autoupdate feature or something
    else external, you will probably want to update the state in a rule using
    postUpdate.

    Unfortunately, most decimal fractions cannot be represented exactly as
    binary fractions. A consequence is that, in general, the decimal
    floating-point numbers you enter are only approximated by the binary
    floating-point numbers actually stored in the machine. Therefore,
    comparing the stored value with the new value will most likely always
    result in a difference. You can supply the named argument floatPrecision
    to round the value before comparing.

    Args:
        item_or_item_name (Item or str): name of the Item
        new_value (State or Command): state to update the Item with, or Command
            if using sendACommand (must be of a type supported by the Item)
        sendACommand (Boolean): (optional) ``True`` to send a command instead
            of an update
        floatPrecision (int): (optional) the precision of the Item's state to
            use when comparing values

    Returns:
        bool: ``True``, if the command or update was sent, else ``False``
    """
    compare_value = None
    item = itemRegistry.getItem(item_or_item_name) if isinstance(item_or_item_name, basestring) else item_or_item_name

    if sendACommand:
        compare_value = TypeParser.parseCommand(item.acceptedCommandTypes, str(new_value))
    else:
        compare_value = TypeParser.parseState(item.acceptedDataTypes, str(new_value))

    if compare_value is not None:
        if item.state != compare_value or (isinstance(new_value, float) and floatPrecision is not None and round(item.state.floatValue(), floatPrecision) != new_value):
            if sendACommand:
                sendCommand(item, new_value)
                LOG.debug("New sendCommand value for [{}] is [{}]".format(item.name, new_value))
            else:
                postUpdate(item, new_value)
                LOG.debug("New postUpdate value for [{}] is [{}]".format(item.name, new_value))
            return True
        else:
            LOG.debug("Not {} {} to {} since it is the same as the current state".format("sending command" if sendACommand else "posting update", new_value, item.name))
            return False
    else:
        LOG.warn("[{}] is not an accepted {} for [{}]".format(new_value, "command type" if sendACommand else "state", item.name))
        return False

# backwards compatibility
postUpdateCheckFirst = post_update_if_different

def send_command_if_different(item_or_item_name, new_value, floatPrecision=None):
    """
    See postUpdateCheckFirst
    """
    return postUpdateCheckFirst(item_or_item_name, new_value, sendACommand=True, floatPrecision=floatPrecision)

# backwards compatibility
sendCommandCheckFirst = send_command_if_different

def validate_item(item_or_item_name):
    """
    This function validates whether an Item exists or if an Item name is valid.

    Args:
        item_or_item_name (Item or str): name of the Item

    Returns:
        Item or None: None, if the Item does not exist or the Item name is not
        in a valid format, else validated Item
    """
    item = item_or_item_name
    if isinstance(item, basestring):
        if itemRegistry.getItems(item) == []:
            LOG.warn("[{}] is not in the ItemRegistry".format(item))
            return None
        else:
            item = itemRegistry.getItem(item_or_item_name)
    elif not hasattr(item_or_item_name, 'name'):
        LOG.warn("[{}] is not a Item or string".format(item))
        return None

    if itemRegistry.getItems(item.name) == []:
        LOG.warn("[{}] is not in the ItemRegistry".format(item.name))
        return None

    return item

def validate_channel_uid(channel_uid_or_string):
    """
    This function validates whether a ChannelUID exists or if a ChannelUID is
        valid.

    Args:
        channel_uid_or_string (ChannelUID or string): the ChannelUID

    Returns:
        ChannelUID or None: None, if the ChannelUID does not exist or the
        ChannelUID is not in a valid format, else validated ChannelUID
    """
    channel_uid = channel_uid_or_string
    if isinstance(channel_uid_or_string, basestring):
        channel_uid = ChannelUID(channel_uid_or_string)
    elif not isinstance(channel_uid_or_string, ChannelUID):
        LOG.warn("[{}] is not a string or ChannelUID".format(channel_uid_or_string))
        return None
    if things.getChannel(channel_uid) is None:
        LOG.warn("[{}] is not a valid Channel".format(channel_uid))
        return None
    return channel_uid

def validate_uid(uid):
    """
    This function validates UIDs.

    Args:
        uid (string or None): the UID to validate or None

    Returns:
        string: a valid UID
    """
    if uid is None:
        uid = uuid.uuid1().hex
    else:
        uid = re.sub(r"[^A-Za-z0-9_-]", "_", uid)
        uid = "{}_{}".format(uid, uuid.uuid1().hex)
    if not re.match("^[A-Za-z0-9]", uid):# in case the first character is still invalid
        uid = "{}_{}".format("jython", uid)
    uid = re.sub(r"__+", "_", uid)
    return uid
