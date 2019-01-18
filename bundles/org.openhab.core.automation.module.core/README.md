# Action modules via annotated classes

Action modules can be defined by writing classes and annotating their methods with special annotations.
The framework offers two providers, namely `AnnotatedActionModuleTypeProvider` and `AnnotatedThingActionModuleTypeProvider`, which collect these annotated elements and dynamically create the according action modules.

## Types of annotated classes

There are three different ways to offer action modules via annotations:

### Service

If the goal is to provide an action which is independent of a specific `ThingHandler` and which should only exists as one single instance, it should be implemented as a service.
This service has to implement the `org.eclipse.smarthome.automation.AnnotatedActions` interface.
It can be configured like any other framework service via a `ConfigDescription`, however its category should be `RuleActions`.

### Multi Service

This case is similar to the one above, except for that it can be instantiated and thus configured multiple times.
The service also has to implement the `org.eclipse.smarthome.automation.AnnotatedActions` interface.
It makes use of the multi service infrastructure of the framework and the specified "Service context" becomes the identifier for the specific configuration.
Its category should also be `RuleActions`.

### Thing

For actions that need access to the logic of a `ThingHandler`, one has to implement a service which implements the `org.eclipse.smarthome.core.thing.binding.AnnotatedActionThingHandlerService` interface.
The `ThingHandler` has to override the `Collection<Class> getServices()` method from the `BaseThingHandler` and return the class of the aforementioned service.
The framework takes care of registering and un-registering of that service.

## Annotations

Service classes mentioned above should have the following annotations:

- `@ActionScope(name = "myScope")`: This annotation has to be on the class and `myScope` defines the first part of the ModuleType UID, for example `binding.myBindinName` or `twitter`.
- `@RuleAction(label = "@text/myLabel", description = "myDescription text")`: Each method that should be offered as an action has to have this annotation. The method name will be the second part of the ModuleType uid (after the scope, separated by a "."). There are more parameters available, basically all fields which are part of `org.eclipse.smarthome.automation.type.ActionType`. Translations are also possible if `@text/id` placeholders are used and the bundle providing the actions offers the corresponding files.
- `@ActionOutput(name = "output1", type = "java.lang.String")`: This annotation (or multiple of it) has to be on the return type of the method and specifies under which name and type a result will be available. Usually the type should be the fully qualified Java type, but in the future it will be extented to support further types.
- `@ActionInput(name = "input1")`: This annotation has to be before a parameter of the method to name the input for the module. If the annotation is omitted, the implicit name will be "pN", whereas "N" will be the position of the parameter, i.e. 0-n.

## Method definition

Each annotated method inside of such a service will be turned into an Action ModuleType, i.e. one can have multiple module type definitions per service if multiple methods are annotated.
In addition to the annotations the methods should have a proper name since it is used inside the ModuleType uid.
The return type of a method should be `Map<String, Object>`, because the automation engine uses this mapping between all its modules, or `void` if it does not provide any outputs.
However, there is one shortcut for simple dataypes like `boolean`, `String`, `int`, `double`, and `float`. Such return types will automatically be put into a map with the predefined keyword "result" for the following modules to process.
Within the implementation of the method, only those output names which are specified as `@ActionOutput` annotations should be used, i.e. the resulting map should only contain those which also appear in an `@ActionOutput` annotation.

## Examples

For examples, please see the package `org.eclipse.smarthome.magic.binding.internal.automation.modules` inside the `org.eclipse.smarthome.magic` bundle.
