# Magic Bundle

The Magic Bundle is a virtual device bundle which provides different Things, Channels and supporting functionality for easy UI testing.

Future plans:

* Simulate communication errors
* Provide REST API to update thing status from _outside_
* Provide REST API to temporarily create new Channels/Things

## Provided Things

* Magic Light - On/Off
* Magic Light - Dimmable
* Magic Light - Color
* Magic Sensor - Door/Window Contact
* Magic Location
* Magic Configurable Thing
* Magic Thermostat
* Magic Delayed Online Thing - goes online after some time
* Magic Firmware Updatable Thing - can be firmware updated, depending on the model


## Discovery

The Things provided by this bundle do not require discovery but can all be set up manually using PaperUI.

## Bundle Configuration

Right now Magic has no specific configuration. This may change when _Future plans_ are implemented.

## Thing Configuration

The provided Things need no parameters right now.

## Channels

Available channels:

* switch - the on/off toggle maps to a Switch item.
* brightness - the brightness value maps to a Dimmer item.
* color - the color maps to a Color item.
* alert - the alert function of the color light.
* contact - the contact of the door/window contact.
* location - the location of the magic location.
* temperature - the temperature of the magic thermostat.
* number - the delay in seconds for the delayed thing to go online.

## Full Example

*.things:

```
magic:onoff-light:mylight "Bright or Dark"
magic:dimmable-light:greys "Shades of light"
magic:color-light:rainbow "Rainbow"
```

*.items:

```
Switch Light1 "On/Off Light" { channel="magic:onoff-light:mylight:switch" }
Dimmer Light2 "Shades of light" { channel="magic:dimmable-light:greys:brightness" }
Color Rainbow "Rainbow" { channel="magic:color-light:rainbow:color" }
```

