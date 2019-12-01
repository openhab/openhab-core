"""
The ``area_triggers_and_actions`` package provides a mechanism for using group
logic to trigger rules and then perform a particular action.

This package provides the following modules:

* ``area_actions``
"""
__all__ = ['start_action', 'stop_timer']

from threading import Timer

from core.jsr223.scope import ON, OFF, OPEN, CLOSED
from core.metadata import get_key_value
from core.log import logging, LOG_PREFIX, log_traceback

from community.area_triggers_and_actions.area_actions import *

try:
    import sys
    import personal.area_triggers_and_actions.area_actions
    reload(sys.modules['personal.area_triggers_and_actions.area_actions'])
    from personal.area_triggers_and_actions.area_actions import *
except:
    pass

#from org.joda.time import DateTime

log = logging.getLogger("{}.community.area_triggers_and_actions".format(LOG_PREFIX))

timer_dict = {}

@log_traceback
def _timer_function(item, active, function_name, timer_type, timer_delay, recurring, function):
    """This is the function called by the timers."""
    #log.warn("_timer_function: item.name [{}], active [{}], function_name [{}], timer_type [{}], timer_delay [{}], recurring [{}], function [{}]".format(item.name, active, function_name, timer_type, timer_delay, recurring, function))
    function(item, active)
    log.debug("{}: [{}] second {} {} timer has completed".format(item.name, timer_delay, function_name, timer_type))
    if recurring and item.state in [ON, OPEN] if active else item.state in [OFF, CLOSED]:
        timer_dict.update({item.name: {function_name: {timer_type: Timer(timer_delay, _timer_function, [item, active, function_name, timer_type, timer_delay, recurring, function])}}})
        timer_dict[item.name][function_name][timer_type].start()
        log.debug("{}: [{}] second recurring {} {} timer has started".format(item.name, timer_delay, function_name, timer_type))

def start_action(item, active, function_name):
    """
    This is the function called by the rule to begin the selected action,
    which may be first passed through a timer.

    Args:
        item Item: The Item to perform the action on
        active boolean: Area activity (True for active and False for inactive)
        function_name string: Name of the action function
    """
    #start_time = DateTime.now().getMillis()
    timer_type = "ON" if active else "OFF"
    function = globals()[function_name]
    function_metadata = get_key_value(item.name, "area_triggers_and_actions", "actions", function_name)
    limited = function_metadata.get("limited")
    timer_metadata = function_metadata.get(timer_type, {})
    if not limited or timer_metadata:
        timer_delay = timer_metadata.get("delay")
        recurring = timer_metadata.get("recurring")
        #log.warn("start_action: item.name [{}], active [{}], function_name [{}], timer_type [{}], timer_delay [{}], recurring [{}], function [{}]".format(item.name, active, function_name, timer_type, timer_delay, recurring, function))
        if not timer_delay:
            function(item, active)
        elif timer_dict.get(item.name, {}).get(function_name, {}).get(timer_type) is None or not timer_dict[item.name][function_name][timer_type].isAlive():# if timer does not exist, create it
            timer_dict.update({item.name: {function_name: {timer_type: Timer(timer_delay, _timer_function, [item, active, function_name, timer_type, timer_delay, recurring, function])}}})
            timer_dict[item.name][function_name][timer_type].start()
            log.debug("{}: [{}] second {}{} {} timer has started".format(item.name, timer_delay, "recurring " if recurring else "", function_name, timer_type))
    stop_timer(item.name, function_name, "OFF" if active else "ON")
    #log.warn("Test: start_action: {}: [{}]: time=[{}]".format(item.name, timer_type, DateTime.now().getMillis() - start_time))

def stop_timer(item_name, function_name, timer_type):
    """This function stops the timer."""
    #log.warn("stop_timer: function_name [{}], timer_type [{}], item_name [{}]".format(function_name, timer_type, item_name))
    if timer_dict.get(item_name, {}).get(function_name, {}).get(timer_type) is not None and timer_dict[item_name][function_name][timer_type].isAlive():# if timer exists, stop it
        timer_dict[item_name][function_name][timer_type].cancel()
        log.debug("{}: {} {} timer has been cancelled".format(item_name, function_name, timer_type))
