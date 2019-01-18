---
layout: documentation
---

# Framework Utilities

In this chapter useful services/utilities of the Eclipse SmartHome project are described. 

## Network Address Service

The `NetworkAddressService` is an OSGi service that can be used like any other OSGi service by adding a service reference to it. Its OSGi service name is `org.eclipse.smarthome.network`.
A user can configure his default network address via Paper UI under `Configuration -> System -> Network Settings`.
One can obtain the configured address via the `getPrimaryIpv4HostAddress()` method on the service.
This service is useful for example in the `ThingHandlerFactory` or an `AudioSink` where one needs a specific IP address of the host system to provide something like a `callback` URL.

Some static methods like `getAllBroadcastAddresses()` for retrieving all interface broadcast addresses or `getInterfaceAddresses()` for retrieving all assigned interface addresses might be usefull as well for discovery services.

### Network Address Change Listener

The `NetworkAddressChangeListener` is a consumer type OSGi service interface. If listeners want to be notified about network interface address changes, they can implement `NetworkAddressChangeListener` and register as an OSGi service.

Please be aware that not all network interface changes are notified to the listeners, only "useful" network interfaces :--
When a network interface status changes from "up" to "down", it is considered as "removed".
When a "loopback" or "down" interface is added, the listeners are not notified.

## Caching

The framework provides some caching solutions for common scenarios.

### Simple expiring and reloading cache

A common usage case is in a `ThingHandler` to encapsulate one value of an internal state and attach an expire time on that value. A cache action will be called to refresh the value if it is expired. This is what `ExpiringCache` implements. If `handleCommand(ChannelUID channelUID, Command command)` is called with the "RefreshType" command, you just return `cache.getValue()`. 

It is a good practice to return as fast as possible from the `handleCommand(ChannelUID channelUID, Command command)` method to not block callers especially UIs.
Use this type of cache only, if your refresh action is a quick to compute, blocking operation. If you deal with network calls, consider the asynchronously reloading cache implementation instead.

### Expiring and asynchronously reloading cache

If we refreshed a value of the internal state in a `ThingHandler` just recently, we can return it immediately via the usual `updateState(channel, state)` method in response to a "RefreshType" command.
If the state is too old, we need to fetch it first and this may involve network calls, interprocess operations or anything else that will would block for a considerable amout of time.

A common usage case of the `ExpiringCacheAsync` cache type is in a `ThingHandler` to encapsulate one value of an internal state and attach an expire time on that value.


A **handleCommand** implementation with the interesting *RefreshType* could look like this:
```
public void handleCommand(ChannelUID channelUID, Command command) {
    if (command instanceof RefreshType) {
        switch (channelUID.getId()) {
            case CHANNEL_1:
                cache1.getValue(updater).thenAccept(value -> updateState(CHANNEL_1, value));
                break;
            ...
        }
    }
}
```

The interesting part is the `updater`. If the value is not yet expired, the returned CompletableFuture will complete immediately and the given code is executed.
If the value is expired, the updater will be used to request a refreshed value.

An updater can be any class or lambda that implements the funtional interface of `Supplier<CompletableFuture<VALUE_TYPE>>`.

In the following example the method `CompletableFuture<VALUE_TYPE> get()` is accordingly implemented. The example assumes that we deal
with a still very common callback based device refreshing method `doSuperImportantAsyncStuffHereToGetRefreshedValue(listener)`. The listener is the class
itself, which implements `DeviceStateUpdateListener`. We will be called back with a refreshed device state in `asyncCallbackFromDeviceStateRefresh`
and mark the Future as *complete*.

```
class FetchValueFromDevice implements Supplier<CompletableFuture<double>>, DeviceStateUpdateListener {
    CompletableFuture<double> c;
    
    @Override
    CompletableFuture<double> get() {
       if (c != null) {
          c = new CompletableFuture<double>();
          doSuperImportantAsyncStuffHereToGetRefreshedValue( (DeviceStateUpdateListener)this );
       }
       return c;
    }
    
    // Here you process the callback from your device refresh method
    @Override
    void asyncCallbackFromDeviceStateRefresh(double newValue) {
       // Notify the future that we have something
       if (c != null) {
          c.complete(newValue);
          c = null;
       }
    }
}
```
If you deal with a newer implementation with a CompletableFuture support, it is even easier. You would just return your CompletableFuture.
