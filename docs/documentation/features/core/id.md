---
layout: documentation
---

{% include base.html %}

# Unique Instance IDs

Bundle: `org.eclipse.smarthome.core.id`

## Description

When communicating with external systems, it is often desirable to have a unique identifier for the instance of Eclipse SmartHome. This optional bundle is a mean to generate such an id, which is automatically persisted. The persistence is done in the configured `userdata` directory as a file called `uuid`. 

The id is provided through a static method and can be retrieved through
```
    String uuid = InstanceUUID.get();
```

If the [REST API](../rest.html) is installed as well, this bundle will additionally register a rest endpoint `uuid` (e.g. `http://localhost:8080/rest/uuid`), which returns the id as a plain string on a GET access.
