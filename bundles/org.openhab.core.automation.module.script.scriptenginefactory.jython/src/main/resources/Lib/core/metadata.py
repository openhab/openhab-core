"""
This module provides functions for manipulating Item Metadata.

See the :ref:`Guides/Metadata:Metadata` guide for details on the metadata
structure.
"""
__all__ = [
    "get_all_namespaces", "get_metadata", "set_metadata", "remove_metadata",
    "get_value", "set_value", "get_key_value", "set_key_value",
    "remove_key_value"
]

from core import osgi
from core.log import logging, LOG_PREFIX

try:
    from org.openhab.core.items import Metadata, MetadataKey
except:
    from org.eclipse.smarthome.core.items import Metadata, MetadataKey

METADATA_REGISTRY = osgi.get_service("org.openhab.core.items.MetadataRegistry") or osgi.get_service("org.eclipse.smarthome.core.items.MetadataRegistry")

LOG = logging.getLogger("{}.core.metadata".format(LOG_PREFIX))

def get_all_namespaces(item_name):
    """
    This function will return a list of an Item's namespaces.

    Examples:
        .. code-block::

            # Get a list of an Item's namespaces
            get_all_namespaces("Item_Name")

    Args:
        item_name (str): the name of the Item to retrieve the namespace names
        for

    Returns:
        list: a list of strings representing the namespace names found for the
        specified Item
    """
    LOG.debug("get_all_namespaces: Item [{}]".format(item_name))
    return [metadata.UID.namespace for metadata in METADATA_REGISTRY.getAll() if metadata.UID.itemName == item_name]

def get_metadata(item_name, namespace):
    """
    This function will return the Metadata object associated with the
    specified Item.

    Examples:
        .. code-block::

            # Get Metadata object from an Item's namespace
            get_metadata("Item_Name", "Namespace_Name")

    Args:
        item_name (str): name of the Item
        namespace (str): name of the namespace

    Returns:
        Metadata object or None: Metadata object containing the namespace
        ``value`` and ``configuration`` dictionary, but will be ``None`` if
        the namespace or the Item does not exist
    """
    LOG.debug("get_metadata: Item [{}], namespace [{}]".format(item_name, namespace))
    return METADATA_REGISTRY.get(MetadataKey(namespace, item_name))

def set_metadata(item_name, namespace, configuration, value=None, overwrite=False):
    """
    This function creates or modifies Item metadata, optionally overwriting
    the existing data. If not overwriting, the provided keys and values will
    be overlaid on top of the existing keys and values.

    Examples:
        .. code-block::

            # Add/change metadata in an Item's namespace (only overwrites existing keys and "value" is optional)
            set_metadata("Item_Name", "Namespace_Name", {"Key_1": "key 1 value", "Key_2": 2, "Key_3": False}, "namespace_value")

            # Overwrite metadata in an Item's namespace with new data
            set_metadata("Item_Name", "Namespace_Name", {"Key_5": 5}, overwrite=True)

    Args:
        item_name (str): name of the Item
        namespace (str): name of the namespace
        configuration (dict): ``configuration`` dictionary to add to the
            namespace
        value (str): either the new namespace value or ``None``
        overwrite (bool): if ``True``, existing namespace data will be
            discarded
    """
    if overwrite:
        remove_metadata(item_name, namespace)
    metadata = get_metadata(item_name, namespace)
    if metadata is None or overwrite:
        LOG.debug("set_metadata: adding or overwriting metadata namespace with [value: {}, configuration: {}]: Item [{}], namespace [{}]".format(value, configuration, item_name, namespace))
        METADATA_REGISTRY.add(Metadata(MetadataKey(namespace, item_name), value, configuration))
    else:
        if value is None:
            value = metadata.value
        new_configuration = dict(metadata.configuration).copy()
        new_configuration.update(configuration)
        LOG.debug("set_metadata: setting metadata namespace to [value: {}, configuration: {}]: Item [{}], namespace [{}]".format(value, new_configuration, item_name, namespace))
        METADATA_REGISTRY.update(Metadata(MetadataKey(namespace, item_name), value, new_configuration))

def remove_metadata(item_name, namespace=None):
    """
    This function removes the Item metadata for the specified namepsace or for
    all namespaces.

    Examples:
        .. code-block::

            # Remove a namespace from an Item
            remove_metadata("Item_Name", "Namespace_Name")

            # Remove ALL namespaces from an Item
            remove_metadata("Item_Name")

    Args:
        item_name (str): name of the item
        namespace (str): name of the namespace or ``None``, which will
            remove metadata in all namespaces for the specified Item
    """
    if namespace is None:
        LOG.debug("remove_metadata (all): Item [{}]".format(item_name))
        METADATA_REGISTRY.removeItemMetadata(item_name)
    else:
        LOG.debug("remove_metadata: Item [{}], namespace [{}]".format(item_name, namespace))
        METADATA_REGISTRY.remove(MetadataKey(namespace, item_name))

def get_key_value(item_name, namespace, *args):
    """
    Ths function returns the ``configuration`` value for the specified key.

    Examples:
        .. code-block::

            # Get key/value pair from Item's namespace "configuration"
            get_key_value("Item_Name", "Namespace_Name", "Key", "Subkey", "Subsubkey")

    Args:
        item_name (str): name of the Item
        namespace (str): name of the namespace
        key (str): ``configuration`` key to return (multiple keys in
            descending branches can be used)

    Returns:
        str, decimal, boolean, None, or dict: The ``configuration`` key value
        will be returned. Since None is a valid value for a key, when the key,
        Item or namespace does not exist, this function will return an empty
        dictionary.
    """
    LOG.debug("get_key_value: Item [{}], namespace [{}], args [{}]".format(item_name, namespace, args))
    metadata = get_metadata(item_name, namespace)
    if metadata is not None:
        result = metadata.configuration.get(args[0])
        if result is None:
            return {}
        else:
            for arg in args[1:]:
                result = result.get(arg, {})
            return result
    else:
        return {}

def set_key_value(item_name, namespace, *args):
    """
    This function creates or updates a key value in the specified namespace.

    Examples:
        .. code-block::

            # Set key/value pair in Item's namespace "configuration"
            set_key_value("Item_Name", "Namespace_Name", "Key", "Subkey", "Subsubkey", "Value")

    Args:
        item_name (str): name of the Item
        namespace (str): name of the namespace
        key (str): key to create or update (multiple keys in descending
            branches can be used)
        value (str, decimal, boolean, dict or None): value to set
    """
    LOG.debug("set_key_value: Item [{}], namespace [{}], args [{}]".format(item_name, namespace, args))
    if len(args) > 1:
        metadata = get_metadata(item_name, namespace)
        new_configuration = {}
        if metadata is not None:
            new_configuration = dict(metadata.configuration).copy()
        sub_dict = new_configuration
        for arg in args[:-1]:
            if arg not in sub_dict.keys():
                sub_dict[arg] = {}
            if arg == args[-2]:
                sub_dict[arg] = args[-1]
            else:
                sub_dict = sub_dict[arg]
        set_metadata(item_name, namespace, new_configuration)
    else:
        LOG.warn("set_key_value: at least two args required: args [{}]".format(args))

def remove_key_value(item_name, namespace, *args):
    """
    This function removes a key from a namespace's ``configuration``.

    Examples:
        .. code-block::

            # Remove key/value pair from namespace ``configuration``
            remove_key_value("Item_Name", "Namespace_Name", "Key", "Subkey", "Subsubkey")

    Args:
        item_name (str): name of the Item
        namespace (str): name of the namespace
        key (str): ``configuration`` key to remove (multiple keys in
            descending branches can be used)
    """
    LOG.debug("remove_key_value: Item [{}], namespace [{}], args [{}]".format(item_name, namespace, args))
    if args:
        metadata = get_metadata(item_name, namespace)
        if metadata is not None:
            new_configuration = dict(metadata.configuration).copy()
            sub_dict = new_configuration
            for arg in args:
                if arg != args[-1]:
                    sub_dict = sub_dict.get(arg, {})
                elif sub_dict != {}:
                    sub_dict.pop(arg)
                else:
                    LOG.warn("remove_key_value: key does not exist: Item [{}], namespace [{}], args [{}]".format(item_name, namespace, args))
            set_metadata(item_name, namespace, new_configuration, metadata.value, True)
        else:
            LOG.warn("remove_key_value: metadata does not exist: Item [{}], namespace [{}]".format(item_name, namespace))
    else:
        LOG.warn("remove_key_value: at least one key is required")

def get_value(item_name, namespace):
    """
    This function will return the Item metadata ``value`` for the specified
    namespace.

    Examples:
        .. code-block::

            # Get Item's namespace "value"
            get_value("Item_Name", "Namespace_Name")

    Args:
        item_name (str): name of the item
        namespace (str): name of the namespace

    Returns:
        str or None: namespace ``value`` or ``None`` if the namespace or
        Item does not exist
    """
    LOG.debug("get_value: Item [{}], namespace [{}]".format(item_name, namespace))
    metadata = get_metadata(item_name, namespace)
    if metadata is not None:
        return metadata.value
    else:
        return None

def set_value(item_name, namespace, value):
    """
    This function creates or updates the Item metadata ``value`` for the
    specified namespace.

    Examples:
        .. code-block::

            # Set Item's namespace "value"
            set_value("Item_Name", "Namespace_Name", "namespace value")

    Args:
        item_name (str): name of the Item
        namespace (str): name of the namespace
        value (str): new or updated namespace value
    """
    LOG.debug("set_value: Item [{}], namespace [{}], value [{}]".format(item_name, namespace, value))
    metadata = get_metadata(item_name, namespace)
    if metadata is not None:
        set_metadata(item_name, namespace, metadata.configuration, value, True)
    else:
        set_metadata(item_name, namespace, {}, value)
