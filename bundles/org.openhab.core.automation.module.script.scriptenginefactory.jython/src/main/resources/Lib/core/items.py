# pylint: disable=wrong-import-position
"""
This module allows runtime creation and removal of items. It will also remove
any links from an Item before it is removed.
"""
__all__ = ["add_item", "remove_item"]

from core.jsr223.scope import scriptExtension, itemRegistry
scriptExtension.importPreset(None)
#import core
from core import osgi
from core.log import logging, LOG_PREFIX
from core.links import remove_all_links

ITEM_BUILDER_FACTORY = osgi.get_service("org.openhab.core.items.ItemBuilderFactory") or osgi.get_service("org.eclipse.smarthome.core.items.ItemBuilderFactory")

MANAGED_ITEM_PROVIDER = osgi.get_service("org.openhab.core.items.ManagedItemProvider") or osgi.get_service("org.eclipse.smarthome.core.items.ManagedItemProvider")

LOG = logging.getLogger("{}.core.items".format(LOG_PREFIX))

def add_item(item_or_item_name, item_type=None, category=None, groups=None, label=None, tags=None, gi_base_type=None, group_function=None):
    """
    Adds an Item using a ManagedItemProvider.

    Args:
        item_or_item_name (Item or str): Item object or name for the Item to
            create
        item_type (str): (optional, if item_oritem_name is an Item) the type
            of the Item
        category (str): (optional) the category (icon) for the Item
        groups (str): (optional) a list of groups the Item is a member of
        label (str): (optional) the label for the Item
        tags (list): (optional) a list of tags for the Item
        gi_base_type (str): (optional) the group Item base type for the Item
        group_function (GroupFunction): (optional) the group function used by
            the Item

    Returns:
        Item or None: The Item that was created or None

    Raises:
        TypeError: if item_or_item_name is not an Item or string, or if
            item_or_item_name is not an Item and item_type is not provided
    """
    try:
        if not isinstance(item_or_item_name, basestring) and not hasattr(item_or_item_name, 'name'):
            raise TypeError("\"{}\" is not a string or Item".format(item_or_item_name))
        item = item_or_item_name
        if isinstance(item_or_item_name, basestring):
            item_name = item_or_item_name
            if item_type is None:
                raise TypeError("Must provide item_type when creating an Item by name")

            base_item = None if item_type != "Group" or gi_base_type is None else ITEM_BUILDER_FACTORY.newItemBuilder(gi_base_type, item_name + "_baseItem").build()
            group_function = None if item_type != "Group" else group_function
            if tags is None:
                tags = []
            item = ITEM_BUILDER_FACTORY.newItemBuilder(item_type, item_name)\
                                                    .withCategory(category)\
                                                    .withGroups(groups)\
                                                    .withLabel(label)\
                                                    .withBaseItem(base_item)\
                                                    .withGroupFunction(group_function)\
                                                    .withTags(set(tags))\
                                                    .build()

        MANAGED_ITEM_PROVIDER.add(item)
        LOG.debug("Item added: [{}]".format(item))
        return item
    except:
        import traceback
        LOG.error(traceback.format_exc())
        return None

def remove_item(item_or_item_name):
    """
    This function removes an Item using a ManagedItemProvider.

    Args:
        item_or_item_name (Item or str): the Item object or name for the
            Item to create

    Returns:
        Item or None: the Item that was removed or None
    """
    try:
        item = remove_all_links(item_or_item_name)
        if item is None:
            LOG.warn("Item cannot be removed because it does not exist in the ItemRegistry: [{}]".format(item_or_item_name))
            return None

        MANAGED_ITEM_PROVIDER.remove(item.name)
        if itemRegistry.getItems(item.name) == []:
            LOG.debug("Item removed: [{}]".format(item.name))
            return item
        else:
            LOG.warn("Failed to remove Item from the ItemRegistry: [{}]".format(item.name))
            return None
    except:
        import traceback
        LOG.error(traceback.format_exc())
        return None
