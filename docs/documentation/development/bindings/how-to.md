---
layout: documentation
---

{% include base.html %}

# Implementing a Binding

A binding is an extension to the Eclipse SmartHome runtime that integrates an external system like a service, a protocol or a single device. Therefore the main purpose of a binding is to translate events from the Eclipse SmartHome event bus to the external system and vice versa. The external system is represented as a set of *Things*. For each *Thing* the binding must provide a proper `ThingHandler` implementation that is able to handle the communication.

In this tutorial you will learn how to implement a simple binding and you will get familiar with important concepts and APIs of Eclipse SmartHome. The [Yahoo Weather Binding](https://github.com/eclipse/smarthome/tree/master/extensions/binding/org.eclipse.smarthome.binding.yahooweather) is taken as example.

## Structure of a Binding

The structure of a binding follows the structure of a typical OSGi bundle project. Therefore there exists a `MANIFEST.MF` file inside the `META-INF` folder and other OSGi artefacts like the `build.properties` file. In the `ESH-INF` folder XML configuration files for Eclipse SmartHome are located. The Java source code is under `src/main/java`.

The structure of the Yahoo Weather Binding:

```
|- ESH-INF
|---- binding
|------- binding.xml
|---- thing
|------- thing-types.xml
|- META-INF
|---- MANIFEST.MF
|- OSGI-INF
|---- YahooWeatherHandlerFactory.xml
|- src
|---- main
|------- java
|---------- [...]
|- build.properties
|- pom.xml
```

## Binding Definition

Every binding needs to define a `binding.xml` file, which is located in the folder `/ESH-INF/binding/`. In this file meta information for a binding like the author and a description, that are accessible at runtime, can be defined. The binding ID is a unique identifier for the binding. The following `binding.xml` shows the binding definition of the Yahoo Weather Binding:

```xml
<binding:binding id="yahooweather"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:binding="http://eclipse.org/smarthome/schemas/binding/v1.0.0"
        xsi:schemaLocation="http://eclipse.org/smarthome/schemas/binding/v1.0.0 http://eclipse.org/smarthome/schemas/binding-1.0.0.xsd">

    <name>YahooWeather Binding</name>
    <description>The Yahoo Weather Binding requests the Yahoo Weather Service
		to show the current temperature, humidity and pressure.</description>
    <author>Kai Kreuzer</author>

</binding:binding>
```

## Describing Things

External systems are represented as *Things* in the Eclipse SmartHome runtime. When starting the implementation of an Eclipse SmartHome binding, you should think about the abstraction of your external system. Different services or devices should be represented as individual *Things* described by a *ThingType*. Each functionality of the *Thing* should be modelled as a `Channel`. A binding should define all *ThingTypes* that are supported by that binding.

Eclipse SmartHome allows you to define your *ThingTypes* in a declarative way through XML files. The XML files must be located at `/ESH-INF/thing/`. A *ThingType* definition must contain the UID and optionally a description and a manufacturer. Moreover, supported channels must be specified. For channels it is important to specify which type of *Item* can be linked to the *Channel*. Below an excerpt of the Yahoo Weather service *ThingType* definition is shown:

```xml
<thing-type id="weather">
    <label>Weather Information</label>
    <description>Provides various weather data from the Yahoo service</description>
    <channels>
        <channel id="temperature" typeId="temperature" />
    </channels>
    <config-description>
        <parameter name="location" type="integer" required="true">
            <label>Location</label>
            <description>Location for the weather information.
                Syntax is WOEID, see https://en.wikipedia.org/wiki/WOEID.
            </description>
        </parameter>
        <parameter name="refresh" type="integer" min="1">
            <label>Refresh interval</label>
            <description>Specifies the refresh interval in seconds.</description>
            <default>60</default>
        </parameter>
    </config-description>
</thing-type>

<channel-type id="temperature">
    <item-type>Number:Temperature</item-type>
    <label>Temperature</label>
    <description>Current temperature</description>
    <category>Temperature</category>
    <state readOnly="true" pattern="%.1f %unit%" />
</channel-type>
```

The channel type definition allows one to specify a category and additional meta information for the state of the linked item. 
Together with the definition of the `readOnly` attribute, user interfaces get an idea how to render an item for this channel.
For example, a channel with the category `Temperature` which is readable only, indicates that this is a sensor for temperature.
In that case the user interface can render an appropriate icon and label for the current value. 

The label is further described by the `state` tags `pattern` attribute: 
In this case the temperature should be rendered with one decimal place followed by the unit the binding specifies for the current measurement. 
The `Number:Temperature` item type describes this channels ability to send state updates using the type `QuantityType` with a value and a unit. 
For detailed information about _dimensions_ and _units_ see the [QuantityType definition](../../concepts/units-of-measurement.html#quantitytype). 

If the `readOnly` flag is set to `false` (which is the default value), the user interface could render a slider to change the temperature, since this means it is temperature actuator. 
Restrictions of the state such as the minimum or maximum value can also be specified.

In order to give user interfaces a chance to render good default UIs for things, the binding should specify as much meta data as possible. For a complete list of possible configuration options and categories please see the [Thing Definition](thing-definition.html#channels) section.

## The ThingHandlerFactory

The `ThingHandlerFactory` is responsible for creating `ThingHandler` instances. Every binding must implement a `ThingHandlerFactory` and register it as OSGi service so that the runtime knows which class needs to be called for creating and handling things. From the generated archetype there already exists a `ThingHandlerFactory`, which can be extended with further *ThingTypes*.

When a new *Thing* is added, the Eclipse SmartHome runtime queries every `ThingHandlerFactory` for support of the *ThingType* by calling the `supportsThingType` method. When the method returns `true`, the runtime calls `createHandler`, which should then return a proper `ThingHandler` implementation.

The `YahooWeatherHandlerFactory` supports only one *ThingType* and instantiates a new `YahooWeatherHandler` for a given thing:

```java
@NonNullByDefault
@Component(configurationPid = "binding.yahooweather", service = ThingHandlerFactory.class)
public class YahooWeatherHandlerFactory extends BaseThingHandlerFactory {
    
    private static final Collection<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(YahooWeatherBindingConstants.THING_TYPE_WEATHER);
    
    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (YahooWeatherBindingConstants.THING_TYPE_WEATHER.equals(thingTypeUID)) {
            return new YahooWeatherHandler(thing);
        }

        return null;
    }
}
```

Constants like the `THING_TYPE_WEATHER` UID and also *Channel* UIDs are typically defined inside a public `BindingConstants` class.

Depending on your implementation, each *ThingType* may use its own handler. It is also possible to use the same handler for different *Things*, or use different handlers for the same *ThingType*, depending on the configuration.

## The ThingHandler

The core part of a binding is the `ThingHandler` implementation. The handler is responsible for translating Eclipse SmartHome commands and states to the external system and vice versa. 

### Handling Commands and Updating the State

For handling commands the `ThingHandler` interface defines the `handleCommand` method. This method is called when a command is sent to an item linked to a channel on a *Thing*. Inside the `handleCommand` method binding specific logic can be executed. The following code snippet shows the handle command method of the Yahoo Weather Binding:

```java
@Override
public void handleCommand(ChannelUID channelUID, Command command) {
    if (command instanceof RefreshType) {
        
        updateWeatherData();
        
        switch (channelUID.getId()) {
            case CHANNEL_TEMPERATURE:
                updateState(channelUID, getTemperature());
                break;
            case CHANNEL_HUMIDITY:
                [...]
        }
    }
}
```

When a `RefreshType` command is sent to the `ThingHandler` it updates the weather data by executing an HTTP call in the `updateWeatherData` method and sends a state update via the `updateState` method. This will update the state of the Item, which is linked to the channel for the given channel UID.

### Lifecycle

The `ThingHandler` has two important lifecycle methods: `initialize` and `dispose`. The `initialize` method is called when the handler is started and `dispose` just before the handler is stopped. Therefore these methods can be used to allocate and deallocate resources. For an example, the Yahoo Weather binding starts and stops a scheduled job within these methods.

### Configuration

*Things* can be configured with parameters. To retrieve the configuration of a *Thing* one can call `getThing().getConfiguration()` inside the `ThingHandler`. The configuration class has the equivalent methods as the `Map` interface, thus the method `get(String key)` can be used to retrieve a value for a given key. 

Moreover the configuration class has a utility method `as(Class<T> configurationClass)` that transforms the configuration into a Java object of the given type. All configuration values will be mapped to properties of the class. The type of the property must match the type of the configuration. Only the following types are supported for configuration values: `Boolean`, `String` and `BigDecimal`.

For example, the Yahoo Weather binding allows configuration of the location and the refresh frequency.

### Properties

*Things* can have properties. If you would like to add meta data to your thing, e.g. the vendor of the thing, then you can define your own thing properties by simply adding them to the thing type definition. The properties section [here](thing-definition.html#Properties) explains how to specify such properties.

To retrieve the properties one can call the operation `getProperties` of the corresponding `org.eclipse.smarthome.core.thing.type.ThingType` instance. If a thing will be created for this thing type then its properties will be automatically copied into the new thing instance. Therefore the `org.eclipse.smarthome.core.thing.Thing` interface also provides the `getProperties` operation to retrieve the defined properties. In contrast to the `getProperties` operation of the thing type instance the result of the thing´s `getProperties` operation will also contain the properties updated during runtime (cp. the thing handler [documentation](thing-handler.html)).

## Bridges

In the domain of an IoT system there are often hierarchical structures of devices and services. For example, one device acts as a gateway that enables communication with other devices that use the same protocol. In Eclipse SmartHome this kind of device or service is called *Bridge*. Philips Hue is one example of a system that requires a bridge. The Hue gateway is an IP device with an HTTP API, which communicates over the ZigBee protocol with the Hue bulbs. In the Eclipse SmartHome model the Hue gateway is represented as a *Bridge* with connected *Things*, that represent the Hue bulbs. *Bridge* inherits from *Thing*, so that it also has *Channels* and all other features of a thing, with the addition that it also holds a list of things.

When implementing a binding with *Bridges*, the logic to communicate with the external system is often shared between the different `ThingHandler` implementations. In that case it makes sense to implement a handler for the *Bridge* and delegate the actual command execution from the *ThingHandler* to the *BridgeHandler*. To access the *BridgeHandler* from the *ThingHandler*, call `getBridge().getHandler()`

The following excerpt shows how the `HueLightHandler` delegates the command for changing the light state to the `HueBridgeHandler`:

```java
@Override
public void handleCommand(ChannelUID channelUID, Command command) {

    HueBridgeHandler hueBridgeHandler = (HueBridgeHandler) getBridge().getHandler();

    switch (channelUID.getId()) {
        case CHANNEL_ID_COLOR_TEMPERATURE:
            StateUpdate lightState = lightStateConverter.toColorLightState(command);    
            hueBridgeHandler.updateLightState(getLight(), lightState);
            break;    
        case CHANNEL_ID_COLOR: 
            // ...
    }
}
```

Inside the `BridgeHandler` the list of *Things* can be retrieved via the `getThings()` call.

## Documentation

When you have finished the implementation of the binding, you should spend a minute to also document it. Please find some [details here](docs.html).
