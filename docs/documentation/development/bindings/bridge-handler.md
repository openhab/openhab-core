---
layout: documentation
---

{% include base.html %}

# Bridge Handler Implementation

A `BridgeHandler` handles the communication between the Eclipse SmartHome framework and a *bridge*  (a device that acts as a gateway to enable the communication with other devices) represented by a `Bridge` instance.
 
A bridge handler has the same properties as thing handler. Therefore, the `BridgeHandler` interface extends the `ThingHandler` interface.
 
## The BaseBridgeHandler

Eclipse SmartHome provides an abstract implementation of the `BridgeHandler` interface named `BaseBridgeHandler`. It is recommended to use this class, because it covers a lot of common logic.


## Life cycle

A `BridgeHandler` has the same life cycle than a `ThingHandler` (created by a `ThingHandlerFactory`, well defined life cycle by handler methods `initialize()` and `dispose()`, see chapter [Life Cycle](thing-handler.html#life-cycle)). A bridge acts as a gateway in order to provide access to other devices, the *child things*. Hence, the life cycle of a child handler depends on the life cycle of a bridge handler. Bridge and child handlers are subject to the following restrictions: 

- A `BridgeHandler` of a bridge is initialized before `ThingHandler`s of its child things are initialized.
- A `BridgeHandler` is disposed after all `ThingHandler`s of its child things are disposed.     


## Handler initialization notification
A `BridgeHandler` is notified about the initialization and disposal of child things. Therefore, the `BridgeHandler` interface provides the two methods `childHandlerInitialized(ThingHandler, Thing)` and `childHandlerDisposed(ThingHandler, Thing)`.  

These methods can be used to allocate and deallocate resources for child things.
