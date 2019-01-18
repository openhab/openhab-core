---
layout: documentation
---

{% include base.html %}

# Thing Type Definitions

In order to work with things, some meta information about them is needed. This is provided through 'ThingType' definitions, which describe details about their functionality and configuration options.

Technically, the thing types are provided by [ThingTypeProvider](https://github.com/eclipse/smarthome/blob/master/bundles/core/org.eclipse.smarthome.core.thing/src/main/java/org/eclipse/smarthome/core/thing/binding/ThingTypeProvider.java)s. 
Eclipse SmartHome comes with an implementation of such a provider that reads XML files from the folder `ESH-INF/thing` of bundles. Although we refer to this XML syntax in the following, you also have the option to provide directly object model instances through your own provider implementation. 
The same applies for the channel types. 
The [ChannelTypeProvider](https://github.com/eclipse/smarthome/blob/master/bundles/core/org.eclipse.smarthome.core.thing/src/main/java/org/eclipse/smarthome/core/thing/type/ChannelTypeProvider.java) interface can be registered as OSGi service to provide channel types programmatically. 
When implementing a dynamic `ThingTypeProvider` you can also refer to the channel types that are defined inside XML files.

## Things

Things represent devices or services that can be individually added to, configured or removed from the system. 
They either contain a set of channels or a set of channel groups. 
A bridge is a specific type of thing as it can additionally provide access to other Things as well. 
Which Things can be associated through which bridge type is defined within the description of a thing:

```xml
    <thing-type id="thingTypeID">
        <supported-bridge-type-refs>
            <bridge-type-ref id="bridgeTypeID" />
        </supported-bridge-type-refs>
        <label>Sample Thing</label>
        <description>Some sample description</description>
        <category>Lightbulb</category>
		...
    </thing-type>
```

Bindings may optionally set the listing of a thing type. By doing do, they indicate to user interfaces whether it should be shown to the users or not, e.g. when pairing things manually:

```xml
    <thing-type id="thingTypeID" listed="false">
        ...
    </thing-type>
```

Thing types are listed by default, unless specified otherwise. 
Hiding thing types potentially makes sense if they are deprecated and should not be used anymore. 
Also, this can be useful if users should not be bothered with distinguishing similar devices which for technical reasons have to have separate thing types. 
In that way, a generic thing type could be listed for users and a corresponding thing handler would change the thing type immediately to a more concrete one, handing over control to the correct specialized handler.

### Thing Categories

A description about thing categories as well as an overview about which categories exist can be found in our [categories overview](../../concepts/categories.html).

## Channels

A channel describes a specific functionality of a thing and can be linked to an item.
So the basic information is, which command types the channel can handle and which state it sends to the linked item.
This can be specified by the accepted item type.
Inside the thing type description XML file a list of channels can be referenced.
The channel type definition is specified on the same level as the thing type definition.
That way channels can be reused in different things.

The granularity of channel types should be on its semantic level, i.e. very fine-grained:
If a Thing measures two temperature values, one for indoor and one for outdoor, this should be modelled as two different channel types.
Overriding labels of a channel type must only be done if the very same functionality is offered multiple times, e.g. having an actuator with 5 relays, which each is a simple "switch", but you want to individually name the channels (1-5).

The following XML snippet shows a thing type definition with 2 channels and one referenced channel type:

```xml
<thing-type id="thingTypeID">
    <label>Sample Thing</label>
    <description>Some sample description</description>
    <channels>
        <channel id="switch" typeId="powerSwitch" />
        <channel id="temperature" typeId="setpointTemperature" />
    </channels>
</thing-type>
<channel-type id="setpointTemperature" advanced="true">
    <item-type>Number</item-type>
    <label>Setpoint Temperature</label>
    <category>Temperature</category>
    <state min="12" max="30" step="0.5" pattern="%.1f °C" readOnly="false" />
</channel-type>
```

In order to reuse identical channels in different bindings a channel type can be system-wide.
A channel type can be declared as system-wide by setting its `system` property to true and can then be referenced using a `system.` prefix in a `channel` `typeId` attribute in any binding - note that this should only be done in the core framework, but not by individual bindings!

The following XML snippet shows a system channel type definition and thing type definition that references it:

```xml
<thing-type id="thingTypeID">
    <label>Sample Thing</label>
    <description>Some sample description</description>
    <channels>
        <channel id="s" typeId="system.system-channel" />
    </channels>
</thing-type>
<channel-type id="system-channel" system="true">
    <item-type>Number</item-type>
    <label>System Channel</label>
    <category>QualityOfService</category>
</channel-type>
```

### System State Channel Types

There exist system-wide channel types that are available by default:

| Channel Type ID      | Reference typeId            | Item Type            | Category         | Description                                                                                                                                                                                                             |
|----------------------|-----------------------------|----------------------|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| signal-strength      | system.signal-strength      | Number               | QualityOfService | Represents signal strength of a device as a Number with values 0, 1, 2, 3 or 4; 0 being worst strength and 4 being best strength.                                                                                       |
| low-battery          | system.low-battery          | Switch               | Battery          | Represents a low battery warning with possible values on (low battery) and off (battery ok).                                                                                                                                                           |
| battery-level        | system.battery-level        | Number               | Battery          | Represents the battery level as a percentage (0-100%). Bindings for things supporting battery level in a different format (e.g. 4 levels) should convert to a percentage to provide a consistent battery level reading. |
| power                | system.power                | Switch               | -                | Turn a device on/off.                                                                                                                                                                                                   |
| brightness           | system.brightness           | Dimmer               | Light            | Brightness of a bulb (0-100%).                                                                                                                                                                                          |
| color                | system.color                | Color                | ColorLight       | Color of a bulb.                                                                                                                                                                                                        |
| color-temperature    | system.color-temperature    | Dimmer               | ColorLight       | Color temperature of a bulb (0-100%). 0% should be the coldest setting (highest Kelvin value).                                                                                                                          |
| location             | system.location             | Location             | -                | Location in lat.,lon.,height coordinates.                                                                                                                                                                               |
| motion               | system.motion               | Switch               | Motion           | Motion detected by the device (ON if motion is detected).                                                                                                                                                               |
| mute                 | system.mute                 | Switch               | SoundVolume      | Turn on/off the volume of a device.                                                                                                                                                                                     |
| volume               | system.volume               | Dimmer               | SoundVolume      | Change the sound volume of a device (0-100%).                                                                                                                                                                           |
| media-control        | system.media-control        | Player               | MediaControl     | Control for a media player.                                                                                                                                                                                             |
| media-title          | system.media-title          | String               | -                | Title of a (played) media file.                                                                                                                                                                                         |
| media-artist         | system.media-artist         | String               | -                | Artist of a (played) media file.                                                                                                                                                                                        |
| outdoor-temperature  | system.outdoor-temperature  | Number:Temperature   | Temperature      | Current outdoor temperature.                                                                                                                                                                                            |
| wind-direction       | system.wind-direction       | Number:Angle         | Wind             | Wind direction in degrees (0-360°).                                                                                                                                                                                     |
| wind-speed           | system.wind-speed           | Number:Speed         | Wind             | Wind speed                                                                                                                                                                                                              |
| atmospheric-humidity | system.atmospheric-humidity | Number:Dimensionless | Humidity         | Atmospheric humidity in percent.                                                                                                                                                                                        |
| barometric-pressure  | system.barometric-pressure  | Number:Pressure      | Pressure         | Barometric pressure                                                                                                                                                                                                     |

For further information about categories see the [categories page](../../concepts/categories.html).

The `advanced` property indicates whether this channel is a basic or a more specific functionality of the thing. 
If `advanced` is set to `true` a user interface may hide this channel by default. 
The default value is `false` and thus will be taken if the `advanced` attribute is not specified. 
Especially for complex devices with a lot of channels, only a small set of channels - the most important ones - should be shown to the user to reduce complexity. 
Whether a channel should be declared as `advanced` depends on the device and can be decided by the binding developer. 
If a functionality is rarely used it should be better marked as `advanced`.

The following XML snippet shows a trigger channel:

```xml
<thing-type id="thingTypeID">
    <label>Sample Thing</label>
    <description>Some sample description</description>
    <channels>
        <channel id="s" typeId="trigger-channel" />
    </channels>
</thing-type>
<channel-type id="trigger-channel">
    <kind>trigger</kind>
    <label>Trigger Channel</label>
    <event>
        <options>
            <option value="PRESSED">pressed</option>
            <option value="RELEASED">released</option>
            <option value="DOUBLE_PRESSED">double pressed</option>
        </options>
    </event>
</channel-type>
```

This channel can emit the event payloads `PRESSED`, `RELEASED` and `DOUBLE_PRESSED`.

If no `<event>` tag is specified, the channel can be triggered, but has no event payload.
If an empty `<event>` tag is specified, the channel can trigger any event payload.

### System Trigger Channel Types

There exist system-wide trigger channel types that are available by default:

| Channel Type ID | Reference typeId       | Description  |
|-----------------|------------------------|------------- |
| trigger         | system.trigger         | Can only trigger, no event payload |
| rawbutton       | system.rawbutton       | Can trigger `PRESSED` and `RELEASED` |
| button          | system.button          | Can trigger `SHORT_PRESSED`, `DOUBLE_PRESSED` and `LONG_PRESSED` |
| rawrocker       | system.rawrocker       | Can trigger `DIR1_PRESSED`, `DIR1_RELEASED`, `DIR2_PRESSED` and `DIR2_RELEASED` |

In the following sections the declaration and semantics of tags, state descriptions and channel categories will be explained in more detail. 
For a complete sample of the thing types XML file and a full list of possible configuration options please see the [XML Configuration Guide](xml-reference.html).

### Default Tags

The XML definition of a ThingType allows to assign default tags to channels. 
All items bound to this channel will automatically be tagged with these default tags. 
The following snippet shows a 'Lighting' tag definition:

```xml
<tags>
    <tag>Lighting</tag>
</tags>
```

Please note that only tags from a pre-defined tag library should be used.
This library is still t.b.d., and only a very small set of tags are defined so far:

| Tag                | Item Types                 | Description                                                                           |
|--------------------|----------------------------|---------------------------------------------------------------------------------------|
| Lighting           | Switch, Dimmer, Color      | A light source, either switchable, dimmable or color                                  |
| Switchable         | Switch, Dimmer, Color      | An accessory that can be turned off and on.                                           |
| CurrentTemperature | Number, Number:Temperature | An accessory that provides a single read-only temperature value.                      |
| TargetTemperature  | Number, Number:Temperature | A target temperature that should engage a thermostats heating and cooling actions.   |
| CurrentHumidity    | Number                     | An accessory that provides a single read-only value indicating the relative humidity. |


### State Description

The state description allows to specify restrictions and additional information for the state of an item, that is linked to the channel. 
Some configuration options are only valid for specific item types. 
The following XML snippet shows the definition for a temperature actuator channel:

```xml
<state min="12" max="30" step="0.5" pattern="%.1f %unit%" readOnly="false"></state>
```

The attributes `min` and `max` can only be declared for channel with the item type `Number`. 
It defines the range of the numeric value. 
The Java data type is a BigDecimal. 
For example user interfaces can create sliders with an appropriate scale based on this information. 
The `step` attribute can be declared for `Number` and `Dimmer` items and defines what is the minimal step size that can be used. 
The `readonly` attribute can be used for all item types and defines if the state of an item can be changed. 
For all sensors the `readonly` attribute should be set to `true`. 
The `pattern` attribute can be used for `Number` and  `String` items. 
It gives user interface a hint how to render the item. 
The format of the pattern must be compliant to the [Java Number Format](http://docs.oracle.com/javase/tutorial/java/data/numberformat.html). 
The pattern can be localized (see also [Internationalization](../../features/internationalization.html)).
The special pattern placeholder `%unit%` is used for channels which bind to items of type `Number:<dimension>` which define a dimension for unit support. 
These channels will send state updates of type [QuantityType](../../concepts/units-of-measurement.html#quantitytype) and the unit is then rendered for the placeholder. 

Some channels might have only a limited and countable set of states. 
These states can be specified as options. 
A `String` item must be used as item type. 
The following XML snippet defines a list of predefined state options:

```xml
<state readOnly="true">
    <options>
        <option value="HIGH">High Pressure</option>
        <option value="MEDIUM">Medium Pressure</option>
        <option value="LOW">Low Pressure</option>
    </options>
</state>
```

The user interface can use these values to render labels for values or to provide a selection of states, when the channel is writable. 
The option labels can also be localized.

#### Dynamic State Description

In situations where the static definition of a state description is not sufficient a binding may implement a `DynamicStateDescriptionProvider`.
It allows to provide a StateDescription based on the specific `Channel`.
Also implement this interface if you want to provide dynamic state options.
The original `StateDescription` is available for modification and enhancement.
The `StateDescriptionFragmentBuilder` can be used to only provide the information which is available at the time of construction.

### Channel Categories

A description about channel categories as well as an overview about which categories exist can be found in out [categories overview](../../concepts/categories.html).

### Channel Groups

Some devices might have a lot of channels. 
There are also complex devices like a multi-channel actuator, which is installed inside the switchboard, but controls switches in other rooms. 
Therefore channel groups can be used to group a set of channels together into one logical block. 
A thing can only have direct channels or channel groups, but not both.

Inside the thing types XML file channel groups can be defined like this:

```xml
<thing-type id="multiChannelSwitchActor">
    <!-- ... -->
    <channel-groups>
        <channel-group id="switchActor1" typeId="switchActor" />
        <channel-group id="switchActor2" typeId="switchActor" />
    </channel-groups>
    <!-- ... -->
</thing-type>    
```

The channel group type is defined on the same level as the thing types and channel types. 
The group type must have a label, an optional description, and an optional [category](../../concepts/categories.html). 
Moreover the list of contained channels must be specified:

```xml
<channel-group-type id="switchActor">
    <label>Switch Actor</label>
    <description>This is a single switch actor with a switch channel</description>
    <category>Light</category>
    <channels>
        <channel id="switch" typeId="switch" />
    </channels>
</channel-group-type>
```

When a thing will be created for a thing type with channel groups, the channel UID will contain the group ID in the last segment divided by a hash (#).
If an Item should be linked to a channel within a group, the channel UID would be `binding:multiChannelSwitchActor:myDevice:switchActor1#switch` for the XML example before.
Details about the category can be found in our [categories overview](../../concepts/categories.html).

## Properties

Solutions based on Eclipse SmartHome might require meta data from a device. 
These meta data could include:

- general device information, e.g. the device vendor, the device series or the model ID, ...
- device characteristics, e.g. if it is battery based, which home automation protocol is used, what is the current firmware version or the serial number, ...
- physical descriptions, e.g. what is the size, the weight or the color of the device, ...
- any other meta data that should be made available for the solution by the binding

Depending on the solution the provided meta data can be used for different purposes. 
Among others the one solution could use the data during a device pairing process whereas another solution might use the data to group the devices/things by the vendors or by the home automation protocols on a user interface. 
To define such thing meta data the thing type definition provides the possibility to specify so-called `properties`:

```xml
    <thing-type id="thingTypeId">
        ...
        <properties>
             <property name="vendor">MyThingVendor</property>
             <property name="modelId">thingTypeId</property>
             <property name="protocol">ZigBee</property>
             ...
        </properties>
		...
    </thing-type>
```

In general each `property` must have a name attribute which should be written in camel case syntax. 
The actual property value is defined as plain text and is placed as child node of the property element. 
It is recommended that at least the vendor and the model id properties are specified here since they should be definable for the most of the devices. 
In contrast to the properties defined in the 'ThingType' definitions the thing handler [documentation](thing-handler.html) explains how properties can be set during runtime.

### Representation Property

A thing type can contain a so-called `representation property`. 
This optional property contains the _name_ of a property whose value can be used to uniquely identify a device.
The `thingUID` cannot be used for this purpose because there can be more than one thing for the same device.

Each physical device normally has some kind of a technical identifier which is unique.
This could be a MAC address (e.g. Hue bridge, camera, all IP-based devices), a unique device id (e.g. a Hue lamp) or some other property that is unique per device type. 
This property is normally part of a discovery result for that specific thing type. 
Having this property identified per binding it could be used as the `representation property` for this thing.

The `representation property` will be defined in the thing type XML: 

```xml
    <thing-type id="thingTypeId">
        ...
        <properties>
            <property name="vendor">Philips</property>
        </properties>
        <representation-property>serialNumber</representation-property>
        ...
    </thing-type>
```

Note that the `representation property` is normally not listed in the `properties` part of the thing type, as this part contains static properties, that are the same for each thing of this thing type. 
The name of the `representation property` identifies a property that is added to the thing in the thing handler upon successful initialization. 

### Representation Property and Discovery

The representation property is being used to auto-ignore discovery results of devices that already have a corresponding thing. 
This happens if a device is being added manually. 
If the new thing is going online, the auto-ignore service of the inbox checks if the inbox already contains a discovery result of the same type where the value of its `representation property` is identical to the value of the `representation property` of the newly added thing. 
If this is the case, the result in the inbox is automatically set to ignored. 
Note that this result is automatically removed when the manual added thing is eventually removed. 
A new discovery would then automatically find this device again and add it to the inbox properly. 

## Formatting Labels and Descriptions

The label and descriptions for things, channels and config descriptions should follow the following format. 
The label should be short so that for most UIs it does not spread across multiple lines. 
The description can contain longer text to describe the thing in more detail. 
Limited use of HTML tags is permitted to enhance the description - if a long description is provided, the first line should be kept short, and a line break (```<br />```) placed at the end of the line to allow UIs to display a short description in limited space.

Configuration options should be kept short so that they are displayable in a single line in most UIs. 
If you want to provide a longer description of the options provided by a particular parameter, then this should be placed into the ```<description>``` of the parameter to keep the option label short. 
The description can include limited HTML to enhance the display of this information.

The following HTML tags are allowed : ```<b>, <br />, <em>, <h1>, <h2>, <h3>, <h4>, <h5>, <h6>, <i>, <p>, <small>, <strong>, <sub>, <sup>, <ul>, <ol>, <li>```. 
These must be inside the XML escape sequence - e.g. ```<description><![CDATA[ HTML marked up text here ]]></description>```.

## Auto Update Policies

Channel types can optionally define a policy with respect to the auto update handling. 
This influences the decision within the framework if an auto-update of the item's state should be sent in case a command is received for it.
The auto update policy typically is inherited by the channel from its channel type. 
Nevertheless, this value can be overridden in the channel definition.

In this example, an auto update policy is defined for the channel type, but is overridden in the channel definition:

```xml
<channel-type id="channel">
    <label>Channel with an auto update policy</label>
    <autoUpdatePolicy>recommend</autoUpdatePolicy>
</channel-type>

<thing-type id="thingtype">
    <label>Sample Thing</label>
    <description>Thing type which overrides the auto update policy of a channel</description>
    <channels>
      <channel id="instance" typeId="channel">
        <autoUpdatePolicy>default</autoUpdatePolicy>
      </channel>
    </channels>
</thing-type>
```

The following policies are supported:

* **veto**: No automatic state update should be sent by the framework. The thing handler will make sure it sends a state update and it can do it better than just converting the command to a state.
* **default**: The binding does not care and the framework may do what it deems to be right. The state update which the framework will send out normally will correspond the command state anyway. This is the default if no other policy is set explicitly.
* **recommend**: An automatic state update should be sent by the framework because no updates are sent by the binding. This usually is the case when devices don't expose their current state to the handler.
