---
layout: documentation
---

{% include base.html %}

# Java API Demo

The purpose of the demo is to give an example of how to use the Eclipse SmartHome Automation Java API. It is a basic example of an application, named "Welcome Home Application". The application gives ability to the user to switch on the air conditioner and lights in its home remotely. It initializes and registers the services that provide this functionality - Rule Provider, Rule Template Provider, Module Type Provider and Handler Factory for handlers of the modules that compose the rules. Of course, these providers are not mandatory for each application. Some applications may contain only Template Provider or Rule Provider, or Module Type Provider, or Module Handler Factory for some particular module types. Also, to enable the user to have control over the settings and to enforce execution, the demo initializes and registers a service that provides console commands.

## Welcome Home Application

The Welcome Home Application illustrates:

* How to implement Module Types and their provider and how to register it.
* How to implement Rule Templates and their provider and how to register it.
* How to use templates for creation of the Automation Rules.
* How to inject the rules into the Automation Rule Engine by registering Rule Provider service.
* How to implement a Module Handler Factory that helps the Automation Engine to execute the rules and how to register it.
* How to implement the Module Handles of the Automation Module objects.

### Module Types

The Welcome Home Application illustrates how to create your own Module Types and how to provide them to the Automation Engine. Module Types are templates for the creation of the Automation Module objects - Conditions, Triggers and Actions, which are the building blocks of the Automation Rules. When creating an Automation Module object you must specify a Module Type. This will inform the Rule Engine how to treat this object.

#### Trigger Type

Trigger Type gives a base for creation of Trigger objects. Welcome Home Application illustrates how to implement some specific Trigger Types and how to provide them to the Automation Engine. In this demo are exposed two Trigger Types:

	AirConditionerTriggerType
	LightsTriggerType

`AirConditionerTriggerType` can be used for creation of the Triggers that firing a rule that will switch on the Air Conditioner if its current state is "off" and the current room temperature is lower or higher comparing to the target temperature and if the mode of the conditioner is  accordingly "Heating" or "Cooling".

`LightsTriggerType` can be used for creation of the Triggers that firing a rule that will switch on the lights if their current state is "off" or a rule that will lowering the blinds if their current state is "up".

#### Condition Type

Condition Type gives a base for creation of Condition objects. Welcome Home Application illustrates how to implement some specific Condition Types and how to provide them to the Automation Engine. In this demo are exposed two Condition Types:

	TemperatureConditionType
	StateConditionType

`TemperatureConditionType` can be used for creation of the Conditions that make the comparison between the desired temperature and the current room temperature and gives a permission to the Automation Engine to complete the execution of the rule or to terminate it.

`StateConditionType` can be used for creation of the Conditions that make the comparison between the desired state and the current state and gives a permission to the Automation Engine to complete the execution of the rule or to terminate it.

These conditions can be missed if the particular home devices handle the situation.

#### Action Type

Action Type gives a base for creation of Action objects. Welcome Home Application illustrates how to implement some specific Action Types and how to provide them. In this demo is exposed one Action Type:

    WelcomeHomeActionType

`WelcomeHomeActionType` can be used for creation of the Actions that give a command to the particular home devices. Then they will execute it and the rule will be completed.

#### Module Type Provider

Module Type Provider informs the Rule Engine, that it provides some particular Module Types by registering itself as a `ModuleTypeProvider` service and declaring their UIDs in its own registration property `REG_PROPERTY_MODULE_TYPES = "module.types"`.

Example:

	Map<String, ModuleType> providedModuleTypes = new HashMap<String, ModuleType>();
	Dictionary<String, Object> properties = new Hashtable<String, Object>();
	properties.put(REG_PROPERTY_MODULE_TYPES, providedModuleTypes.keySet());
	(BundleContext)bc.registerService(ModuleTypeProvider.class.getName(), moduleTypeProviderObj, properties);

Simple implementation of the `moduleTypeProviderObj` is offered by the class `WelcomeHomeModuleTypeProvider`.

This is the way to give possibility to other applications or users to use these Module Types for creating their own Rules or Templates.

### Rule Templates

The Welcome Home Application illustrates how to create your own Rule Templates and how to provide them to the Automation Engine. They can be used for creation of Automation Rules. The template can be created by one person but other person to choose the rule template and by providing a configuration for it, to create a rule from that. Of course, Welcome Home Application also illustrates a creation of the rules directly from scratch, without need of templates.

#### Rule Templates Description

In this demo is exposed one rule template `AirConditionerRuleTemplate`.

It is created using described above Module Types for creation of its Automation Modules and illustrates how to define its configuration and how to refer its configuration parameters into the configuration of the Automation Modules.

The template configuration parameters are:

* `CONFIG_TARGET_TEMPERATURE = "targetTemperature"` referred by `TemperatureConditionType.CONFIG_TEMPERATURE = "temperature"`
* `CONFIG_OPERATION = "operation"` referred by `TemperatureConditionType.CONFIG_OPERATOR = "operator"`

which are the configuration parameters of the `TemperatureConditionType`. Their default values are `"operation" = "heating"` and `"targetTemperature" = 18`. They are referred so the user can modify their values. For example the `"targetTemperature"` can be set to 20 or the `"operation"` can be set to "cooling".


Also the template illustrates how to connect the outputs of the Trigger to the inputs of the Conditions and Action in scope of the template. The outputs are defined into the `AirConditionerTriggerType`.

The outputs of the Trigger are:

	StateConditionType.INPUT_CURRENT_STATE = "currentState"
	TemperatureConditionType.INPUT_CURRENT_TEMPERATURE = "currentTemperature"

If the default configuration parameter values are set ("heating", 18) and "currentState" output is "off", "currentTemperature" output is 12, the air conditioner will be switched on. If the "currentState" output is "on" or the "currentTemperature" output is 20, then the rule will end before the action to be executed.

If we change the configuration parameter values to the offered one ("cooling", 20) and "currentState" output is "off", "currentTemperature" output is 22, the air conditioner will be switched on. If the "currentState" output is "on" or the "currentTemperature" output is 20, then the rule will end before the action to be executed.

#### Rule Template Provider

Rule Template Provider informs the Rule Engine, that it provides some particular Rule Templates by registering itself as a `TemplateProvider` service and declaring their UIDs in its own registration property `REG_PROPERTY_RULE_TEMPLATES = "rule.templates"`.

Example:

	Map<String, RuleTemplate> providedRuleTemplates = new HashMap<String, RuleTemplate>();
	Dictionary<String, Object> properties = new Hashtable<String, Object>();
	properties.put(REG_PROPERTY_RULE_TEMPLATES, providedRuleTemplates.keySet());
	(BundleContext)bc.registerService(TemplateProvider.class.getName(), ruleTemplateProviderObj, properties);

Simple implementation of the `ruleTemplateProviderObj` is offered by the class `WelcomeHomeTemplateProvider`.

This is the way to give possibility to other applications or users to use the Templates for creating their own Rules.

### Rules

The Welcome Home Application illustrates how to create your own Rules and how to provide them to the Automation Engine, so it can execute them.

#### Rules Description

Creation of the rules provided by this demo are exposed into `WelcomeHomeRulesProvider` class.

Demo Rule UIDs are:

	AC_UID = "AirConditionerSwitchOnRule"
	L_UID = "LightsSwitchOnRule"

`AirConditionerSwitchOnRule` illustrates how to create a rule from template by defining `UID`, `templateUID` and `configuration`. The configuration should contain as keys all required parameter names of the configuration parameters of the template and values for them. This rule gives ability to the user to switch on the air conditioner if it is switched off, his mode is set on "heating" and the current temperature is lower then target temperature or to switch on the air conditioner if it is switched off, his mode is set on "cooling" and the current temperature is higher then target temperature.

`LightsSwitchOnRule` illustrates how to create a rule from scratch by defining one trigger, two conditions, one action, configuration descriptions, configuration values and tags. This rule gives ability to the user to switch on the lights in its home if they are switched off.

#### Rule Provider

Rule Provider is implemented by `WelcomeHomeRulesProvider` class. It provides two rules that give ability to the user to switch on the air conditioner and lights in its home remotely. It informs the Automation Engine by registering itself as a `RuleProvider` service.

Example:

	(BundleContext)bc.registerService(RuleProvider.class.getName(), ruleProviderObj, null);

### Module Handlers

Module Handlers are helpers of the Automation Engine. Automation Engine gives them the module and they know what to do with it. They do the real work. The Automation Engine only decides which of them to use and when.

#### Trigger Handler

Trigger Handler serves to notify the Automation Engine about firing the Triggers. Simple implementation of it can be seen into `WelcomeHomeTriggerHandler` class.

#### Condition Handler

Condition Handler serves to help the Automation Engine to decide if it continues with the execution of the rule or to terminate it. Simple implementation of it can be seen into `StateConditionHandler` or `TemperatureConditionHandler` class.

#### Action Handler

Action Handler is used to help the Automation Engine to execute the specific Actions. A simple implementation of it can be seen into `WelcomeHomeActionHandler` class.

#### Module Handler Factory

Module Handler Factory serves as provider of the Module Handlers. It registers itself as a service to inform the Automation Engine, handlers for which Module Types provides. The Automation Engine refers to it, gives it a module and receives in response a handler of the module. A simple implementation of it can be seen into `WelcomeHomeHandlerFactory` class.

### Commands

The demo provides two commands that enable the user to have control over the settings and to enforce execution of the rules.

#### Welcomehome Commands Description

##### Command `settings`
Parameters: `<mode> <temperature>`

Description:
	Serves to configure air conditioner's mode(cooling/heating) and target temperature by updating the `AirConditionerSwitchOnRule` rule configuration

##### Command `activateAC`
Parameters: `<currentState> <currentTemperature>`

Description:
	Serves to switch on the air conditioner by providing current temperature, current state(on/off) of the air conditioner as inputs of the conditions `TemperatureConditionType` and `StateConditionType`

##### Command `activateLights`
Parameters: `<currentState>`

Description:
	Serves to switch on the lights by providing current state(on/off) of the lights as an input of the condition `StateConditionType`


##### Examples:

###### `welcomehome settings cooling 20` -> result : "AirConditionerSwitchOnRule" rule configuration will be updated: "targetTemperature"=20, "operation"="cooling"

###### `welcomehome activateAC off 20` -> result : Air Conditioner is switched on

###### `welcomehome activateLights off` -> result : Lights are switched on


You can try to play with these values and see what will happen.

#### SmartHome Commands

To see the automation objects that this demo provides you can use `smarthome automation` group commands.

Example(s):
* `smarthome automation listRules AirConditionerSwitchOnRule`
