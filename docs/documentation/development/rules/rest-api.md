---
layout: documentation
---

{% include base.html %}

# RESTful Web Service API Demo #

## Description ##

The purpose of the demo is to give an example of how to use the Eclipse SmartHome Automation RESTful API. It provides implementation of a simple SmartHome Automation REST Client that gives to the user:

- access to the Automation REST Rule Resource, which provides ability to:
	- list all Automation Rules if there are any
	- create one particular Automation Rule - `AutomationRestAPIDemoSampleRule`
	- update the contents of an Automation Rule, specified by UID
	- list the contents of an Automation Rule, specified by UID
	- delete an Automation Rule, specified by UID
	- enable/disable an Automation Rule, specified by UID
	- list the configuration parameters of an Automation Rule, specified by UID
	- update the configuration parameter values of an Automation Rule, specified by UID
	- list all modules, corresponding to a category, for Automation Rule, specified by UID
	- list a module, corresponding to a category and UID, for Automation Rule, specified by UID
	- list a module configuration, for Module corresponding to a category and UID and for Automation Rule, specified by UID
	- list a module configuration parameter, specified by name, for Module corresponding to a category and UID and for Automation Rule, specified by UID
	- update the value of the module configuration parameter, specified by name, for Module corresponding to a category and UID and for Automation Rule, specified by UID
- access to the Automation REST Module Type Resource, which provides ability to:
	- list all Automation Module Types if there are any
	- list the contents of an Automation Module Type, specified by UID
- access to the Automation REST Template Resource, which provides ability to:
	- list all Automation Templates if there are any
	- list the contents of an Automation Template, specified by UID

## HTTP Service Resources ##

HTTP Service Resources are registered under `**alias** = "/esh/automation/restdemo"`:

- `**name** = "/index.html"`
- `**name** = "/rules.html"`
- `**name** = "/module-types.html"`
- `**name** = "/templates.html"`

----------
Legend:

	**alias** is the equivalent of the OSGi Http Services "alias" in registerResource.
	**name** is the equivalent of the OSGi Http Services "name" in registerResource.
