# pylint: disable=wrong-import-position, invalid-name
"""
This module provides an OSGi EventAdmin event monitor and rule trigger. This
can trigger off any OSGi event. Rule manager events are filtered to avoid
circular loops in the rule execution.

.. code-block::

    class ExampleRule(SimpleRule):
        def __init__(self):
            self.triggers = [ core.osgi.events.OsgiEventTrigger() ]

        def execute(self, module, inputs):
            event = inputs['event']
            # do something with event
"""
import uuid
import traceback

from core.jsr223.scope import scriptExtension
scriptExtension.importPreset("RuleSupport")
from core.jsr223.scope import Trigger, TriggerBuilder, Configuration
#import core
from core.osgi import BUNDLE_CONTEXT
from core.log import logging, LOG_PREFIX

import java.util

#from org.osgi.framework import FrameworkUtil
from org.osgi.service.event import EventHandler, EventConstants#, EventAdmin
#from org.osgi.service.cm import ManagedService

LOG = logging.getLogger("{}.core.osgi.events".format(LOG_PREFIX))

def hashtable(*key_values):
    """
    Creates a Hashtable from 2-tuples of key/value pairs.

    Args:
        key_values (2-tuples): the key/value pairs to add to the Hashtable

    Returns:
        java.util.Hashtable: initialized Hashtable
    """
    _hashtable = java.util.Hashtable()
    for key, value in key_values:
        _hashtable.put(key, value)
    return _hashtable
class OsgiEventAdmin(object):
    _event_handler = None
    event_listeners = []

    log = logging.getLogger("{}.core.osgi.events.OsgiEventAdmin".format(LOG_PREFIX))

    # Singleton
    class OsgiEventHandler(EventHandler):
        def __init__(self):
            self.log = logging.getLogger("jsr223.jython.core.osgi.events.OsgiEventHandler")
            self.registration = BUNDLE_CONTEXT.registerService(
                EventHandler, self, hashtable((EventConstants.EVENT_TOPIC, ["*"])))
            self.log.info("Registered openHAB OSGi event listener service")
            self.log.debug("Registration: [{}]".format(self.registration))

        def handleEvent(self, event):
            self.log.critical("Handling event: [{}]".format(event))
            for listener in OsgiEventAdmin.event_listeners:
                try:
                    listener(event)
                except:
                    self.log.error("Listener failed: [{}]".format(traceback.format_exc()))

        def dispose(self):
            self.registration.unregister()

    @classmethod
    def add_listener(cls, listener):
        cls.log.debug("Adding listener admin: [{} {}]".format(id(cls), listener))
        cls.event_listeners.append(listener)
        if len(cls.event_listeners) == 1:
            if cls._event_handler is None:
                cls._event_handler = cls.OsgiEventHandler()

    @classmethod
    def remove_listener(cls, listener):
        cls.log.debug("Removing listener: [{}]".format(listener))
        if listener in cls.event_listeners:
            cls.event_listeners.remove(listener)
        if not cls.event_listeners:
            if cls._event_handler is not None:
                cls.log.info("Unregistering openHAB OSGi event listener service")
                cls._event_handler.dispose()
                cls._event_handler = None


# The OH / JSR223 design does not allow trigger handlers to access
# the original trigger instance. The trigger information is copied into a
# RuntimeTrigger and then provided to the trigger handler. Therefore, there
# is no way AFAIK to access the original trigger from the trigger handler.
# Another option is to pass trigger information in the configuration, but
# OSGi doesn't support passing Jython-related objects. To work around these
# issues, the following dictionary provides a side channel for obtaining the original
# trigger.
OSGI_TRIGGERS = {}

class OsgiEventTrigger(Trigger):
    def __init__(self, filter_predicate=None):
        """
        The filter_predicate is a predicate taking an event argument and
        returning True (keep) or False (drop).
        """
        self.filter = filter_predicate or (lambda event: True)
        trigger_name = type(self).__name__ + "-" + uuid.uuid1().hex
        self.trigger = TriggerBuilder.create().withId(trigger_name).withTypeUID("jsr223.OsgiEventTrigger").withConfiguration(Configuration()).build()
        #global OSGI_TRIGGERS
        #OSGI_TRIGGERS[self.id] = self
        #OSGI_TRIGGERS[trigger_name] = self
        OSGI_TRIGGERS[self.trigger.id] = self

    def event_filter(self, event):
        return self.filter(event)

    def event_transformer(self, event):
        return event

def log_event(event):
    LOG.info("OSGi event: [{} ({})]".format(event, type(event).__name__))
    if isinstance(event, dict):
        for name in event:
            value = event[name]
            LOG.info("  '{}': {} ({})".format(name, value, type(value)))
    else:
        for name in event.propertyNames:
            value = event.getProperty(name)
            LOG.info("  '{}': {} ({})".format(name, value, type(value)))

def event_dict(event):
    return {key: event.getProperty(key) for key in event.getPropertyNames()}
