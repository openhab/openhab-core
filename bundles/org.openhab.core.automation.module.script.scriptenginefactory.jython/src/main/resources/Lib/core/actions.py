"""
This module discovers action services registered from OH1 or OH2 bundles or
add-ons. The specific actions that are available will depend on which add-ons
are installed. Each action class is exposed as an attribute of the
``core.actions`` module. The action methods are static methods on those classes
(don't try to create instances of the action classes).

.. warning:: In order to avoid namespace conflicts with the ``actions`` object
    provided in the default scope, don't use ``import core.actions`` or
    ``from core import actions``.

See the :ref:`Guides/Actions:Actions` guide for details on the use of this
module.
"""
import sys
from core import osgi

__all__ = []

OH1_ACTIONS = osgi.find_services("org.openhab.core.scriptengine.action.ActionService", None) or []
OH2_ACTIONS = osgi.find_services("org.eclipse.smarthome.model.script.engine.action.ActionService", None) or []

_MODULE = sys.modules[__name__]

for s in OH1_ACTIONS + OH2_ACTIONS:
    action_class = s.actionClass
    name = str(action_class.simpleName)
    setattr(_MODULE, name, action_class)
    __all__.append(name)

try:
    from org.openhab.core.model.script.actions import Exec
    from org.openhab.core.model.script.actions import HTTP
    from org.openhab.core.model.script.actions import LogAction
    from org.openhab.core.model.script.actions import Ping
    from org.openhab.core.model.script.actions import ScriptExecution
except:
    from org.eclipse.smarthome.model.script.actions import Exec
    from org.eclipse.smarthome.model.script.actions import HTTP
    from org.eclipse.smarthome.model.script.actions import LogAction
    from org.eclipse.smarthome.model.script.actions import Ping
    from org.eclipse.smarthome.model.script.actions import ScriptExecution

STATIC_IMPORTS = [Exec, HTTP, LogAction, Ping, ScriptExecution]

for s in STATIC_IMPORTS:
    name = str(s.simpleName)
    setattr(_MODULE, name, s)
    __all__.append(name)
