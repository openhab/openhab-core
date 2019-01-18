---
layout: post
title: "Units of Measurement with Eclipse SmartHome"
date:   2018-02-22
image: "2018-02-22-units-of-measurement.png"
published: true
---

A lot of functionality in a smart home setup is based on environmental conditions which until now are measured without an explicit unit. Now Eclipse SmartHome comes with full "Units of Measurement" support to make the unit part of the item state and provide automatic or specific unit conversion. <!--more--> 

##### Overview
A lot of functionality in a smart home setup is based on environmental conditions. Sensors will report temperatures from all sorts of locations, weather data is collected and displayed, or in house luminance is measured to control steady lighting conditions.
All these sensor data is not only measured with a scalar value which represents the current state but has an implicit unit attached to it. Temperatures are usually given in either degree Celsius or degree Fahrenheit. Wind speed will be displayed in either kilometers per hour (km/h) or miles per hour (mph). The measured value might even be given in a device specific unit or value scale.
Up to now these units where not represented in Eclipse SmartHome and the user had to "know" its units and model items and rules accordingly.
In order to unify the different values and units and let the smart home make use of it, Eclipse SmartHome now comes with support for "Units of Measurement" or in short "UoM".

##### Units of Measurement
From the [UoM Wikipedia](https://en.wikipedia.org/wiki/Units_of_measurement) article: "A unit of measurement is a definite magnitude of a quantity, defined and adopted by convention or by law, that is used as a standard for measurement of the same kind of quantity. Any other quantity of that kind can be expressed as a multiple of the unit of measurement."
Units are organised in systems of units and the global standard for units is the [International System of Units (SI)](https://en.wikipedia.org/wiki/International_System_of_Units).
Apart from SI there are other systems in use like the imperial unit system. The same physical quantity can be expressed in different units and mathematical transformations exist to convert between them.
Depending on the users locale or cultural preference a specific unit might fit the users needs. For temperature it is common to use degree Celsius (°C) in most places around the globe but degree Fahrenheit (°F) is used in the United States of America.

##### UoM data with QuantityType
Sensor devices and internet services connected to Eclipse SmartHome as Things provide their scalar values in an implicit unit of measurement. The binding developer knows what exact unit the sensor or service data uses and can pass those values to the framework using the new [`QuantityType`]({{ site.baseurl }}{% link documentation/concepts/units-of-measurement.md %}). QuantityType represents the scalar value with the corresponding unit. It also serves as the main API to convert between different units.
The Yahoo Weather binding makes use of `QuantityType` and publishes state updates in the following form:

```(java)
QuantityType<Temperature> state = new QuantityType<>(tempDouble, SIUnits.CELSIUS);
updateState(temperatureChannelUID, state);
```

For this example the binding developer knows from the Yahoo API description that degree Celsius is used for temperature values. For pressure values the API description states that Hecto Pascal (hPa) is used, so a state update for the pressure channel looks like this:

```(java)
QuantityType<Pressure> state = new QuantityType<>(pressDouble, HECTO(SIUnits.PASCAL));
updateState(pressureChannelUID, state);
```

In the temperature example the state is marked to be a "\<Temperature\>" typed QuantityType. It will only accept units which correspond to the dimension type, like degree Celsius, degree Fahrenheit or kelvin.
In the same way the Thing's Channels and the linked Number items have to be typed to match against each other. Both Channel and Item definition use an extended item type format which passes the expected dimension to a Number item.

The temperature channel definition for Yahoo Weather looks like this:

```(xml)
<channel-type id="temperature">
    <item-type>Number:Temperature</item-type>
    <label>Temperature</label>
    <description>Current temperature</description>
    <category>Temperature</category>
    <state readOnly="true" pattern="%.1f %unit%"/>
</channel-type>
```

A corresponding item definition in the `*.items` DSL is:

```
Number:Temperature Weather_Temperature "Outside Temperature [%.1f %unit%]" { channel="yahooweather:weather:berlin:temperature" }
```

Both definitions use the extended item type `Number:Temperature` which configures the Number item to only accept state updates with units from the dimension "Temperature".
For backward compatibility a Number item without a specified dimension will receive state updates as DecimalType. For this situation the scalar value from the QuantityType is taken without any conversion and passed on to the item. 

##### Conversion
Once a binding provides state updates with QuantityTypes the unit which should be used on the item can be different from the unit selected by the binding.

###### Locale Conversion
The default behavior of the framework is to convert the source value to the unit which is defined to be the default for the current measurement system.
Currently two measurement systems are supported by Eclipse SmartHome: For a system locale which is set to "US" or a system language which is set to "en-LR" (which is Liberia) the imperial system is chosen. The SI system of units is chosen otherwise. 

###### Fixed Measurement System
Apart from the locale based selection the framework may be configured to use a predefined measurement system by configuring the `UnitProvider` component.

###### Item State Conversion
In case the default conversion to the system's measurement system does not fit the current use case, items may define their own target unit. The formatter pattern in the item's state description may use the `%unit%` placeholder to render the default unit (see item definition example above) or just define a fixed unit to which state updates will be converted:

```
Number:Temperature Weather_Temperature "Outside Temperature [%.1f K]" { channel="yahooweather:..." }
```

With this definition the target unit is defined to be kelvin (K) so all state updates to this item will be converted to kelvin.

##### Rendering Units in UIs
The same unit conversion mechanism used by the item's state description can be used in sitemap descriptions. The `label` property of a widget in a sitemap and also the `mapping` property of `Switch` and `Select` widgets support the unit definition in the formatter pattern. 

##### QuantityType commands
Any number item which is defined to use QuantityType states will also accept commands from matching units for its dimension. These commands will be forwarded to the corresponding channel(s).
From Paper UI, Basic UI and Classic UI numerous widgets bound to number items will issue QuantityType commands to these items. 

##### Scripts & Rules
The classic rule engine in Eclipse SmartHome also has full support of the new QuantityType. It can be used in arithmetic calculations and comparison with automatic unit conversion.
The `Weather_Temperature` item from the example above with the fixed unit kelvin can be compared against other QuantityType instances with other units from the Temperature dimension.

The scripts

```
20.0|"°C" > 20|"°F"
```

and 

```
20.0|°C > 20|°F
```

both evaluate to `true` and

```
postUpdate(myTemperature, 20.0|°C)
```

will create a new QuantityType and update the Number item `myTemperature` to 20°C.

Note that a QuantityType in scripts and rules must be written as `<value>|"<unit>"`. Some frequently used units and those which are valid identifiers can also ommit the quotation marks and can conveniently be written as `<value>|<unit>` (e.g. `20|°C`)

##### Coding the UoM
###### Packages
When using units, metric prefixes (like MILLI, CENTI, HECTO, ...) and dimensions please make sure to use the following package imports:
For units and metric prefixes use `org.eclipse.smarthome.core.library.unit` package, for dimensions use the `org.eclipse.smarthome.core.library.dimension` or the official `javax.measure.quantity` package.
###### Classes
Prominent classes are `SIUnits` for units unique to the SI standard measurement system, `ImperialUnits` for units unique to the imperial system and `SmartHomeUnits` which are used in both systems.
The `MetricPrefix` class provides wrapper methods to generate prefixed units.
All interfaces from the dimension packages mentioned above should be used to type the generic `QuantityType` and respective units. 

##### Extend UoM
At the time of releasing the UoM support there are only physical units and the two measurement systems SI and Imperial available. While these will already cover the majority of use cases there might be the need for solutions to extend the available units and measurement systems. The start point for extension is to provide an individual implementation of the `org.eclipse.smarthome.core.i18n.UnitProvider`. This way also device dependent units and conversion of those to physical units may be implemented. 
