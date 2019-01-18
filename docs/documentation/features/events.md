---
layout: documentation
---

{% include base.html %}

# Events

The Eclipse SmartHome framework provides an event bus for inter-component communication. The communication is based on events which can be sent and received through the event bus in an asynchronous way. Examples of Eclipse SmartHome event types are _ItemCommandEvent_, _ItemStateEvent_, _ItemAddedEvent_, _ThingStatusInfoEvent_, etc.

This section gives a short overview about the event API and illustrates how to receive such events. Furthermore, the sending of events and the implementation of new event types will be described.

A code snippet about receiving events can be found in chapter "Receive Events". In particular, receiving _ItemStateEvents_ and _ItemCommandEvents_ is described in chapter "Receive ItemStateEvents and ItemCommandEvents".

## API Introduction

### The Interfaces

![Event Interfaces](diagrams/event_interfaces.png)

The `EventPublisher` posts `Event`s through the Eclipse SmartHome event bus in an asynchronous way. The `EventSubscriber` defines the callback interface to receive  events of specific types to which the event subscriber is subscribed. The EventPublisher and the EventSubscribers are registered as OSGi services. An event subscriber can provide an `EventFilter` in order to filter events based on the topic or the content. If there is no filter all subscribed event types are received. The event itself will be subclassed for each event type, which exists in the System (e.g. ItemCommandEvent, ItemUpdatedEvent, ThingStatusInfoEvent).

### The Core Events
This section lists the core events provided by Eclipse SmartHome which can be divided into the categories _Item Events_, _Thing Events_ and _Inbox Events_.

An event consists of a `topic`, a `type`, a `payload` and a `source`. The payload can be serialized with any String representation and is determined by its concrete event type implementation (e.g. ItemCommandEvent, ItemUpdatedEvent). The payloads of the Eclipse SmartHome core events are serialized with JSON. Each event implementation provides the payload as high level methods as well, usually presented by a data transfer object (DTO).

A topic clearly defines the target of the event and its structure is similar to a REST URI, except the last part, the action. The topics of Eclipse SmartHome events are divided into the following four parts: `{namespace}/{entityType}/{entity}/{action}`, e.g. `smarthome/items/{itemName}/command`.

The type of an event is represented by a string, usually the name of the concrete event implementation class, e.g. ItemCommandEvent, ItemUpdatedEvent. This string type presentation is used by event subscribers for event subscription (see chapter "Receive Events") and by the framework for the creation of concrete event instances.

The event source is optional and represents the name of the source identifying the sender.

#### Item Events

| Event                 |Description                                      |Topic                                   |
|-----------------------|-------------------------------------------------|----------------------------------------|
| ItemAddedEvent        |An item has been added to the item registry.     |smarthome/items/{itemName}/added        |
| ItemRemovedEvent      |An item has been removed from the item registry. |smarthome/items/{itemName}/removed      |
| ItemUpdatedEvent      |An item has been updated in the item registry.   |smarthome/items/{itemName}/updated      |
| ItemCommandEvent      |A command is sent to an item via a channel.      |smarthome/items/{itemName}/command      |
| ItemStateEvent        |The state of an item is updated.                 |smarthome/items/{itemName}/state        |
| ItemStateChangedEvent |The state of an item has changed.                |smarthome/items/{itemName}/statechanged |

**Note:** The ItemStateEvent is always sent if the state of an item is updated, even if the state did not change. ItemStateChangedEvent is sent only if the state of an item was really changed. It contains the old and the new state of the item.

#### Thing Events

| Event                 |Description                                       |Topic                                   |
|-----------------------|-------------------------------------------------|-----------------------------------|
| ThingAddedEvent         |A thing has been added to the thing registry.    |smarthome/things/{thingUID}/added  |
| ThingRemovedEvent      |A thing has been removed from the thing registry.|smarthome/things/{thingUID}/removed|
| ThingUpdatedEvent     |A thing has been updated in the thing registry.  |smarthome/things/{thingUID}/updated|
| ThingStatusInfoEvent    |The status of a thing is updated.                  |smarthome/things/{thingUID}/status |
| ThingStatusInfoChangedEvent    |The status of a thing changed.                  |smarthome/things/{thingUID}/statuschanged |

**Note:** The ThingStatusInfoEvent is always sent if the status info of a thing is updated, even if the status did not change. ThingStatusInfoChangedEvent is sent only if the status of a thing was really changed. It contains the old and the new status of the thing.

#### Inbox Events

| Event                 |Description                                         |Topic                                   |
|-----------------------|---------------------------------------------------|-----------------------------------|
| InboxAddedEvent         |A discovery result has been added to the inbox.     |smarthome/inbox/{thingUID}/added   |
| InboxRemovedEvent     |A discovery result has been removed from the inbox. |smarthome/inbox/{thingUID}/removed |
| InboxUpdateEvent         |A discovery result has been updated in the inbox.   |smarthome/inbox/{thingUID}/updated |

#### Link Events

| Event                       |Description                                              |Topic                                           |
|-----------------------------|---------------------------------------------------------|------------------------------------------------|
| ItemChannelLinkAddedEvent   |An item channel link has been added to the registry.     |smarthome/links/{itemName}-{channelUID}/added   |
| ItemChannelLinkRemovedEvent |An item channel link has been removed from the registry. |smarthome/links/{itemName}-{channelUID}/removed |

#### Channel Events

| Event                       |Description                                              |Topic                                           |
|-----------------------------|---------------------------------------------------------|------------------------------------------------|
| ChannelTriggeredEvent       |A channel has been triggered.                            |smarthome/channels/{channelUID}/triggered       |

## Receive Events

This section describes how to receive Eclipse SmartHome events in Java. If you want to receive events "outside" Eclipse SmartHome, e.g. with JavaScript, please refer to the [Server Sent Events section](../features/rest.html).

An event subscriber defines the callback interface for receiving events from the Eclipse SmartHome event bus. The following Java snippet shows how to receive `ItemStateEvent`s and `ItemCommandEvent`s from the event bus. Therefore, the `EventSubscriber` interface must be implemented.

```java
public class SomeItemEventSubscriber implements EventSubscriber {
    private final Set<String> subscribedEventTypes = ImmutableSet.of(ItemStateEvent.TYPE, ItemCommandEvent.TYPE);
    private final EventFilter eventFiter = new TopicEventFilter("smarthome/items/ItemX/.*");

    @Override
    public Set<String> getSubscribedEventTypes() {
        return subscribedEventTypes;
    }

    @Override
    public EventFilter getEventFilter() {
        return eventFilter;
    }

    @Override
    public void receive(Event event) {
        String topic = event.getTopic();
        String type = event.getType();
        String payload = event.getPayload();
        if (event instanceof ItemCommandEvent) {
            ItemCommandEvent itemCommandEvent = (ItemCommandEvent) event;
            String itemName = itemCommandEvent.getItemName();
            Command command = itemCommandEvent.getItemCommand();
            // ...
        } else if (event instanceof ItemStateEvent) {
            ItemStateEvent itemStateEvent = (ItemStateEvent) event;
            // ...
        }
    }
}
```
The `SomeItemEventSubscriber` is subscribed to the event types `ItemStateEvent` and `ItemCommandEvent`, provided by the method `getSubscribedEventTypes()`. A string representation of an event type can be found by a public member `TYPE` which usually presents the name of the class. To subscribe to all available event types, use the public member `ALL_EVENT_TYPES` of the event subscriber interface.

The event subscriber provides a `TopicEventFilter` which is a default Eclipse SmartHome `EventFilter` implementation that ensures filtering of events based on a topic. The argument of the filter is a [Java regular expression](http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html). The filter method `EventFilter.apply()` will be called for each event on the event bus to which the event subscriber is subscribed (in the example above ItemStateEvent and ItemCommandEvent). If the filter applies (in the given example for all item events with the item name "ItemX"), the event will be received by the `EventSubscriber.receive()` method. Received events can be cast to the event implementation class for further processing.

Each event subscriber must be registered via OSGi Declarative Services (DS) under the  `org.eclipse.smarthome.event.EventSubscriber` interface.

```xml
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0" immediate="true" name="SomeItemEventSubscriber">
   <implementation class="org.eclipse.smarthome.core.items.events.SomeItemEventSubscriber"/>
   <service>
      <provide interface="org.eclipse.smarthome.core.events.EventSubscriber"/>
   </service>
</scr:component>
```  

The listing below summarizes some best practices in order to implement event subscribers:

- To subscribe to only one event type Eclipse SmartHome provides the `org.eclipse.smarthome.core.events.AbstractTypedEventSubscriber` implementation. To receive an already cast event the `receiveTypedEvent(T)` method must be implemented. To provide an event filter the method `getEventFilter()` can be overridden.
- Eclipse SmartHome provides an `AbstractItemEventSubscriber` class in order to receive ItemStateEvents and ItemCommandEvents (more information can be obtained in the next chapter).
- To filter events based on a topic the  `org.eclipse.smarthome.core.events.TopicEventFilter` implementation from the Eclipse SmartHome core bundle can be used. The filtering is based on [Java regular expression](http://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html).
- The subscribed event types and the filter should be stored as class members (see example above) due to performance reasons.
- If the subscribed event types are sufficient in order to receive all interested events, do not return any filter (in that case the method getFilter() returns null) due to performance reasons.
- Avoid the creation of too many event subscribers. Similar event types can be received in one event subscriber.
- Handle exceptions in event subscriber implementation and throw only serious exceptions. Thrown exceptions will be handled in the framework by logging an error message with the cause.
- The receive method should terminate quickly, since it blocks other event subscribers. Create a thread for long running operations.


### Receive ItemStateEvents and ItemCommandEvents
Due to the fact that receiving ItemStateEvents and ItemCommandEvents is a common use case, Eclipse SmartHome provides an abstract event subscriber implementation via the core bundle. The class `org.eclipse.smarthome.core.items.events.AbstractItemEventSubscriber` provides two methods `receiveUpdate(ItemStateEvent)` and `receiveCommand(ItemCommandEvent)` which can be implemented in order to receive and handle such events.

```java
public class SomeItemEventSubscriber extends AbstractItemEventSubscriber {
    private final EventFilter eventFiter = new TopicEventFilter("smarthome/items/ItemX/.*");

    @Override
    public EventFilter getEventFilter() {
        return eventFilter;
    }

    @Override    
    protected void receiveCommand(ItemCommandEvent commandEvent) {
        // do something
    }

    @Override
    protected void receiveUpdate(ItemStateEvent stateEvent) {
        // do something
    }
}
```

## Send Events

Usually the core events are only sent by the Eclipse SmartHome framework. However, it is possible to sent events explicitly, e.g. ItemCommandEvents and ItemStateEvents. The Java snippet below illustrates how to send events via the EventPublisher. The Eclipse SmartHome core events can only be created via the corresponding event factory.

```java
public class SomeComponentWantsToPost {
    private EventPublisher eventPublisher;

    public void postSomething() {
        ItemCommandEvent itemCommandEvent = ItemEventFactory.createCommandEvent("ItemX", OnOffType.ON);
        eventPublisher.post(itemCommandEvent);
    }

    protected void setEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = null;
    }
}
```

The EventPublisher will be injected via OSGi Declarative Services.

```xml
<scr:component xmlns:scr="http://www.osgi.org/xmlns/scr/v1.1.0" immediate="true" name="SomeComponentWantsToPost">
    <!-- ... -->
   <reference bind="setEventPublisher" cardinality="1..1" interface="org.eclipse.smarthome.core.events.EventPublisher"
           name="EventPublisher" policy="static" unbind="unsetEventPublisher"/>
</scr:component>
```

## Define new Event Types

It is possible to create and provide new event types. For a detailed description please refer to the [Event Type Definition section](./event-type-definition.html).
