---
layout: documentation
---

{% include base.html %}

# Event Type Definition

Eclipse SmartHome provides the possibility to easily implement new event types and event factories. 

## Define new Event Type

Events can be added by implementing the `Event` interface or extending the `AbstractEvent` class which offers a default implementation. Both classes are located in the Eclipse SmartHome core bundle. 

The following Java snippet shows a new event type extending the class `AbstractEvent`.

```java
public class SunriseEvent extends AbstractEvent {

    public static final String TYPE = SunriseEvent.class.getSimpleName();
    
    private final SunriseDTO sunriseDTO;

    SunriseEvent(String topic, String payload, SunriseDTO sunriseDTO) {
        super(topic, payload, null);
        this.sunriseDTO = sunriseDTO;
    }

    @Override
    public String getType() {
        return TYPE;
    }
    
    public SunriseDTO getSunriseDTO() {
        return sunriseDTO;
    }

    @Override
    public String toString() {
        return "Sunrise at '" + getSunriseDTO.getTime() + "'.";
    }
}
```

The listing below summarizes some coding guidelines as illustrated in the example above:

- Events should only be created by event factories. Constructors do not have any access specifier in order to make the class package private.
- The serialization of the payload into a data transfer object (e.g. `SunriseDTO`) should be part of the event factory and will be assigned to a class member via the constructor. 
- A public member `TYPE` represents the event type as string representation and is usually the name of the class.
- The `toString()` method should deliver a meaningful string since it is used for event logging.
- The source of an event can be `null` if not required.

For more information about implementing an event, please refer to the Java documentation.

## Define new Event Factory

Event factories can be added by implementing the `EventFactory` interface or extending the `AbstractEventFactory` class. The `AbstractEventFactory` provides some useful utility for parameter validation and payload serialization & deserialization with JSON. The classes are located in the Eclipse SmartHome core bundle.

```java 
public class SunEventFactory extends AbstractEventFactory {

    private static final String SUNRISE_EVENT_TOPIC = "smarthome/sun/{time}/sunrise";

    public SunEventFactory() {
        super(Sets.newHashSet(SunriseEvent.TYPE);
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, String source) throws Exception {
        if (SunriseEvent.TYPE.equals(eventType)) {
            return createSunriseEvent(topic, payload);
        } 
        return null;
    }
    
    private Event createSunriseEvent(String topic, String payload) {
        SunriseDTO sunriseDTO = deserializePayload(payload, SunriseDTO.class);
        return new SunriseEvent(topic, payload, sunriseDTO);
    }
    
    public static SunriseEvent createSunriseEvent(Sunrise sunrise) {
        String topic = buildTopic(SUNRISE_EVENT_TOPIC, sunrise.getTime());
        SunriseDTO sunriseDTO = map(sunrise);
        String payload = serializePayload(sunriseDTO);
        return new SunriseEvent(topic, payload, sunriseDTO);
    }
}
```
The listing below summarizes some guidelines as illustrated in the example above:

- Provide the supported event types (`SunriseEvent.TYPE`) via an `AbstractEventFactory` constructor call. The supported event types will be returned by the `AbstractEventFactory.getSupportedEventTypes()` method.
- The event factory defines the topic (`SUNRISE_EVENT_TOPIC`) of the supported event types. Please ensure that the topic format follows the topic structure of the Eclipse SmartHome core events, similar to a REST URI (`{namespace}/{entityType}/{entity}/{sub-entity-1}/.../{sub-entity-n}/{action}`). The namespace must be `smarthome`.
- Implement the method `createEventByType(String eventType, String topic, String payload, String source)` to create a new event based on the topic and the payload, determined by the event type. This method will be called by the framework in order to dispatch received events to the corresponding event subscribers. If the payload is serialized with JSON, the method `deserializePayload(String payload, Class<T> classOfPayload)` can be used to deserialize the payload into a data transfer object.
- Provide a static method to create event instances based on a domain object (Item, Thing, or in the example above `Sunrise`). This method can be used by components in order to create events based on domain objects which should be sent by the EventPublisher. If the data transfer object should be serialized into a JSON payload, the method `serializePayload(Object payloadObject)` can be used.

For more information about implementing an event factory, please refer to the Java documentation.
