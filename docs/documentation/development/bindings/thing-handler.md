---
layout: documentation
---

{% include base.html %}

# Thing Handler Implementation

A `ThingHandler` handles the communication between the Eclipse SmartHome framework and an entity from the real world, e.g. a physical device, a web service, represented by a `Thing`. 

The communication is bidirectional. The framework informs a thing handler about commands, state and configuration updates, and so on, by the corresponding handler methods. The handler can notify the framework about changes like state and status updates, updates of the whole thing, by a `ThingHandlerCallback`.

In this section the ThingHandler API is described in more detail and you get hints how to implement your binding.

## The BaseThingHandler

Eclipse SmartHome provides an abstract base class named `BaseThingHandler`. It is recommended to use this class, because it covers a lot of common logic. Most of the explanations are based on the assumption, that the binding inherits from the BaseThingHandler in all concrete `ThingHandler` implementations. Nevertheless if there are reasons why you can not use the base class, the binding can also directly implement the `ThingHandler` interface.

The communication between the framework and the ThingHandler is bidirectional. If the framework wants the binding to do something or just notfiy it about changes, it calls methods like `handleCommand`, `handleUpdate` or `thingUpdated`. If the ThingHandler wants to inform the framework about changes, it uses a callback. The `BaseThingHandler` provides convience methods like `updateState`, `updateStatus` `updateThing` or `triggerChannel`, that can be used to inform the framework about changes.

## Life cycle 

The `ThingHandler` has a well defined life cycle. The two most important life cycle methods are: `initialize` and `dispose`. The `initialize` method is called, when the handler is started and `dispose` is called just before the handler is stopped. Therefore the methods can be used to allocate and deallocate resources.

### Startup

The startup of a handler is divided in two essential steps:

1. Handler is registered: `ThingHandler` instance is created by a `ThingHandlerFactory` and tracked by the framework. In addition, the handler can be registered as a service if required, e.g. as `FirmwareUpdateHandler` or `ConfigStatusProvider`.
 
2. Handler is initialized: `ThingHandler.initialize()` is called by the framework in order to initialize the handler. This method is only called if all 'required' configuration parameters of the Thing are present. The handler is ready to work (methods like `handleCommand`, `handleUpdate` or `thingUpdated` can be called).

The diagram below illustrates the startup of a handler in more detail. The life cycle is controlled by the `ThingManager`.

![thing_life_cycle_startup](diagrams/thing_life_cycle_startup.png)

The `ThingManager` mediates the communication between a `Thing` and a `ThingHandler` from the binding. The `ThingManager` creates for each Thing a `ThingHandler` instance using a `ThingHandlerFactory`. Therefore, it tracks all `ThingHandlerFactory`s from the binding. 

The `ThingManager` determines if the `Thing` is initializable or not. A `Thing` is considered as *initializable* if all *required* configuration parameters (cf. property *parameter.required* in [Configuration Description](xml-reference.html)) are available. If so, the method `ThingHandler.initialize()` is called.

Only Things with status (cf. [Thing Status](../../concepts/things.html#thing-status)) *UNKNOWN*, *ONLINE* or *OFFLINE* are considered as *initialized* by the framework and therefore it is the handler's duty to assign one of these states sooner or later. To achieve that, the status must be reported to the framework via the callback or `BaseThingHandler.updateStatus(...)` for convenience. Furthermore, the framework expects `initialize()` to be non-blocking and to return quickly. For longer running initializations, the implementation has to take care of scheduling a separate job which must guarantee to set the status eventually. Also, please note that the framework expects the `initialize()` method to handle anticipated error situations gracefully and set the thing to *OFFLINE* with the corresponding status detail (e.g. *COMMUNICATION_ERROR* or *CONFIGURATION_ERROR* including a meaningful description) instead of throwing exceptions. 

If the `Thing` is not initializable the configuration can be updated via `ThingHandler.handleConfigurationUpdate(Map)`. The binding has to notify the `ThingManager` about the updated configuration by a callback. The `ThingManager` tries to initialize the `ThingHandler` resp. `Thing` again.

After the handler is initialized, the handler must be ready to handle methods calls like `handleCommand` and `handleUpdate`, as well as `thingUpdated`. 

### Shutdown

The shutdown of a handler is also divided in two essential steps:

1. Handler is unregistered: `ThingHandler` instance is no longer tracked by the framework. The `ThingHandlerFactory` can unregister handler services (e.g. `FirmwareUpdateHandler` or `ConfigStatusProvider`) if registered, or release resources.

2. Handler is disposed: `ThingHandler.disposed()` method is called. The framework expects `dispose()` to be non-blocking and to return quickly. For longer running disposals, the implementation has to take care of scheduling a separate job. 

![thing_life_cycle_shutdown](diagrams/thing_life_cycle_shutdown.png)

After the handler is disposed, the framework will not call the handler anymore.

## Bridge Status Changes

A `ThingHandler` is notified about Bridge status changes to *ONLINE* and *OFFLINE* after a `BridgeHandler` has been initialized. Therefore, the method `ThingHandler.bridgeStatusChanged(ThingStatusInfo)` must be implemented (this method is not called for a bridge status updated through the bridge initialization itself). If the Thing of this handler does not have a Bridge, this method is never called.

If the bridge status has changed to OFFLINE, the status of the handled thing must also be updated to *OFFLINE* with detail *BRIDGE_OFFLINE*. If the bridge returns to *ONLINE*, the thing status must be changed at least to *OFFLINE* with detail *NONE* or to another thing specific status.


## Handling Commands

For handling commands the `ThingHandler` interface defines the `handleCommand` method. This method is called when a command is sent to an item, which is linked to a channel of the *Thing*. A Command represents the intention that an action should be executed on the external system, or that the state should be changed. Inside the `handleCommand` method binding specific logic can be executed.

The ThingHandler implementation must be prepared to handle different command types depending on the item types, that are defined by the channels. The method can also be called at the same time from different threads, so it must be thread-safe.

If an exception is thrown in the method, it will be caught by the framework and logged as an error. So it is better to handle communication errors within the binding and to update the thing status accordingly. Typically only the binding is knowledgeable about the severity of an error and if it should be logged as info, warning or error message. If the communication to the device or service was successful it is good practice to set the thing status to *ONLINE* by calling `statusUpdated(ThingStatus.ONLINE)`.

The following code block shows a typical implementation of the `handleCommand` method:

```java
@Override
public void handleCommand(ChannelUID channelUID, Command command) {
    try {
    	switch (channelUID.getId()) {
	    	case CHANNEL_TEMPERATURE:
	        	if(command instanceof OnOffType.class) {
	        		// binding specific logic goes here
	        		SwitchState deviceSwitchState = convert((OnOffType) command);
	        		updateDeviceState(deviceSwitchState);
	        	}
	        	break;
	    	// ...
    	}
    	statusUpdated(ThingStatus.ONLINE);
	} catch(DeviceCommunicationException ex) {
		// catch exceptions and handle it in your binding
		logger.warn("Communication with device failed: " + ex.getMessage(), ex);
        statusUpdated(ThingStatus.OFFLINE);
    }
}
```

## Updating the Channel State

State updates are sent from the binding to inform the framework, that the state of a channel has been updated. For this the binding developer can call a method from the `BaseThingHandler` class like this:

```java
updateState("channelId", OnOffType.ON)
```    

The call will be delegated to the framework, which changes the state of all bound items. It is binding specific when the channel should be updated. If the device or service supports an event mechanism the ThingHandler should make use of it and update the state every time when the device changes its state.

### Polling for a State

If no event mechanism is available, the binding can poll for the state. The `BaseThingHandlerFactory` has an accessible `ScheduledExecutorService`, which can be used to schedule a job. The following code block shows how to start a polling job in the initialize method of a `ThingHandler`, which runs with an interval of 30 seconds:

```java
@Override
public void initialize() {
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // execute some binding specific polling code
        }
    };
    pollingJob = scheduler.scheduleAtFixedDelay(runnable, 0, 30, TimeUnit.SECONDS);
}
```

Of course, the polling job must be cancelled in the dispose method:

```java
@Override
public void dispose() {
    pollingJob.cancel(true);
}
```

Even if the state has not changed since the last update, the binding should inform the framework, because it indicates that the value is still present.

## Trigger a channel

The binding can inform the framework, that a channel has been triggered. For this the binding developer can call a method from the BaseThingHandler class like this:

```java
triggerChannel("channelId")
```

If an event payload is needed, use the overloaded version:

```java
triggerChannel("channelId", "PRESSED")
```

The call will be delegated to the framework. It is binding specific when the channel should be triggered.

## Updating the Thing Status

The *ThingHandler* must also manage the thing status (see also: [Thing Status Concept](../../concepts/things.html#thing-status)). If the device or service is not working correctly, the binding should change the status to *OFFLINE* and back to *ONLINE*, if it is working again. The status can be updated via an inherited method from the BaseThingHandler class by calling:

```java
updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR);
```    

The second argument of the method takes a `ThingStatusDetail` enumeration value, which further specifies the current status situation. A complete list of all thing statuses and thing status details is listed in the [Thing Status](../../concepts/things.html#thing-status) chapter.

For debugging purposes the binding can also provide an additional status description. This description might contain technical information (e.g. an HTTP status code, or any other protocol specific information, which helps to identify the current problem):

```java
updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, "HTTP 401");
```

After the thing is created, the framework calls the `initialize` method of the handler. At this time the state of the thing is *INTIALIZING* as long as the binding sets it to something else. Because of this the default implementation of the `initialize()` method in the `BaseThingHandler` just changes the status to *ONLINE*.

*Note:* A binding should not set any other state than ONLINE, OFFLINE and UNKNOWN. Additionally, REMOVED must be set after `handleRemoval()` has completed the removal process. All other states are managed by the framework.

Furthermore bindings can specify a localized description of the thing status by providing the reference of the localization string, e.g &#64;text/rate_limit. The corresponding handler is able to provide placeholder values as a JSON-serialized array of strings:

```
&#64;text/rate_limit ["60", "10", "@text/hour"]
```

```
rate_limit=Device is blocked by remote service for {0} minutes. Maximum limit of {1} configuration changes per {2} has been exceeded. For further info please refer to device vendor.
```

## Channel Links

Some bindings might want to start specific functionality for a channel only if an item is linked to the channel.
The `ThingHandler` has two callback methods `channelLinked(ChannelUID channelUID)` and `channelUnlinked(ChannelUID channelUID)`, which are called for every link that is added or removed to/from a channel.
So please be aware of the fact that both methods can be called multiple times.

The `channelLinked` method is only called, if the thing handler has been initialized (status ONLINE/OFFLINE/UNKNOWN).
To actively check if a channel is linked, you can use the `isChannelLinked(ChannelUID channelUID)` method of the `ThingHandlerCallback`.

## Handling Thing Updates

If the structure of a thing has been changed during runtime (after the thing was created), the binding is informed about this change in the ThingHandler within the `thingUpdated` method. The `BaseThingHandler` has a default implementation for this method:

```java
@Override
public void thingUpdated(Thing thing) {
    dispose();
    this.thing = thing;
    initialize();
}
```

If your binding contains resource-intensive logic in your initialize method, you should think of implementing the method by yourself and figuring out what is the best way to handle the change.

For configuration updates, which are triggered from the binding, the framework does not call the `thingUpdated` method to avoid infinite loops.

## Handling Configuration Updates

For changes of the configuration the `ThingHandler` has a separate callback named  `handleConfigurationUpdate(Map<String, Object> configurationParameters)`. This method is called with a map of changed configuration parameters. Depending on the UI multiple parameters can be sent at once, or just a subset or even a single parameter.

The default implementation of this method in the `BaseThingHandler` class does simply apply the configuration parameters, updates the configuration of the thing and reinitializes the handler:

```java
 @Override
public void handleConfigurationUpdate(Map<String, Object> configurationParmeters) {
    // can be overridden by subclasses
    Configuration configuration = editConfiguration();
    for (Entry<String, Object> configurationParmeter : configurationParmeters.entrySet()) {
        configuration.put(configurationParmeter.getKey(), configurationParmeter.getValue());
    }

    // reinitialize with new configuration and persist changes
    dispose();
    updateConfiguration(configuration);
    initialize();
}
```

If configuration needs to be sent to devices, this method should be overridden and some binding-specific logic should be performed. The binding is also responsible for updating the thing, so as in the default implementation `updateConfiguration` should be called, if the configuration was successfully updated. In some radio protocols configuration cannot directly be transmitted to devices, because the communication is done in specific intervals only. The binding could indicate a not yet transmitted configuration change for a device by setting the thing status detail to `CONFIGURATION_PENDING` (see [Thing Status Section](../../concepts/things.html#status-details)).


## Updating the Thing from a Binding

It can happen that the binding wants to update the configuration or even the whole structure of a thing. If the `BaseThingHandler` class is used, it provides some helper methods for modifying the thing.

Please note that not all thing providers are writable. Therefore bindings must not rely on any thing changes to be persisted. 

### Updating the Configuration

Usually the configuration is maintained by the user and the binding is informed about the updated configuration. But if the configuration can also be changed in the external system, the binding should reflect this change and notify the framework about it.

If the configuration should be updated, then the binding developer can retrieve a copy of the current configuration by calling `editConfiguration()`. The updated configuration can be stored as a whole by calling `updateConfiguration(Configuration)`.

Suppose that an external system causes an update of the configuration, which is read in as a `DeviceConfig` instance. The following code shows how to update configuration:

```java
protected void deviceConfigurationChanged(DeviceConfig deviceConfig) {
    Configuration configuration = editConfiguration();
    configuration.put("parameter1", deviceConfig.getValue1());
    configuration.put("parameter2", deviceConfig.getValue2());
    updateConfiguration(configuration);
}
```

The `BaseThingHandler` will propagate the update to the framework, which then notifies all registered listeners about the updated thing. But the thing update is **not** propagated back to the handler through a `thingUpdated(Thing)` call.

### Updating Thing Properties

Thing properties can be updated in the same way as the configuration. The following example shows how to modify two properties of a thing:

```java
protected void devicePropertiesChanged(DeviceInfo deviceInfo) {
	Map<String, String> properties = editProperties();
    properties.put(Thing.PROPERTY_SERIAL_NUMBER, deviceInfo.getSerialNumber());
    properties.put(Thing.PROPERTY_FIRMWARE_VERSION, deviceInfo.getFirmwareVersion());
    updateProperties(properties);
}
```

If only one property must be changed, there is also a convenient method `updateProperty(String name, String value)`. Both methods will only inform the framework that the thing was modified, if at least one property was added, removed or updated. Thing handler implementations must not rely though on properties to be persisted as not all providers support that.

### Updating the Thing Structure

The binding also has the possibility to change the thing structure by adding or removing channels. The following code shows how to use the ThingBuilder to add one channel to the thing:

```java
protected void thingStructureChanged() {
    ThingBuilder thingBuilder = editThing();
    Channel channel = ChannelBuilder.create(new ChannelUID("bindingId:type:thingId:1"), "String").build();
    thingBuilder.withChannel(channel);
    updateThing(thingBuilder.build());
}
```

## Handling Thing Removal

If a thing should be removed, the framework informs the binding about the removal request by calling `handleRemoval` at the thing handler. The thing will not be removed from the runtime until the binding confirms the deletion by setting the thing status to `REMOVED`. If no special removal handling is required by the binding, you do not have to care about removal because the default implementation of this method in the `BaseThingHandler` class just calls `updateStatus(ThingStatus.REMOVED)`.

However, for some radio-based devices it is needed to communicate with the device in order to unpair it safely. After the device was successfully unpaired, the binding must inform the framework that the thing was removed by setting the thing status to `REMOVED`.

After the removal was requested (i.e. the thing is in  `REMOVING` state), it cannot be changed back anymore to `ONLINE`/`OFFLINE`/`UNKNOWN` by the binding. The binding may only initiate the status transition to `REMOVED`.

## Providing the Configuration Status
As on the [XML Reference](xml-reference.html) page explained the *ThingHandler* as handler for the thing entity can provide the configuration status of the thing by implementing the `org.eclipse.smarthome.config.core.status.ConfigStatusProvider` interface. For things that are created by sub-classes of the `BaseThingHandlerFactory` the provider is already automatically registered as an OSGi service if the concrete thing handler implements the configuration status provider interface. Currently the framework provides two base thing handler implementations for the configuration status provider interface:
* `org.eclipse.smarthome.core.thing.binding.ConfigStatusThingHandler` extends the `BaseThingHandler` and is to be used if the configuration status is to be provided for thing entities
* `org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler` extends the `BaseBridgeHandler` and is to be used if the configuration status is to be provided for bridge entities

Sub-classes of these handlers must only override the operation `getConfigStatus` to provide the configuration status in form of a collection of `org.eclipse.smarthome.config.core.status.ConfigStatusMessage`s.

The framework will take care of internationalizing the messages. For this purpose there must be an i18n properties file inside the bundle of the configuration status provider that has a message declared for the message key of the `ConfigStatusMessage`. The actual message key is built by the operation `withMessageKeySuffix(String)` of the message´s builder in the manner that the given message key suffix is appended to *config-status."config-status-message-type."*. As a result depending on the type of the message the final constructed message keys are:
* config-status.information.any-suffix
* config-status.warning.any-suffix
* config-status.error.any-suffix
* config-status.pending.any-suffix
