"""
This module bridges the `Python standard logging module <https://docs.python.org/2/library/logging.html>`_
with the slf4j library used by openHAB. The ``configuration`` module provides
a ``LOG_PREFIX`` variable that is used as the default logger throughout the
core modules and scripts.
"""
import logging
import functools
import traceback

from org.slf4j import Logger, LoggerFactory

from configuration import LOG_PREFIX

class Slf4jHandler(logging.Handler):
    def emit(self, record):
        message = self.format(record)
        logger_name = record.name
        if record.name == "root":
            logger_name = Logger.ROOT_LOGGER_NAME
        logger = LoggerFactory.getLogger(logger_name)
        level = record.levelno
        if level == logging.CRITICAL:
            logger.trace(message)
        elif level == logging.ERROR:
            logger.error(message)
        elif level == logging.DEBUG:
            logger.debug(message)
        elif level == logging.WARNING:
            logger.warn(message)
        elif level == logging.INFO:
            logger.info(message)

HANDLER = Slf4jHandler()
logging.root.setLevel(logging.DEBUG)
logging.root.handlers = [HANDLER]

def log_traceback(function):
    """
    Decorator to provide better Jython stack traces

    Essentially, the decorated function/class/method is wrapped in a try/except
    and will log a traceback for exceptions. If openHAB Cloud Connector is
    installed, exceptions will be sent as a notification. If the
    configuration.adminEmail variable is populated, the notification will be
    sent to that address. Otherwise, a broadcast notification will be sent.
    """
    functools.wraps(function)
    def wrapper(*args, **kwargs):
        try:
            return function(*args, **kwargs)
        except:
            rule_name = None
            if hasattr(function, 'log'):
                function.log.error(traceback.format_exc())
                rule_name = function.name
            elif args and hasattr(args[0], 'log'):
                args[0].log.error(traceback.format_exc())
                rule_name = args[0].name
            else:
                logging.getLogger(LOG_PREFIX).error(traceback.format_exc())
            import core.actions
            if hasattr(core.actions, 'NotificationAction'):
                import configuration
                if hasattr(configuration, 'admin_email') and configuration.admin_email != "admin_email@some_domain.com":
                    core.actions.NotificationAction.sendNotification(configuration.admin_email, "Exception: {}: [{}]".format(rule_name, traceback.format_exc()))
                else:
                    core.actions.NotificationAction.sendBroadcastNotification("Exception: {}: [{}]".format(rule_name, traceback.format_exc()))
    return wrapper
