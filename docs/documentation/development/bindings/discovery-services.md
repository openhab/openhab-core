---
layout: documentation
---

{% include base.html %}

# Implementing a Discovery Service

Bindings can implement the `DiscoveryService` interface and register it as an OSGi service to inform the framework about devices and services, that can be added as things to the system (see also [Inbox & Discovery Concept](../../concepts/discovery.html)).

A discovery service provides discovery results.
The following table gives an overview about the main parts of a `DiscoveryResult`: 

| Field | Description |
|-------|-------------|
| `thingUID` | The `thingUID` is the unique identifier of the specific discovered thing (e.g. a device's serial number). It  *must not* be constructed out of properties, that can change (e.g. IP addresses). A typical `thingUID` could look like this: `hue:bridge:001788141f1a` 
| `thingTypeUID` | Contrary to the `thingUID` is the `thingTypeUID` that specifies the type the discovered thing belongs to. It could be constructed from e.g. a product number. A typical `thingTypeUID` could be the following: `hue:bridge`. 
| `bridgeUID` | If the discovered thing belongs to a bridge, the `bridgeUID` contains the UID of that bridge. 
| `properties` | The `properties` of a `DiscoveryResult` contain the configuration for the newly created thing. 
| `label` | The human readable representation of the discovery result. Do not put IP/MAC addresses or similar into the label but use the special `representationProperty` instead. |
| `representationProperty` | The name of one of the properties which discriminates the discovery result best against other results of the same type. Typically this is a serial number, IP or MAC address. The representationProperty often matches a configuration parameter and is also explicitly given in the thing-type definition. |

To simplify the implementation of own discovery services, an abstract base class `AbstractDiscoveryService` implements the `DiscoveryService`, that must only be extended.
Subclasses of `AbstractDiscoveryService` do not need to handle the `DiscoveryListeners` themselves, they can use the methods `thingDiscovered` and `thingRemoved` to notify the registered listeners.
Most of the descriptions in this chapter refer to the `AbstractDiscoveryService`.

For UPnP and mDNS there already are generic discovery services available.
Bindings only need to implement a `UpnpDiscoveryParticipant` resp. `mDNSDiscoveryParticipant`.
For details refer to the chapters [UPnP Discovery](#upnp-discovery) and [mDNS Discovery](#mdns-discovery). 

The following example is taken from the `HueLightDiscoveryService`, it calls `thingDiscovered` for each found light.
It uses the `DiscoveryResultBuilder` to create the discovery result. 

```java
    private void onLightAddedInternal(FullLight light) {
        ThingUID thingUID = getThingUID(light);
        if (thingUID != null) {
            ThingUID bridgeUID = hueBridgeHandler.getThing().getUID();
            Map<String, Object> properties = new HashMap<>(1);
            properties.put(LIGHT_ID, light.getId());
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withBridge(bridgeUID).withLabel(light.getName()).build();
            thingDiscovered(discoveryResult);
        } else {
            logger.debug("discovered unsupported light of type '{}' with id {}", light.getModelID(), light.getId());
        }
    }
```

The discovery service needs to provide the list of supported thing types, that can be found by the discovery service.
This list will be given to the constructor of `AbstractDiscoveryService` and can be requested by using `DiscoveryService#getSupportedThingTypes` method. 

## Registering as an OSGi service

The `Discovery` class of a binding which implements `AbstractDiscoveryService` should be annotated with

```java
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.<bindingID>")
```

where `<bindingID>` is the id of the binding, i.e. `astro` for the Astro binding.
Such a registered service will be picked up automatically by the framework.

## Discovery 

### Background Discovery 

If the implemented discovery service enables background discovery, the `AbstractDiscoveryService` class automatically starts it.
If background discovery is enabled, the framework calls `AbstractDiscoveryService#startBackgroundDiscovery` when the binding is activated and `AbstractDiscoveryService#stopBackgroundDiscovery` when the component is deactivated.
The default implementations of both methods are empty and could be overridden by the binding developer.
Depending on the concrete implementation the discovery service might start and stop a scheduler in these method or register a listener for an external protocol.
The `thingDiscovered` method can be used to notify about a newly discovered thing.

The following example shows the implementation of the above mentioned methods in the Wemo binding. 

```java
    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("Start WeMo device background discovery");
        if (wemoDiscoveryJob == null || wemoDiscoveryJob.isCancelled()) {
            wemoDiscoveryJob = scheduler.scheduleWithFixedDelay(wemoDiscoveryRunnable, 0, refreshInterval, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("Stop WeMo device background discovery");
        if (wemoDiscoveryJob != null && !wemoDiscoveryJob.isCancelled()) {
            wemoDiscoveryJob.cancel(true);
            wemoDiscoveryJob = null;
        }
    }
```

### Active Scan

If the user triggers an active scan for a binding or specific set of thing types, the method `startScan` of each discovery service which supports these thing type is called.
Within these methods the things can be discovered.
The abstract base class automatically starts a thread, so the implementation of this method can be long-running.

The following example implementation for `startScan` is taken from the `HueLightDiscoveryService`, that triggers a scan for known and also for new lights of the hue bridge.
Already discovered things are identified by the ThingUID the DiscoveryResult was created with, and won't appear in the inbox again.

```java
    @Override
    public void startScan() {
        List<FullLight> lights = hueBridgeHandler.getFullLights();
        if (lights != null) {
            for (FullLight l : lights) {
                onLightAddedInternal(l);
            }
        }
        // search for unpaired lights
        hueBridgeHandler.startSearch();
    }
```

### Re-Discovered Results and Things

The `getThingUID` method of the discovery service should create a consistent UID every time the same thing gets discovered.
This way existing discovery results and existing things with this UID will be updated with the properties from the current scan.
With this, dynamic discoveries (like UPnP or mDNS) can re-discover existing things and update communication properties like host names or TCP ports.

### Remove older results

Normally, older discovery results already in the inbox are left untouched by a newly triggered scan.
If this behavior is not appropriate for the implemented discovery service, one can override the method `stopScan` to call `removeOlderResults` as shown in the following example from the Hue binding: 

```java
    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }
```

## UPnP Discovery

UPnP discovery is implemented in the framework as `UpnpDiscoveryService`.
It is widely used in bindings. To facilitate the development, binding developers only need to implement a `UpnpDiscoveryParticipant`.
Here the developer only needs to implement three simple methods: 

- `getSupportedThingTypeUIDs` - Returns the list of thing type UIDs that this participant supports.
The discovery service uses this method of all registered discovery participants to return the list of currently supported thing type UIDs. 
- `getThingUID` - Creates a thing UID out of the UPnP result or returns `null` if this is not possible.
This method is called from the discovery service during result creation to provide a unique thing UID for the result. 
- `createResult` - Creates the `DiscoveryResult` out of the UPnP result.
This method is called from the discovery service to create the actual discovery result.
It uses the `getThingUID` method to create the thing UID of the result. 

The following example shows the implementation of the UPnP discovery participant for the Hue binding, the `HueBridgeDiscoveryParticipant`. 

```java
public class HueBridgeDiscoveryParticipant implements UpnpDiscoveryParticipant {

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return Collections.singleton(THING_TYPE_BRIDGE);
    }

    @Override
    public DiscoveryResult createResult(RemoteDevice device) {
        ThingUID uid = getThingUID(device);
        if (uid != null) {
            Map<String, Object> properties = new HashMap<>(2);
            properties.put(HOST, device.getDetails().getBaseURL().getHost());
            properties.put(SERIAL_NUMBER, device.getDetails().getSerialNumber());

            DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
                    .withLabel(device.getDetails().getFriendlyName()).withRepresentationProperty(SERIAL_NUMBER).build();
            return result;
        } else {
            return null;
        }
    }

    @Override
    public ThingUID getThingUID(RemoteDevice device) {
        DeviceDetails details = device.getDetails();
        if (details != null) {
            ModelDetails modelDetails = details.getModelDetails();
            if (modelDetails != null) {
                String modelName = modelDetails.getModelName();
                if (modelName != null) {
                    if (modelName.startsWith("Philips hue bridge")) {
                        return new ThingUID(THING_TYPE_BRIDGE, details.getSerialNumber());
                    }
                }
            }
        }
        return null;
    }
}
```

## mDNS Discovery

mDNS discovery is implemented in the framework as `MDNSDiscoveryService`.
To facilitate the development, binding developers only need to implement a `MDNSDiscoveryParticipant`.
Here the developer only needs to implement four simple methods: 

- `getServiceType` - Defines the [mDNS service type](http://www.dns-sd.org/ServiceTypes.html).
- `getSupportedThingTypeUIDs` - Returns the list of thing type UIDs that this participant supports.
The discovery service uses this method of all registered discovery participants to return the list of currently supported thing type UIDs. 
- `getThingUID` - Creates a thing UID out of the mDNS service info or returns `null` if this is not possible.
This method is called from the discovery service during result creation to provide a unique thing UID for the result. 
- `createResult` - Creates the `DiscoveryResult` out of the UPnP result.
This method is called from the discovery service to create the actual discovery result.
It uses the `getThingUID` method to create the thing UID of the result. 
