---
layout: documentation
---

{% include base.html %}

# REST API

## Introduction

The REST API of Eclipse SmartHome serves different purposes. It can be used to integrate with other system as it allows read access to items and item states as well as status updates or the sending of commands for items. Furthermore, it gives access to things, links, sitemaps, etc., so that it is the main interface to be used by remote UIs (e.g. fat clients or HTML5 web applications).

The REST API is complemented by support for server sent events (SSE), so you can subscribe  on change notification for certain resources.

## Configuration Options

By default, the REST API is registered on the HTTP service under `rest`. This configuration can be changed through configuring the property `root` of the service pid `com.eclipsesource.jaxrs.connector` through the Configuration Admin service.

## REST Endpoints

The details about available REST urls, their parameters, etc. can be found [here](../../rest/index.html).

## Server Sent Events (SSE)

In order to receive notice of important events outside of the Eclipse SmartHome framework they are exposed using the Server Sent Events (SSE) standard.

To subscribe to events a developer can listen to `/rest/events`. In general any SSE Consumer API can be used (for example HTML5 EventSource Object) to read from the event stream.

### Events

The framework broadcasts all events on the Eclipse SmartHome event bus also as an SSE event. A complete list of the framework event types can be found in the [Event chapter](events.html).

All events are represented as JSON objects on the stream with the following format:

```json
{
    "topic": "smarthome/inbox/yahooweather:weather:12811438/added",
    "type": "InboxAddedEvent",
    "payload": "{
        "flag": "NEW",
        "label": "Yahoo weather Berlin, Germany",
        "properties": {
            "location": "12811438"
        },
        "thingUID": "yahooweather:weather:12811438"
    }"
}
```

* `topic`: the event topic (see also [Runtime Events](events.html))
* `type`: the event type (see also [Runtime Events](event-type-definition.html))
* `payload`: String, which contains the payload of the Eclipse SmartHome event. For all core events, the payload will be in the JSON format. For example the `smarthome/items/item123/added` event will include the new item that was added and the `smarthome/items/item123/updated` event will include both old and new item.
  
### Filtering

By default when listening to `/rest/events` a developer will receive all events that are currently broadcasted. In order to listen for specific events the `topics` query parameter can be used.

For example while listening to `/services/events?topics=smarthome/items/*` a developer would receive notifications about item events only. The wildcard character (\*) can be used replacing one (or multiple) parts of the topic.

The `topics` query parameter also allows for multiple filters to be specified using a comma (,) for a separator - `?topics=smarthome/items/*, smarthome/things/*`.

### Example

An example of listing events in JavaScript using the HTML5 EventSource object is provided below:

```js
//subscribe for all kind of 'added' and 'inbox' events
var eventSource = new EventSource("/rest/events?topics=smarthome/*/added,smarthome/inbox/*");

eventSource.addEventListener('message', function (eventPayload) {

    var event = JSON.parse(eventPayload.data);
    console.log(event.topic);
    console.log(event.type);
    console.log(event.payload);

    if (event.type === 'InboxAddedEvent') {
        var discoveryResult = JSON.parse(event.payload);
        console.log(discoveryResult.flag);
        console.log(discoveryResult.label);
        console.log(discoveryResult.thingUID);
    }
});
```
