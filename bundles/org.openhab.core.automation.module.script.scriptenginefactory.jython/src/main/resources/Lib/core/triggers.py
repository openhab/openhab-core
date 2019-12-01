# pylint: disable=wrong-import-position, dangerous-default-value
"""
This module includes function decorators and Trigger subclasses to simplify
Jython rule definitions.

If using a build of openHAB **prior** to S1566 or 2.5M2, see
:ref:`Guides/Triggers:System Started` for a ``System started`` workaround. For
everyone, see :ref:`Guides/Triggers:System Shuts Down` for a method of
executing a function when a script is unloaded, simulating a
``System shuts down`` trigger. Along with the ``when`` decorator, this module
includes the following Trigger subclasses (see :ref:`Guides/Rules:Extensions`
for more details):

* **CronTrigger** - fires based on cron expression
* **ItemStateChangeTrigger** - fires when the specified Item's state changes
* **ItemStateUpdateTrigger** - fires when the specified Item's state is updated
* **ItemCommandTrigger** - fires when the specified Item receives a Command
* **GenericEventTrigger** - fires when the specified occurs
* **ItemEventTrigger** - fires when am Item reports an event (based on ``GenericEventTrigger``)
* **ThingEventTrigger** - fires when a Thing reports an event (based on ``GenericEventTrigger``)
* **ThingStatusChangeTrigger** - fires when the specified Thing's status changes **(requires S1636, 2.5M2 or newer)**
* **ThingStatusUpdateTrigger** - fires when the specified Thing's status is updated **(requires S1636, 2.5M2 or newer)**
* **ChannelEventTrigger** - fires when a Channel reports an event
* **DirectoryEventTrigger** - fires when a directory's contents changes
* **ItemRegistryTrigger** - fires when the specified Item registry event occurs
* **ItemAddedTrigger** - fires when an Item is added (based on ``ItemRegistryTrigger``)
* **ItemRemovedTrigger** - fires when an Item is removed (based on ``ItemRegistryTrigger``)
* **ItemUpdatedTrigger** - fires when an Item is updated (based on ``ItemRegistryTrigger``)
* **StartupTrigger** - fires when the rule is activated **(implemented in Jython and requires S1566, 2.5M2 or newer)**
"""
import json
from shlex import split

from core.jsr223.scope import scriptExtension
scriptExtension.importPreset("RuleSupport")
from core.jsr223.scope import itemRegistry, things, Trigger, TriggerBuilder, Configuration
from core.osgi.events import OsgiEventTrigger
from core.utils import validate_uid
from core.log import logging, LOG_PREFIX

from java.nio.file.StandardWatchEventKinds import ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY

from org.quartz.CronExpression import isValidExpression

try:
    from org.openhab.core.thing import ChannelUID, ThingUID, ThingStatus
    from org.openab.core.thing.type import ChannelKind
except:
    from org.eclipse.smarthome.core.thing import ChannelUID, ThingUID, ThingStatus
    from org.eclipse.smarthome.core.thing.type import ChannelKind

try:
    from org.eclipse.smarthome.core.types import TypeParser
except:
    from org.openhab.core.types import TypeParser

LOG = logging.getLogger("{}.core.triggers".format(LOG_PREFIX))

def when(target):
    """
    This function decorator creates triggers attribute in the decorated
    function that is used by the ``rule`` decorator when creating a rule.

    The ``when`` decorator simplifies the use of many of the triggers in this
    module and allows for them to be used with natural language similar to what
    is used in the rules DSL.

    See :ref:`Guides/Rules:Decorators` for examples of how to use this
    decorator.

    Examples:
        .. code-block::

            @when("Time cron 55 55 5 * * ?")
            @when("Item Test_String_1 changed from 'old test string' to 'new test string'")
            @when("Item gMotion_Sensors changed")
            @when("Member of gMotion_Sensors changed from ON to OFF")
            @when("Descendent of gContact_Sensors changed from OPEN to CLOSED")
            @when("Item Test_Switch_2 received update ON")
            @when("Item Test_Switch_1 received command OFF")
            @when("Thing kodi:kodi:familyroom changed")
            @when("Thing kodi:kodi:familyroom changed from ONLINE to OFFLINE")# requires S1636, 2.5M2 or newer
            @when("Thing kodi:kodi:familyroom received update ONLINE")# requires S1636, 2.5M2 or newer
            @when("Channel astro:sun:local:eclipse#event triggered START")# must use a Channel of kind Trigger
            @when("System started")# requires S1566, 2.5M2 or newer ('System shuts down' has not been implemented)

    Args:
        target (string): the `rules DSL-like formatted trigger expression <https://www.openhab.org/docs/configuration/rules-dsl.html#rule-triggers>`_
            to parse
    """
    try:
        def item_trigger(function):
            if not hasattr(function, 'triggers'):
                function.triggers = []
            item = itemRegistry.getItem(trigger_target)
            group_members = []
            if target_type == "Member of":
                group_members = item.getMembers()
            elif target_type == "Descendent of":
                group_members = item.getAllMembers()
            else:
                group_members = [item]
            for member in group_members:
                trigger_name = "Item-{}-{}{}{}{}{}".format(
                    member.name,
                    trigger_type.replace(" ", "-"),
                    "-from-{}".format(old_state) if old_state is not None else "",
                    "-to-" if new_state is not None and trigger_type == "changed" else "",
                    "-" if trigger_type == "received update" and new_state is not None else "",
                    new_state if new_state is not None else "")
                trigger_name = validate_uid(trigger_name)
                if trigger_type == "received update":
                    function.triggers.append(ItemStateUpdateTrigger(member.name, state=new_state, trigger_name=trigger_name).trigger)
                elif trigger_type == "received command":
                    function.triggers.append(ItemCommandTrigger(member.name, command=new_state, trigger_name=trigger_name).trigger)
                else:
                    function.triggers.append(ItemStateChangeTrigger(member.name, previous_state=old_state, state=new_state, trigger_name=trigger_name).trigger)
                LOG.debug("when: Created item_trigger: [{}]".format(trigger_name))
            return function

        def item_registry_trigger(function):
            if not hasattr(function, 'triggers'):
                function.triggers = []
            event_names = {
                'added': 'ItemAddedEvent',
                'removed': 'ItemRemovedEvent',
                'modified': 'ItemUpdatedEvent'
            }
            function.triggers.append(ItemRegistryTrigger(event_names.get(trigger_target)))
            LOG.debug("when: Created item_registry_trigger: [{}]".format(event_names.get(trigger_target)))
            return function

        def cron_trigger(function):
            if not hasattr(function, 'triggers'):
                function.triggers = []
            function.triggers.append(CronTrigger(trigger_type, trigger_name=trigger_name).trigger)
            LOG.debug("when: Created cron_trigger: [{}]".format(trigger_name))
            return function

        def system_trigger(function):
            if not hasattr(function, 'triggers'):
                function.triggers = []
            if trigger_target == "started":
                function.triggers.append(StartupTrigger(trigger_name=trigger_name).trigger)
            else:
                function.triggers.append(ShutdownTrigger(trigger_name=trigger_name).trigger)
            LOG.debug("when: Created system_trigger: [{}]".format(trigger_name))
            return function

        def thing_trigger(function):
            if not hasattr(function, 'triggers'):
                function.triggers = []
            if new_state is not None or old_state is not None:
                if trigger_type == "changed":
                    function.triggers.append(ThingStatusChangeTrigger(trigger_target, previous_status=old_state, status=new_state, trigger_name=trigger_name).trigger)
                else:
                    function.triggers.append(ThingStatusUpdateTrigger(trigger_target, status=new_state, trigger_name=trigger_name).trigger)
            else:
                event_types = "ThingStatusInfoChangedEvent" if trigger_type == "changed" else "ThingStatusInfoEvent"
                function.triggers.append(ThingEventTrigger(trigger_target, event_types, trigger_name=trigger_name).trigger)
            LOG.debug("when: Created thing_trigger: [{}]".format(trigger_name))
            return function

        def channel_trigger(function):
            if not hasattr(function, 'triggers'):
                function.triggers = []
            function.triggers.append(ChannelEventTrigger(trigger_target, event=new_state, trigger_name=trigger_name).trigger)
            LOG.debug("when: Created channel_trigger: [{}]".format(trigger_name))
            return function

        target_type = None
        trigger_target = None
        trigger_type = None
        old_state = None
        new_state = None
        trigger_name = None

        if isValidExpression(target):
            # a simple cron target was used, so add a default target_type and trigger_target (Time cron XXXXX)
            target_type = "Time"
            trigger_target = "cron"
            trigger_type = target
            trigger_name = "Time-cron-{}".format(target)
        else:
            input_list = split(target)
            if len(input_list) > 1:
                # target_type trigger_target [trigger_type] [from] [old_state] [to] [new_state]
                while input_list:
                    if target_type is None:
                        if " ".join(input_list[0:2]) in ["Member of", "Descendent of"]:
                            target_type = " ".join(input_list[0:2])
                            input_list = input_list[2:]
                        else:
                            target_type = input_list.pop(0)
                    elif trigger_target is None:
                        if target_type == "System" and len(input_list) > 1:
                            if " ".join(input_list[0:2]) == "shuts down":
                                trigger_target = "shuts down"
                        else:
                            trigger_target = input_list.pop(0)
                    elif trigger_type is None:
                        if " ".join(input_list[0:2]) == "received update":
                            if target_type in ["Item", "Thing", "Member of", "Descendent of"]:
                                input_list = input_list[2:]
                                trigger_type = "received update"
                            else:
                                raise ValueError("when: \"{}\" could not be parsed. \"received update\" is invalid for target_type \"{}\"".format(target, target_type))
                        elif " ".join(input_list[0:2]) == "received command":
                            if target_type in ["Item", "Member of", "Descendent of"]:
                                input_list = input_list[2:]
                                trigger_type = "received command"
                            else:
                                raise ValueError("when: \"{}\" could not be parsed. \"received command\" is invalid for target_type \"{}\"".format(target, target_type))
                        elif input_list[0] == "changed":
                            if target_type in ["Item", "Thing", "Member of", "Descendent of"]:
                                input_list.pop(0)
                                trigger_type = "changed"
                            else:
                                raise ValueError("when: \"{}\" could not be parsed. \"changed\" is invalid for target_type \"{}\"".format(target, target_type))
                        elif input_list[0] == "triggered":
                            if target_type == "Channel":
                                trigger_type = input_list.pop(0)
                            else:
                                raise ValueError("when: \"{}\" could not be parsed. \"triggered\" is invalid for target_type \"{}\"".format(target, target_type))
                        elif trigger_target == "cron":
                            if target_type == "Time":
                                if isValidExpression(" ".join(input_list)):
                                    trigger_type = " ".join(input_list)
                                    del input_list[:]
                                else:
                                    raise ValueError("when: \"{}\" could not be parsed. \"{}\" is not a valid cron expression. See http://www.quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-06".format(target, " ".join(input_list)))
                            else:
                                raise ValueError("when: \"{}\" could not be parsed. \"cron\" is invalid for target_type \"{}\"".format(target, target_type))
                        else:
                            raise ValueError("when: \"{}\" could not be parsed because the trigger_type {}".format(target, "is missing" if input_list[0] is None else "\"{}\" is invalid".format(input_list[0])))
                    else:
                        if old_state is None and trigger_type == "changed" and input_list[0] == "from":
                            input_list.pop(0)
                            old_state = input_list.pop(0)
                        elif new_state is None and trigger_type == "changed" and input_list[0] == "to":
                            input_list.pop(0)
                            new_state = input_list.pop(0)
                        elif new_state is None and (trigger_type == "received update" or trigger_type == "received command"):
                            new_state = input_list.pop(0)
                        elif new_state is None and target_type == "Channel":
                            new_state = input_list.pop(0)
                        elif input_list:# there are no more possible combinations, but there is more data
                            raise ValueError("when: \"{}\" could not be parsed. \"{}\" is invalid for \"{} {} {}\"".format(target, input_list, target_type, trigger_target, trigger_type))

            else:
                # a simple Item target was used (just an Item name), so add a default target_type and trigger_type (Item XXXXX changed)
                if target_type is None:
                    target_type = "Item"
                if trigger_target is None:
                    trigger_target = target
                if trigger_type is None:
                    trigger_type = "changed"

        # validate the inputs, and if anything isn't populated correctly throw an exception
        if target_type is None or target_type not in ["Item", "Member of", "Descendent of", "Thing", "Channel", "System", "Time"]:
            raise ValueError("when: \"{}\" could not be parsed. target_type is missing or invalid. Valid target_type values are: Item, Member of, Descendent of, Thing, Channel, System, and Time.".format(target))
        elif target_type != "System" and trigger_target not in ["added", "removed", "modified"] and trigger_type is None:
            raise ValueError("when: \"{}\" could not be parsed because trigger_type cannot be None".format(target))
        elif target_type in ["Item", "Member of", "Descendent of"] and trigger_target not in ["added", "removed", "modified"] and itemRegistry.getItems(trigger_target) == []:
            raise ValueError("when: \"{}\" could not be parsed because Item \"{}\" is not in the ItemRegistry".format(target, trigger_target))
        elif target_type in ["Member of", "Descendent of"] and itemRegistry.getItem(trigger_target).type != "Group":
            raise ValueError("when: \"{}\" could not be parsed because \"{}\" was specified, but \"{}\" is not a group".format(target, target_type, trigger_target))
        elif target_type == "Item" and trigger_target not in ["added", "removed", "modified"] and old_state is not None and trigger_type == "changed" and TypeParser.parseState(itemRegistry.getItem(trigger_target).acceptedDataTypes, old_state) is None:
            raise ValueError("when: \"{}\" could not be parsed because \"{}\" is not a valid state for \"{}\"".format(target, old_state, trigger_target))
        elif target_type == "Item" and trigger_target not in ["added", "removed", "modified"] and new_state is not None and (trigger_type == "changed" or trigger_type == "received update") and TypeParser.parseState(itemRegistry.getItem(trigger_target).acceptedDataTypes, new_state) is None:
            raise ValueError("when: \"{}\" could not be parsed because \"{}\" is not a valid state for \"{}\"".format(target, new_state, trigger_target))
        elif target_type == "Item" and trigger_target not in ["added", "removed", "modified"] and new_state is not None and trigger_type == "received command" and TypeParser.parseCommand(itemRegistry.getItem(trigger_target).acceptedCommandTypes, new_state) is None:
            raise ValueError("when: \"{}\" could not be parsed because \"{}\" is not a valid command for \"{}\"".format(target, new_state, trigger_target))
        elif target_type == "Thing" and things.get(ThingUID(trigger_target)) is None:# returns null if Thing does not exist
            raise ValueError("when: \"{}\" could not be parsed because Thing \"{}\" is not in the ThingRegistry".format(target, trigger_target))
        elif target_type == "Thing" and old_state is not None and not hasattr(ThingStatus, old_state):
            raise ValueError("when: \"{}\" is not a valid Thing status".format(old_state))
        elif target_type == "Thing" and new_state is not None and not hasattr(ThingStatus, new_state):
            raise ValueError("when: \"{}\" is not a valid Thing status".format(new_state))
        elif target_type == "Channel" and things.getChannel(ChannelUID(trigger_target)) is None:# returns null if Channel does not exist
            raise ValueError("when: \"{}\" could not be parsed because Channel \"{}\" does not exist".format(target, trigger_target))
        elif target_type == "Channel" and things.getChannel(ChannelUID(trigger_target)).kind != ChannelKind.TRIGGER:
            raise ValueError("when: \"{}\" could not be parsed because \"{}\" is not a trigger Channel".format(target, trigger_target))
        elif target_type == "System" and trigger_target != "started":# and trigger_target != "shuts down":
            raise ValueError("when: \"{}\" could not be parsed. trigger_target \"{}\" is invalid for target_type \"System\". The only valid trigger_type value is \"started\"".format(target, target_type))# and \"shuts down\"".format(target, target_type))

        LOG.debug("when: target=[{}], target_type={}, trigger_target={}, trigger_type={}, old_state={}, new_state={}".format(target, target_type, trigger_target, trigger_type, old_state, new_state))

        trigger_name = validate_uid(trigger_name or target)
        if target_type in ["Item", "Member of", "Descendent of"]:
            if trigger_target in ["added", "removed", "modified"]:
                return item_registry_trigger
            else:
                return item_trigger
        elif target_type == "Thing":
            return thing_trigger
        elif target_type == "Channel":
            return channel_trigger
        elif target_type == "System":
            return system_trigger
        elif target_type == "Time":
            return cron_trigger

    except ValueError as ex:
        LOG.warn(ex)

        def bad_trigger(function):
            if not hasattr(function, 'triggers'):
                function.triggers = []
            function.triggers.append(None)
            return function

        return bad_trigger

    except:
        import traceback
        LOG.debug(traceback.format_exc())

class ItemStateUpdateTrigger(Trigger):
    """
    This class builds an ItemStateUpdateTrigger Module to be used when creating a Rule.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ItemStateUpdateTrigger("MyItem", "ON", "MyItem-received-update-ON").trigger]
            MyRule.triggers.append(ItemStateUpdateTrigger("MyOtherItem", "OFF", "MyOtherItem-received-update-OFF").trigger)

    Args:
        item_name (string): name of item to watch for updates
        state (string): (optional) trigger only when updated TO this state
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, item_name, state=None, trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        configuration = {"itemName": item_name}
        if state is not None:
            configuration["state"] = state
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("core.ItemStateUpdateTrigger").withConfiguration(Configuration(configuration)).build()

class ItemStateChangeTrigger(Trigger):
    """
    This class builds an ItemStateChangeTrigger Module to be used when creating a Rule.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ItemStateChangeTrigger("MyItem", "OFF", "ON", "MyItem-changed-from-OFF-to-ON").trigger]
            MyRule.triggers.append(ItemStateChangeTrigger("MyOtherItem", "ON", "OFF","MyOtherItem-changed-from-ON-to-OFF").trigger)

    Args:
        item_name (string): name of item to watch for changes
        previous_state (string): (optional) trigger only when changing FROM this
            state
        state (string): (optional) trigger only when changing TO this state
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, item_name, previous_state=None, state=None, trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        configuration = {"itemName": item_name}
        if state is not None:
            configuration["state"] = state
        if previous_state is not None:
            configuration["previousState"] = previous_state
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("core.ItemStateChangeTrigger").withConfiguration(Configuration(configuration)).build()

class ItemCommandTrigger(Trigger):
    """
    This class builds an ItemCommandTrigger Module to be used when creating a Rule.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ItemCommandTrigger("MyItem", "ON", "MyItem-received-command-ON").trigger]
            MyRule.triggers.append(ItemCommandTrigger("MyOtherItem", "OFF", "MyOtherItem-received-command-OFF").trigger)

    Args:
        item_name (string): name of item to watch for commands
        command (string): (optional) trigger only when this command is received
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, item_name, command=None, trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        configuration = {"itemName": item_name}
        if command is not None:
            configuration["command"] = command
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("core.ItemCommandTrigger").withConfiguration(Configuration(configuration)).build()

class ThingStatusUpdateTrigger(Trigger):
    """
    This class builds a ThingStatusUpdateTrigger Module to be used when creating a Rule.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ThingStatusUpdateTrigger("kodi:kodi:familyroom", "ONLINE").trigger]

    Args:
        thing_uid (string): name of the Thing to watch for status updates
        status (string): (optional) trigger only when Thing is updated to this
            status
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule.
    """
    def __init__(self, thing_uid, status=None, trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        configuration = {"thingUID": thing_uid}
        if status is not None:
            configuration["status"] = status
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("core.ThingStatusUpdateTrigger").withConfiguration(Configuration(configuration)).build()

class ThingStatusChangeTrigger(Trigger):
    """
    This class builds a ThingStatusChangeTrigger Module to be used when creating a Rule.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ThingStatusChangeTrigger("kodi:kodi:familyroom", "ONLINE", "OFFLINE).trigger]

    Args:
        thing_uid (string): name of the Thing to watch for status changes
        previous_status (string): (optional) trigger only when Thing is changed
            from this status
        status (string): (optional) trigger only when Thing is changed to this
            status
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, thing_uid, previous_status=None, status=None, trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        configuration = {"thingUID": thing_uid}
        if previous_status is not None:
            configuration["previousStatus"] = previous_status
        if status is not None:
            configuration["status"] = status
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("core.ThingStatusChangeTrigger").withConfiguration(Configuration(configuration)).build()

class ChannelEventTrigger(Trigger):
    """
    This class builds a ChannelEventTrigger Module to be used when creating a Rule.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ChannelEventTrigger("astro:sun:local:eclipse#event", "START", "solar-eclipse-event-start").trigger]

    Args:
        channel_uid (string): name of the Channel to watch for trigger events
        event (string): (optional) trigger only when Channel triggers this
            event
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, channel_uid, event=None, trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        configuration = {"channelUID": channel_uid}
        if event is not None:
            configuration["event"] = event
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("core.ChannelEventTrigger").withConfiguration(Configuration(configuration)).build()

class GenericEventTrigger(Trigger):
    """
    This class builds a GenericEventTrigger Module to be used when creating a Rule.
    It allows you to trigger on any event that comes through the Event Bus.
    It's one of the the most powerful triggers, but it is also the most complicated to configure.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [GenericEventTrigger("smarthome/items/Test_Switch_1/", "ItemStateEvent", "smarthome/items/*", "Test_Switch_1-received-update").trigger]

    Args:
        eventSource (string): source to watch for trigger events
        event_types (string or list): types of events to watch
        event_topic (string): (optional) topic to watch
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, event_source, event_types, event_topic="smarthome/*", trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("core.GenericEventTrigger").withConfiguration(Configuration({
            "eventTopic": event_topic,
            "eventSource": "smarthome/{}/".format(event_source),
            "eventTypes": event_types
        })).build()

class ItemEventTrigger(Trigger):
    """
    This class is the same as the ``GenericEventTrigger``, but simplifies it a bit for use with Items.
    The available Item ``eventTypes`` are:

    .. code-block:: none

        "ItemStateEvent" (Item state update)
        "ItemCommandEvent" (Item received Command)
        "ItemStateChangedEvent" (Item state changed)
        "GroupItemStateChangedEvent" (GroupItem state changed)

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ItemEventTrigger("Test_Switch_1", "ItemStateEvent", "smarthome/items/*").trigger]

    Args:
        event_source (string): source to watch for trigger events
        event_types (string or list): types of events to watch
        event_topic (string): (optional) topic to watch (no need to change
            default)
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, event_source, event_types, event_topic="smarthome/items/*", trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("core.GenericEventTrigger").withConfiguration(Configuration({
            "eventTopic": event_topic,
            "eventSource": "smarthome/items/{}/".format(event_source),
            "eventTypes": event_types
        })).build()

class ThingEventTrigger(Trigger):
    """
    This class is the same as the ``GenericEventTrigger``, but simplifies it a bit for use with Things.
    The available Thing ``eventTypes`` are:

    .. code-block:: none

        "ThingAddedEvent"
        "ThingRemovedEvent"
        "ThingStatusInfoChangedEvent"
        "ThingStatusInfoEvent"
        "ThingUpdatedEvent"

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ThingEventTrigger("kodi:kodi:familyroom", "ThingStatusInfoEvent").trigger]

    Args:
        event_source (string): source to watch for trigger events
        event_types (string or list): types of events to watch
        event_topic (string): (optional) topic to watch (no need to change
            default)
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, thing_uid, event_types, event_topic="smarthome/things/*", trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("core.GenericEventTrigger").withConfiguration(Configuration({
            "eventTopic": event_topic,
            "eventSource": "smarthome/things/{}/".format(thing_uid),
            "eventTypes": event_types
        })).build()

EVERY_SECOND = "0/1 * * * * ?"
EVERY_10_SECONDS = "0/10 * * * * ?"
EVERY_MINUTE = "0 * * * * ?"
EVERY_HOUR = "0 0 * * * ?"

class CronTrigger(Trigger):
    """
    This class builds a CronTrigger Module to be used when creating a Rule.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [CronTrigger("0 55 17 * * ?").trigger]

    Args:
        cron_expression (string): a valid `cron expression <http://www.quartz-scheduler.org/documentation/quartz-2.2.2/tutorials/tutorial-lesson-06.html>`_
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, cron_expression, trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        configuration = {
            'cronExpression': cron_expression
        }
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("timer.GenericCronTrigger").withConfiguration(Configuration(configuration)).build()

class StartupTrigger(Trigger):
    """
    This class builds a StartupTrigger Module to be used when creating a Rule.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [StartupTrigger().trigger]

    Args:
        trigger_name (string): (optional) name of this trigger

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, trigger_name=None):
        trigger_name = validate_uid(trigger_name)
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("jsr223.StartupTrigger").withConfiguration(Configuration()).build()

class ItemRegistryTrigger(OsgiEventTrigger):
    """
    This class builds an OsgiEventTrigger Module to be used when creating a
    Rule. Requires the 100_OsgiEventTrigger.py component script. The available
    Item registry ``event_names`` are:

    .. code-block:: none

        "ItemAddedEvent"
        "ItemRemovedEvent"
        "ItemUpdatedEvent"

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ItemRegistryTrigger("ItemAddedEvent").trigger]

    Args:
        event_name (string): name of the event to watch

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, event_name):
        OsgiEventTrigger.__init__(self)
        self.event_name = event_name

    def event_filter(self, event):
        return event.get('type') == self.event_name

    def event_transformer(self, event):
        return json.loads(event['payload'])

class ItemAddedTrigger(ItemRegistryTrigger):
    """
    This class is the same as the ``ItemRegistryTrigger``, but limited to when
    an Item is added. This trigger will fire when any Item is added.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ItemAddedTrigger().trigger]

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self):
        ItemRegistryTrigger.__init__(self, "ItemAddedEvent")

class ItemRemovedTrigger(ItemRegistryTrigger):
    """
    This class is the same as the ``ItemRegistryTrigger``, but limited to when
    an Item is removed. This trigger will fire when any Item is removed.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ItemRemovedTrigger().trigger]

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self):
        ItemRegistryTrigger.__init__(self, "ItemRemovedEvent")

class ItemUpdatedTrigger(ItemRegistryTrigger):
    """
    This class is the same as the ``ItemRegistryTrigger``, but limited to when
    an Item is updated. This trigger will fire when any Item is updated.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Examples:
        .. code-block::

            MyRule.triggers = [ItemUpdatedTrigger().trigger]

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self):
        ItemRegistryTrigger.__init__(self, "ItemUpdatedEvent")

class DirectoryEventTrigger(Trigger):
    """
    This class builds a DirectoryEventTrigger Module to be used when creating a
    Rule. Requires the 100_DirectoryTrigger.py component script.

    See :ref:`Guides/Rules:Extensions` for examples of how to use these extensions.

    Args:
        path (string): path of the directory to watch
        event_kinds (list): (optional) list of the events to watch for
        watch_subdirectories (Boolean): (optional) True will watch
            subdirectories

    Attributes:
        trigger (Trigger): Trigger object to be added to a Rule
    """
    def __init__(self, path, event_kinds=[ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY], watch_subdirectories=False):
        trigger_name = validate_uid(type(self).__name__)
        configuration = {
            'path': path,
            'event_kinds': str(event_kinds),
            'watch_subdirectories': watch_subdirectories,
        }
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID(core.DIRECTORY_TRIGGER_MODULE_ID).withConfiguration(Configuration(configuration)).build()
