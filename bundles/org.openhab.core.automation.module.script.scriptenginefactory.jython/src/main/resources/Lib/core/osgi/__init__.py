"""
Provides utility functions for retrieving, registering and removing OSGi
services.
"""
__all__ = [
    'get_service',
    'find_services',
    'register_service',
    'unregister_service'
]

from core.jsr223.scope import scriptExtension
from org.osgi.framework import FrameworkUtil

_BUNDLE = FrameworkUtil.getBundle(type(scriptExtension))
BUNDLE_CONTEXT = _BUNDLE.getBundleContext() if _BUNDLE else None

REGISTERED_SERVICES = {}

def get_service(class_or_name):
    """
    This function gets the specified OSGi service.

    Args:
        class_or_name (class or str): the class or class name of the service to
            get

    Returns:
        OSGi service or None: the requested OSGi service or None
    """
    if BUNDLE_CONTEXT:
        classname = class_or_name.getName() if isinstance(class_or_name, type) else class_or_name
        ref = BUNDLE_CONTEXT.getServiceReference(classname)
        return BUNDLE_CONTEXT.getService(ref) if ref else None
    else:
        return None

def find_services(class_name, service_filter):
    """
    This function finds the specified OSGi service.

    Args:
        class_or_name (class or str): the class or class name of the service to
            get
        service_filter (str): the filter expression or None for all services

    Returns:
        list: a list of matching OSGi services
    """
    if BUNDLE_CONTEXT:
        references = BUNDLE_CONTEXT.getAllServiceReferences(class_name, service_filter)
        if references:
            return [BUNDLE_CONTEXT.getService(reference) for reference in references]
    else:
        return None

def register_service(service, interface_names, properties=None):
    """
    This function registers the specified service object with the specified
    properties under the specified class names into the Framework.

    Args:
        service (java.lang.Object): the service to register
        interface_names (list): a list of class names
        properties (dict): a dict of properties for the service

    Returns:
        ServiceRegistration: a ServiceRegistration object used to update or
        unregister the service
    """
    if properties:
        import java.util
        properties_hashmap = java.util.Hashtable()
        for key, value in properties.iteritems():
            properties_hashmap.put(key, value)
        properties = properties_hashmap
    registered_service = BUNDLE_CONTEXT.registerService(interface_names, service, properties)
    for name in interface_names:
        REGISTERED_SERVICES[name] = (service, registered_service)
    return registered_service

def unregister_service(service):
    """
    This function unregisters an OSGi service.

    Args:
        service (java.lang.Object): the service to unregister
    """
    keys = REGISTERED_SERVICES.keys()
    for key in keys:
        service_object, registered_service = REGISTERED_SERVICES[key]
        if service == service_object:
            del REGISTERED_SERVICES[key]
            registered_service.unregister()
