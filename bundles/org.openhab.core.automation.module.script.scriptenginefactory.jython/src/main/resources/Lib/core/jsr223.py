# pylint: disable=protected-access, no-init
"""
One of the challenges of scripted automation with Jython is that modules
imported into scripts do not have direct access to the JSR223 scope types and
objects. This module allows imported modules to access that data.

.. code-block::

    # In Jython module, not script...
    from core.jsr223.scope import events

    def update_data(data):
        events.postUpdate("TestString1", str(data))
"""
import sys
import types

def get_scope():
    depth = 1
    while True:
        try:
            frame = sys._getframe(depth)
            name = str(type(frame.f_globals))
            if name == "<type 'scope'>":
                return frame.f_globals
            depth += 1
        except ValueError:
            raise EnvironmentError("No JSR223 scope is available")

def _get_scope_value(scope, name):
    return scope.get(name, None) or getattr(scope, name, None)

_PRESETS = [
    [["SimpleRule"], "RuleSimple"],
    [["automationManager"], "RuleSupport"],
]

class _Jsr223ModuleFinder(object):

    class ScopeModule(types.ModuleType):

        def __getattr__(self, name):
            global _PRESETS
            scope = get_scope()
            if name == "scope":
                return scope
            value = _get_scope_value(scope, name)
            if value is None:
                for preset in _PRESETS:
                    if name in preset[0]:
                        script_extension = _get_scope_value(scope, "scriptExtension")
                        # print "auto-import preset ", name, preset, scriptExtension
                        script_extension.importPreset(preset[1])
            return value if value is not None else _get_scope_value(scope, name)

    def load_module(self, fullname):
        if fullname not in sys.modules:
            module = _Jsr223ModuleFinder.ScopeModule('scope')
            setattr(module, '__file__', '<jsr223>')
            setattr(module, '__name__', 'scope')
            setattr(module, '__loader__', self)
            sys.modules[fullname] = module

    def find_module(self, fullname, path=None):
        if fullname == "core.jsr223.scope":
            return self

sys.meta_path.append(_Jsr223ModuleFinder())

def get_automation_manager():
    scope = get_scope()
    _get_scope_value(scope, "scriptExtension").importPreset("RuleSupport")
    automation_manager = _get_scope_value(scope, "automationManager")
    return automation_manager
