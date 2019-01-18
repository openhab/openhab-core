---
layout: documentation
---

{% include base.html %}

# Textual Configuration

Eclipse SmartHome provides the possibility to do fully text-based system setups. This is done using domain specific languages (DSLs) for the different kinds of artifacts.
In addition, configuration files can be used to configure OSGi services which are able to be configured by Configuration Admin.  

## Thing Configuration DSL

Things can be configured with a Domain specific Language (DSL). It is recommended to use the Eclipse SmartHome Designer for editing the DSL files, because it supports validation and auto-completion.

Thing configuration files must be placed under the `things` folder inside the Eclipse SmartHome `conf` folder and must end with the suffix `.things`.

## Defining Things

Things can be defined as followed:

```
Thing yahooweather:weather:berlin [ location=638242 ]
Thing yahooweather:weather:losangeles "Los Angeles" @ "home" [ location=2442047, unit="us", refresh=120 ]
```

The first keyword defines whether the entry is a bridge or a thing. The next statement defines the UID of the thing which contains of the following three segments: binding id, thing type id, thing id. So the first two segments must match to thing type supported by a binding (e.g. `yahooweather:weatheryahooweather`), whereas the thing id can be freely defined. Optionally, you may provide a label in order to recognize it easily, otherwise the default label from the thing type will be displayed.

To help organizing your things, you also may define a location (here: "home"), which should point to an item. This item can either be a simple String item carrying e.g. the room name, or you may of course also use a Location item containing some geo coordinates.  

Inside the squared brackets configuration parameters of the thing are defined.

The type of the configuration parameter is determined by the binding and must be specified accordingly in the DSL. If the binding requires a text the configuration parameter must be specified as a decimal value: `location=2442047`. Other types are for example boolean values (`refreshEnabled=true`).

For each thing entry in the DSL the framework will create a thing by calling the ThingFactory of the according binding.

### Shortcut

It is possible to skip the `Thing` keyword: `yahooweather:weather:berlin [ location=638242 ]`

## Defining Bridges

The DSL also supports the definition of bridges and contained things. The following configuration shows the definition of a hue bridge with two hue lamps:

```
Bridge hue:bridge:mybridge [ ipAddress="192.168.3.123" ] {
	Thing 0210 bulb1 [ lightId="1" ]
	Thing 0210 bulb2 [ lightId="2" ]
}
```

Within the curly brackets things can be defined, that should be members of the bridge. For the contained thing only the thing type ID and thing ID must be defined (e.g. `0210 bulb1`). So the syntax is `Thing <thingTypeId> <thingId> []`. The resulting UID of the thing is `hue:0210:mybridge:bulb1`.

Bridges that are defined somewhere else can also be referenced in the DSL:

```
Thing hue:0210:mybridge:bulb (hue:bridge:mybridge) [lightId="3"]
```

The referenced bridge is specified in the parentheses. Please notice that the UID of the thing also contains the bridge ID as third segment. For the contained notation of things the UID will be inherited and the bridge ID is automatically taken as part of the resulting thing UID.

## Defining Channels

It is also possible to manually define channels in the DSL. Usually this is not needed, as channels will be automatically created by the binding based on the thing type description. But there might be some bindings, that require the manual definition of channels.

### State channels

```
Thing yahooweather:weather:losangeles [ location=2442047, unit="us", refresh=120 ] {
	Channels:
		State String : customChannel1 "My Custom Channel" [
			configParameter="Value"
		]
		State Number : customChannel2 []
}
```

Each channel definition must be placed inside the curly braces and begin with the keyword `State` followed by the accepted item type (e.g. String). After this the channel ID follows with the configuration of a channel. The framework will merge the list of channels coming from the binding and the user-defined list in the DSL.

As state channels are the default channels, you can omit the `State` keyword, the following example creates the same channels as the example above:

```
Thing yahooweather:weather:losangeles [ location=2442047, unit="us", refresh=120 ] {
	Channels:
		String : customChannel1 "My Custom Channel" [
			configParameter="Value"
		]
		Number : customChannel2 []
}
```

You may optionally give the channel a proper label (like "My Custom Channel" in the example above) so you can distinguish the channels easily.


### Trigger channels

```
Thing yahooweather:weather:losangeles [ location=2442047, unit="us", refresh=120 ] {
	Channels:
		Trigger String : customChannel1 [
			configParameter="Value"
		]
}
```

Trigger channels are defined with the keyword `Trigger` and only support the type String.

### Referencing existing channel types

Many bindings provide standalone channel type definitions like this:  

```
<thing:thing-descriptions bindingId="yahooweather" [...]>
    <channel-type id="temperature">
        <item-type>Number</item-type>
        <label>Temperature</label>
        <description>Current temperature in degrees celsius</description>
        <category>Temperature</category>
        <state readOnly="true" pattern="%.1f °C">
        </state>
    </channel-type>
    [...]
</thing:thing-descriptions>
```

They can be referenced within a thing's channel definition, so that they need to be defined only once and can be reused for many channels. You may do so in the DSL as well:

```
Thing yahooweather:weather:losangeles [ location=2442047, unit="us", refresh=120 ] {
    Channels:
        Type temperature : my_yesterday_temperature "Yesterday's Temperature"
}
```

The `Type` keyword indicates a reference to an existing channel definition. The channel kind and accepted item types of course are takes from the channel definition, therefore they don't need to be specified here again.

You may optionally give the channel a proper label (like "Yesterday's Temperature" in the example above) so you can distinguish the channels easily. If you decide not to, then the label from the referenced channel type definition will be used.

## Configuring OSGi Services

In order to provide file based, static configuration for OSGi services, Eclipse SmartHome provides a way to read, parse and observe configuration files.
The configuration which is read from these files will be passed to the OSGi Configuration Admin.
The fundamentals of configuring OSGi services are described [here](http://enroute.osgi.org/doc/217-ds.html) in great detail.

### Default Configuration File

Eclipse SmartHome reads its basic configuration from the default configuration file `conf/smarthome.cfg` in the programs root folder.
The path to this file can be altered using the program argument `smarthome.servicecfg`.
In case only the `conf` folder path should be altered the program argument `smarthome.configdir` can be used (be aware that this will also change the location for the `things`, `items` and all other configuration folders).
Configurations for OSGi services are kept in a subfolder that can be provided as a program argument `smarthome.servicedir` (default is "services"). Any file in this folder with the extension .cfg will be processed.

### Configuration File Format
The basic configuration file format is very simple. This format is used in the `smarthome.cfg` to address different services from one file:

```
<configuration_pid1>:<key1>=<value1>
<configuration_pid1>:<key2>=<value2>

<configuration_pid2>:<key>=<value>

<configuration_pid3>:<key>=<value>
```

The line prefix `configuration_pid[1-3]` correspond to the configuration PID which is configured for the specific OSGi service.
`<key>` and `<value>` define the configuration option for the specific service.

With every line denoting a specific configuration PID, several services can be configured using just one configuration file.
In addition the configuration PID can also be derived from the filename.
Given the configuration file `conf/services/org.eclipse.smarthome.basicui.cfg` with content `defaultSitemap=demo` will configure the `defaultSitemap` option of the Basic UI service, giving it the value `demo`.
This way a service can be configured just by giving `<key>=<value>` pairs in the file.

When providing the PID by filename or by line prefix there is one additional shortcut:
The files are processed line-by-line from top to bottom.
The last defined PID (either by file name or by line prefix) will be kept to be used for the next entry.
This makes the following example a valid and fully functional service configuration for multiple services.

Although not recommended, the file `conf/services/org.eclipse.smarthome.threadpool.cfg` with the following content:

```property
thingHandler=3
discovery=3

org.eclipse.smarthome.basicui:defaultSitemap=demo
enableIcons=true
condensedLayout=false
```

will configure the `org.eclipse.smarthome.threadpool` service to have the `thingHandler` and `discovery` option set to `3` and also configure the `org.eclipse.smarthome.basicui` service with three different options.

All the above configuration files will be observed for changes.
When the framework detects a change (or even a new *.cfg file) the changes will be read and applied to the specific service.
Since a single service may be configured using multiple files this observation will _not delete_  configuration options from the Configuration Admin.
In order to track deletion of configuration entries and files the configuration has to be marked as an exclusive service configuration.

### Exclusive Service Configuration

The framework will track exclusively marked service configurations for file or entry deletions. This way a configuration can be removed during runtime. For factory services even a whole service instance can be added and removed during runtime.

To mark a configuration file exclusively for one service the _first line_ has to define the configuration PID in the format `pid:<configuration_pid>`.
By giving this PID marker the framework creates an exclusive configuration for the contents of this file.
Other files without this marker which also define configurations for the given PID will be ignored for this PID.

The file `conf/services/myService.cfg` with contents

```property
pid:org.eclipse.smarthome.bundle.myService
timeout=30
callback=MyCallback

```

will exclusively configure the OSGi service with configuration_pid `org.eclipse.smarthome.bundle.myService` set.
Any other configuration for this PID in other configuration files will be ignored now.
In contrast, when removing the line `timeout=30` from the file, the service's `modified` method will be called with a new configuration which does not include the `timeout` option anymore.
When removing the whole file the configuration will completely be deleted from the Configuration Admin.


### Factory Service Configuration

Using the format of an exclusive service configuration it is also possible to create several instances of a specific service.
By giving a unique context along with the exclusive PID in the format `pid:<configuration_pid>#<context>` the framework will create a new instance for this service.
