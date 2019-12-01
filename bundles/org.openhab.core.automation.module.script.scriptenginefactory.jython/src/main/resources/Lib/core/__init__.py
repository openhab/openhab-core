## pylint: disable=too-many-function-args
"""
This module (really a Python package) patches the default scope ``items``
object, so that Items can be accessed as if they were attributes (rather than a
dictionary). It can also be used as a module for registering global variables
that will outlive script reloads.

.. code-block::

    import core

    print items.TestString1

.. note::
    This patch will be applied when any module in the `core` package is loaded.
"""

from core.jsr223.scope import items

#
# Add an attribute-resolver to the items map
#

def _item_getattr(self, name):
    return self[name]

if items:# this check prevents errors if no Items have been created yet
    type(items).__getattr__ = _item_getattr.__get__(items, type(items))
