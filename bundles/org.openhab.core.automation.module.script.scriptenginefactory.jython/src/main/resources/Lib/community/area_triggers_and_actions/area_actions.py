"""
The ``area_actions`` module contains the ``light_action``  and
``toggle_action`` functions that should be useable by everyone without
customization. Custom actions should not be put into this file, as they could
be overwritten during an upgrade. Instead, place them in the
``personal.area_triggers_and_actions.area_actions`` module.
"""
__all__ = ['light_action', 'toggle_action']

from core.jsr223.scope import events, items, PercentType, DecimalType, HSBType, ON, OFF
from core.metadata import get_key_value
from core.log import logging, LOG_PREFIX

import configuration
reload(configuration)
from configuration import area_triggers_and_actions_dict

#from org.joda.time import DateTime

log = logging.getLogger("{}.community.area_triggers_and_actions.area_actions".format(LOG_PREFIX))

def light_action(item, active):
    """
    This function performs an action on a light Item.

    When called, this function pulls in the metadata for the supplied Item or
    uses the default values specified in
    ``configuration.area_triggers_and_actions_dict"["default_levels"]``, if the
    metadata does not exist. This metadata is then compared to the current lux
    level to determine if a light should be turned OFF or set to the specified
    level. This function should work for everyone without modification.

    Args:
        Item item: The Item to perform the action on
        boolean active: Area activity (True for active and False for inactive)
    """
    #start_time = DateTime.now().getMillis()
    item_metadata = get_key_value(item.name, "area_triggers_and_actions", "modes", str(items["Mode"]))
    low_lux_trigger = item_metadata.get("low_lux_trigger", area_triggers_and_actions_dict["default_levels"]["low_lux_trigger"])
    hue = DecimalType(item_metadata.get("hue", area_triggers_and_actions_dict["default_levels"]["hue"]))
    saturation = PercentType(str(item_metadata.get("saturation", area_triggers_and_actions_dict["default_levels"]["saturation"])))
    brightness = PercentType(str(item_metadata.get("brightness", area_triggers_and_actions_dict["default_levels"]["brightness"])))
    #log.warn("light_action: item.name [{}], active [{}], brightness [{}], lux [{}], low_lux_trigger [{}]".format(item.name, active, brightness, items[area_triggers_and_actions_dict["lux_item_name"]], low_lux_trigger))
    lux_item_name = get_key_value(item.name, "area_triggers_and_actions", "light_action", "lux_item_name") or area_triggers_and_actions_dict.get("lux_item_name")
    if active and brightness > PercentType(0) and (True if lux_item_name is None else items[lux_item_name].intValue() <= low_lux_trigger):
        if item.type == "Dimmer" or (item.type == "Group" and item.baseItem.type == "Dimmer"):
            if item.state != brightness:
                if item.state < PercentType(99):
                    events.sendCommand(item, brightness)
                    log.info(">>>>>>> {}: {}".format(item.name, brightness))
                else:
                    log.info("[{}]: dimmer was manually set > 98, so not adjusting".format(item.name))
            else:
                log.debug("[{}]: dimmer is already set to [{}], so not sending command".format(item.name, brightness))
        elif item.type == "Color" or (item.type == "Group" and item.baseType == "Color"):
            if item.state != HSBType(hue, saturation, brightness):
                if item.state.brightness < PercentType(99):
                    events.sendCommand(item, HSBType(hue, saturation, brightness))
                    log.info(">>>>>>> {}: [{}]".format(item.name, HSBType(hue, saturation, brightness)))
                else:
                    log.info("[{}]: brightness was manually set > 98, so not adjusting".format(item.name))
            else:
                log.debug("[{}]: color is already set to [{}, {}, {}], so not sending command".format(item.name, hue, saturation, brightness))
        elif item.type == "Switch" or (item.type == "Group" and item.baseItem.type == "Switch"):
            if item.state == OFF:
                events.sendCommand(item, ON)
                log.info(">>>>>>> {}: ON".format(item.name))
            else:
                log.debug("[{}]: switch is already [ON], so not sending command".format(item.name))
    else:
        if item.type == "Dimmer" or (item.type == "Group" and item.baseItem.type == "Dimmer"):
            if item.state != PercentType(0):
                if item.state < PercentType(99):
                    events.sendCommand(item, PercentType(0))
                    log.info("<<<<<<<<<<<<<<<<<<<<< {}: 0".format(item.name))
                else:
                    log.info("{}: dimmer was manually set > 98, so not adjusting".format(item.name))
            else:
                log.debug("[{}]: dimmer is already set to [0], so not sending command".format(item.name))
        elif item.type == "Color" or (item.type == "Group" and item.baseType == "Color"):
            if item.state != HSBType(DecimalType(0), PercentType(0), PercentType(0)):
                if item.state.brightness < PercentType(99):
                    events.sendCommand(item, "0, 0, 0")
                    log.info("<<<<<<<<<<<<<<<<<<<<< {}: [0, 0, 0]".format(item.name))
                else:
                    log.info("{}: brightness was manually set > 98, so not adjusting".format(item.name))
            else:
                log.debug("[{}]: color is already set to [0, 0, 0], so not sending command".format(item.name))
        elif item.type == "Switch" or (item.type == "Group" and item.baseItem.type == "Switch"):
            if item.state == ON:
                events.sendCommand(item, OFF)
                log.info("<<<<<<<<<<<<<<<<<<<<< {}: OFF".format(item.name))
            else:
                log.debug("[{}]: switch is already set to [OFF], so not sending command".format(item.name))
    #log.warn("Test: light_action: {}: [{}]: time=[{}]".format(item.name, "ON" if active else "OFF", DateTime.now().getMillis() - start_time))

def toggle_action(item, active):
    """
    This function sends the OFF command to the Item.

    Args:
        Item item: The Item to perform the action on
        boolean active: Area activity (True for active and False for inactive)
    """
    events.sendCommand(item, ON if item.state == OFF else OFF)
