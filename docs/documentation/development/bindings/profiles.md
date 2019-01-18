---
layout: documentation
---

{% include base.html %}

# Profiles

This section explains how custom `Profile`s can be created.
For a general explanation of Profiles, please see the [concept section](../../concepts/profiles.html).

## Profile Properties

A `Profile` is determined by its type, i.e. `StateProfileType` or `TriggerProfileType` and its `ProfileTypeUID`.
Both types are specified via interfaces `StateProfile` and `TriggerProfile`, respectively.

The `ProfileTypeUID` identifies one specific type of `Profile`.
Each `Profile` exists in a certain scope.
There are pre-defined `Profile`s that are defined in scope `ProfileTypeUID.SYSTEM_SCOPE`, which is "system".
Custom `Profiles` should be created in a different scope.
Thus a `ProfileTypeUID` can be created as follows: `new ProfileTypeUID("MyScope", "MyProfileName")`.

A `StateProfile` receives `Commands` and `StateUpdates` from the `ThingHandler` and from the `Item`.
It has to implement four methods that specify how the `Command`s or `StateUpdate`s should be handled.

A `TriggerProfile` makes it possible to link a `TriggerChannel` to an `Item`.
This `Profile` receives the `State` of the `Item` and the `Event` that has been triggered.

## ProfileTypeProvider

Custom `ProfileType`s have to be annouced by a `ProfileTypeProvider` to the framework via an OSGi service:

```java
@Component(service = { ProfileTypeProvider.class })
public class MyProfileTypeProvider implements ProfileTypeProvider {
    @Override
    public Collection<ProfileType> getProfileTypes(Locale locale) {
        //return custom types
    }
}
```

## ProfileFactory

The most important interface is the `ProfileFactory` which has to be implemented and announced as an OSGi service:

```java
@Component(service = { ProfileFactory.class })
public class MyProfileFactory implements ProfileFactory {
```

Such a factory is responsible for specific `ProfileTypeUID`s that should be returned by `Collection<ProfileTypeUID> getSupportedProfileTypeUIDs()`.
Further it is capable of creating these `Profile`s via `createProfile(ProfileTypeUID profileTypeUID, ProfileCallback callback, ProfileContext profileContext);`.

For convenience, the `ProfileFactory` and the `ProfileTypeProvider` can be put into one class and announce the two services:

```java
@Component(service = { ProfileFactory.class, ProfileTypeProvider.class })
public class MyProfileFactory implements ProfileFactory, ProfileTypeProvider {
```

### ProfileCallback

`Profile`s need the opportunity to notify the framework about what they did with the `Command`s and `StateUpdate`s it received from the framework.
The `ProfileCallback` provides methods to forward the results of a `Profile`s processing to the `ThingHandler` and to the `Framework`.
It should be injected into the `Profile` upon its creation.

### ProfileContext

Some more advanced `Profile`s which can be configured need access to their `Configuration` object.
This is offered via the `ProfileContext`.
A `ScheduledExecutorService` can also be retrieved via the `ProfileContext` in order to schedule long running tasks in a separate Thread.
The `ProfileContext` may also be injected into the `Profile` upon its creation.

## ProfileAdvisor

A `ProfileAdvisor` is an optional component which can be used to suggest a specific `ProfileTypeUID` for a given `Channel` or `ChannelType`.
Two methods have to be implemented to achieve this:

`ProfileTypeUID getSuggestedProfileTypeUID(Channel channel, @Nullable String itemType);`

`ProfileTypeUID getSuggestedProfileTypeUID(ChannelType channelType, @Nullable String itemType);`

## Using Profiles

### .items file

`Profiles`s can be specified as a parameter for a given channel:

```java
<item-type> MyItem { channel="<bindingID>:<thing-typeID>:MyThing:myChannel"[profile="MyScope:MyProfile"]}
```

## Existing Profiles

### FollowProfile

If one device should "follow" the actions of another device, the FollowProfile can be used. The term "follow" in this case means that any state that is sent to an `Item` will be forwarded from this `Item` to any linked channel with the FollowProfile. The FollowProfile takes state updates on an `Item` and sends them as a command onto the channel. In the direction from the ThingHandler towards the `Item`, the FollowProfile ignores state updates.

```java
<itemType> <itemName> { channel="<channelUID>", channel="<followChannelUID>"[profile="follow"]}
```

### OffsetProfile

The `OffsetProfile` provides the possibility to adjust a value from a device before it arrives at the framework.
An offset can be specified via the parameter `offset` which has to be a `QuantityType` or `DecimalType`.
A positive offset is the amount of change from the device towards the framework, i.e. all values from the device are increased by this offset and values sent to the device are decreased by this offset.
A negative offset subtracts the offset from the value sent by the device to the framework and adds the offset to values sent from the framework to the device.

```java
Number <itemName> { channel="<bindingID>:<thing-typeID>:<thingName>:<channelName>"[profile="offset", offset="<value>"]}
```
