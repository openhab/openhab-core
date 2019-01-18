---
layout: documentation
---

{% include base.html %}

# Declaring Configurations, Bindings and Things

Specific services and bindings have to provide meta information which is used for visualization, validation or internal service mapping. Meta information can be provided by registering specific services at the *OSGi* service registry or by specifying them in a declarative way, which is described in this chapter.

**Three kinds of descriptions/definitions exist:**

- Configuration descriptions: Used for visualization and validation of configuration properties (optional)
- Binding definitions: Required to declare a binding (mandatory)
- Bridge and *Thing* descriptions: Required to specify which bridges and *Thing*s are provided by the binding, which relations they have to each other and which channels they offer (mandatory)


## Configuration Descriptions

Specific services or bindings usually require a configuration to be operational in a meaningful way. To visualize or validate concrete configuration properties, configuration descriptions should be provided. All available configuration descriptions are accessible through the `org.eclipse.smarthome.config.core.ConfigDescriptionRegistry` service.

Although configuration descriptions are usually specified in a declarative way (as described in this section), they can also be provided as `org.eclipse.smarthome.config.core.ConfigDescriptionProvider`.
Any `ConfigDescriptionProvider`s must be registered as service at the *OSGi* service registry. The full Java API for configuration descriptions can be found in the Java package `org.eclipse.smarthome.config.core`. In addition to this there is a `org.eclipse.smarthome.config.core.validation.ConfigDescriptionValidator` that can be used to validate a set of configuration parameters against their declarations in the configuration description before the actual configuration is updated with the new configuration parameters.

Configuration descriptions must be placed as XML file(s) (with the ending `.xml`) in the bundle's folder `/ESH-INF/config/`.

### Formatting Labels
The label and descriptions for things, channels and config descriptions should follow the following format. The label should be short so that for most UIs it does not spread across multiple lines. The description can contain longer text to describe the thing in more detail. Limited use of HTML tags is permitted to enhance the description - if a long description is provided, the first line should be kept short and a line break (```<br>```) should be placed at the end of the line to allow UIs to display a short description in limited space.

Configuration options should be kept short so that they are displayable on a single line in most UIs. If you want to provide a longer description of the options provided by a particular parameter, then this should be placed into the ```<description>``` of the parameter to keep the option label short. The description can include limited HTML to enhance the display of this information.

The following HTML tags are allowed -: ```<b>, <br>, <em>, <h1>, <h2>, <h3>, <h4>, <h5>, <h6>, <i>, <p>, <small>, <strong>, <sub>, <sup>, <ul>, <ol>, <li>```. These must be inside the XML escape sequence - eg. ```<description><![CDATA[ HTML marked up text here ]]></description>```.

### XML Structure for Configuration Descriptions
```xml
<?xml version="1.0" encoding="UTF-8"?>
<config-description:config-descriptions
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:config-description="http://eclipse.org/smarthome/schemas/config-description/v1.0.0"
    xsi:schemaLocation="http://eclipse.org/smarthome/schemas/config-description/v1.0.0
        http://eclipse.org/smarthome/schemas/config-description-1.0.0.xsd">

  <config-description uri="{binding|thing-type|channel-type|any_other}:bindingID:...">
    <parameter-group name="String">
      <label>String</label>
      <description>String</description>
      <context>String</context>
      <advanced>{true|false}</advanced>
    </parameter-group>

    <parameter name="String" type="{text|integer|decimal|boolean}" min="Decimal" max="Decimal" step="Decimal" pattern="String" required="{true|false}" readOnly="{true|false}" multiple="{true|false}" groupName="String" unit="A|cd|K|kg|m|mol|s|g|rad|sr|Hz|N|Pa|J|W|C|V|F|Ω|S|Wb|T|H|Cel|lm|lx|Bq|Gy|Sv|kat|m/s2|m2v|m3|kph|%|l|ms|min|h|d|week|y">
      <context>{network-address|serial-port|password|password-create|color|date|datetime|email|month|week|dayOfWeek|time|tel|url|item|thing|group|tag|service|channel|rule|location}</context>
      <required>{true|false}</required>
      <default>String</default>
      <label>String</label>
      <description>String</description>
      <unitLabel>String</unitLabel>
      <options>
        <option value="String">String</option>
      </options>
      <filter>
        <criteria name="String">String</criteria>
      </filter>
    </parameter>
  </config-description>

  <config-description uri="{binding|thing-type|channel-type|any_other}:bindingID:...">
    ...
  </config-description>
...
</config-description:config-descriptions>
```

<table>
  <tr><td><b>Property</b></td><td><b>Description</b></td></tr>
  <tr><td>config-description.uri</td><td>The URI of this description within the ConfigDescriptionRegistry (mandatory).</td></tr>
  <tr><td>parameter</td><td>The description of a concrete configuration parameter (optional).</td></tr>
  <tr><td>parameter.name</td><td>The name of the configuration parameter (mandatory).</td></tr>
  <tr><td>parameter.type</td><td>The data type of the configuration parameter (mandatory).</td></tr>
  <tr><td>parameter.min</td><td>The minimal value for numeric types, or the minimal length of strings. Note that the value of any options may be outside of this value. (optional).</td></tr>
  <tr><td>parameter.max</td><td>The maximum value for numeric types, or the maximum length of strings. Note that the value of any options may be outside of this value. (optional).</td></tr>  
  <tr><td>parameter.step</td><td>The value granularity for a numeric value (optional).</td></tr>
  <tr><td>parameter.pattern</td><td>The regular expression for a text type (optional).</td></tr>
  <tr><td>parameter.required</td><td>Specifies whether the value is required (optional).</td></tr>
  <tr><td>parameter.readOnly</td><td>Specifies whether the value is read-only (optional).</td></tr>
  <tr><td>parameter.multiple</td><td>Specifies whether multiple selections of options are allowed (optional).</td></tr>
  <tr><td>parameter.groupName</td><td>Sets a group name for this parameter (optional).</td></tr>
  <tr><td>parameter.unit</td><td>Specifies the unit of measurements. The unit declaration in the parameter definition shown above contains the set of valid units. The unit must only be set if the type of the parameter is either integer or decimal (optional).</td></tr>
  <tr><td>advanced</td><td>Specifies that this is an advanced parameter. Advanced parameters may be hidden by a UI (optional).</td></tr>
  <tr><td>verify</td><td>Specifies that this is parameter requires a verification stage with the user before sending. Parameters flagged with *verify=true* could be considered dangerous and should be protected from accidental use by a UI - eg by adding an "Are you sure" prompt (optional).</td></tr>
  <tr><td>context</td><td>The context of the configuration parameter (optional).</td></tr>
  <tr><td>required</td><td>The flag indicating if the configuration parameter has to be set or not (deprecated, optional, default: false).</td></tr>
  <tr><td>default</td><td>The default value of the configuration parameter (optional).</td></tr>
  <tr><td>label</td><td>A human-readable label for the configuration parameter (optional).</td></tr>
  <tr><td>description</td><td>A human-readable description for the configuration parameter (optional).</td></tr>
  <tr><td>unitLabel</td><td>The unit label represents a human-readable label for the unit. It can also be used to provide unit labels for natural language units as iterations, runs, etc. The unit label must only be set if the type of the parameter is either integer or decimal (optional).</td></tr>
  <tr><td>option</td><td>The element definition of a static selection list (optional).</td></tr>
  <tr><td>option.value</td><td>The value of the selection list element. Note that the value may be outside of the range specified in the min/max if this is specified.</td></tr>
  <tr><td>multipleLimit</td><td>If multiple is true, sets the maximum number of options that can be selected (optional).</td></tr>
  <tr><td>limitToOptions</td><td>If true (default) will only allow the user to select items in the options list. If false, will allow the user to enter other text (optional).</td></tr>
  <tr><td>criteria</td><td>The filter criteria for values of a dynamic selection list (optional).</td></tr>  
  <tr><td>criteria.name</td><td>The name of the context related filter.</td></tr>  
</table>

### Supported Contexts

Context is used to provide some semantic details about the parameter. The UIs use it to render different kind of input widgets. The following contexts require a specific format of the content:

<table><tr><th>Name</th><th>Type</th><th>Format</th><th>Sample implementation</th></tr>
  <tr><td>network-address</td><td>text</td><td>IPv4,IPv6, domain name</td><td><code>&lt;input type="text"/></code></td></tr>
  <tr><td>serial-port</td><td>text</td><td>Serial port name, e.g. COM1</td><td>custom input field</td></tr>
  <tr><td>password</td><td>text</td><td>alphanumeric characters</td><td><code>&lt;input type="password"/></code></td></tr>
  <tr><td>password-create</td><td>text</td><td>alphanumeric characters</td><td>custom password input</td></tr>
  <tr><td>color</td><td>text</td><td>#000000 - #ffffff (hex color)</td><td><code>&lt;input type="color"/></code></td></tr>
  <tr><td>date</td><td>text</td><td>YYYY-MM-DD</td><td><code>&lt;input type="date"/></code></td></tr>
  <tr><td>datetime</td><td>text</td><td>YYYY-MM-DD hh:mm</td><td>custom input field</td></tr>
  <tr><td>email</td><td>text</td><td>username@domain.com</td><td><code>&lt;input type="email"/></code></td></tr>
  <tr><td>month</td><td>text</td><td>month of year</td><td>custom input field</td></tr>
  <tr><td>week</td><td>integer</td><td>week of year</td><td>custom input field</td></tr>
  <tr><td>dayOfWeek</td><td>text</td><td>MON, TUE, WED, THU, FRI, SAT, SUN <br></td><td>custom input field</td></tr>
  <tr><td>time</td><td>text/integer</td><td>hh:mm:ss/milliseconds since epoch</td><td><code>&lt;input type="time"/></code></td></tr>
  <tr><td>telephone</td><td>text</td><td>telephone number</td><td>custom input field</td></tr>
  <tr><td>url</td><td>text</td><td>web url</td><td><code>&lt;input type="url"/></code></td></tr>
  <tr><td>item</td><td>text</td><td>Item name</td><td>custom input field</td></tr>
  <tr><td>thing</td><td>text</td><td>UID of a thing</td><td>custom input field</td></tr>
  <tr><td>group</td><td>text</td><td>group name to which this parameter belongs</td><td></td></tr>
  <tr><td>tag</td><td>text</td><td>tag name</td><td>custom input field</td></tr>
  <tr><td>service</td><td>text</td><td>service name</td><td>custom input field</td></tr>
  <tr><td>channel</td><td>text</td><td>UID of a channel<br></td><td>custom input field</td></tr>
  <tr><td>rule</td><td>text</td><td>UID of a rule<br></td><td>custom input field</td></tr>
  <tr><td>location</td><td>text</td><td>latitude,longitude[,altitude]<br></td><td>custom input field</td></tr>
  
</table>

Further, the <strong>item</strong> context can contain criteria to filter the list of items. For example:

```xml
<filter>
  <criteria name="type">Switch,Dimmer</criteria>
  <criteria name="tag">Light,Heating</criteria>
</filter>
```

In the case of above filter only those items will be shown that satisfy the filter's conditions. The above filter is evaluated as follows: 

```
(type=Switch OR type=Dimmer) AND (tag=Light OR tag=Heating) 

```
Similarly, the <strong>Channel</strong> context can contain criteria to filter channels based on <strong>kind</strong> field. The value of <strong>kind</strong> can either be STATE or TRIGGER. For example:

```xml
<filter>
  <criteria name="kind">STATE|TRIGGER</criteria>
</filter>
```

Groups allow parameters to be grouped together into logical blocks so that the user can find the parameters they are looking for. A parameter can be placed into a group so that the UI knows how to display the information.
<table>
  <tr><td><b>Property</b></td><td><b>Description</b></td></tr>
  <tr><td>group.name</td><td>The group name - this is used to link the parameters into the group, along with the groupName option in the parameter (mandatory).</td></tr>
  <tr><td>label</td><td>The human-readable label of the group. (mandatory).</td></tr>
  <tr><td>description</td><td>The description of the group. (optional).</td></tr>
  <tr><td>context</td><td>Sets a context tag for the group. The context may be used in the UI to provide some feedback on the type of parameters in this group (optional).</td></tr>
  <tr><td>advanced</td><td>Specifies that this is an advanced group. The UI may hide this group from the user (optional).</td></tr>
</table>


The full XML schema for configuration descriptions is specified in the [ESH config description XSD](http://eclipse.org/smarthome/schemas/config-description-1.0.0.xsd) file.

**Hints:**

- Although the attribute `uri` is optional, it *must* be specified in configuration description files. Only for embedded configuration descriptions in documents for binding definitions and `Thing` type descriptions, the attribute is optional.


### Example

The following code gives an example for one configuration description.  

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config-description:config-description uri="thing-type:my-great-binding:my-bridge-name"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:config-description="http://eclipse.org/smarthome/schemas/config-description/v1.0.0"
    xsi:schemaLocation="http://eclipse.org/smarthome/schemas/config-description/v1.0.0
        http://eclipse.org/smarthome/schemas/config-description-1.0.0.xsd">

  <parameter name="ipAddress" type="text" required="true">
    <context>network-address</context>
    <label>Network Address</label>
    <description>Network address of the device.</description>
  </parameter>

  <parameter name="userName" type="text" required="true">
    <label>User Name</label>
  </parameter>

  <parameter name="password" type="text" required="false">
    <context>password</context>
  </parameter>

</config-description:config-description>
```

## Binding Definitions

Every binding has to provide meta information such as binding id or name. The meta information of all bindings is accessible through the `org.eclipse.smarthome.core.binding.BindingInfoRegistry` service.

Although binding definitions are usually specified in a declarative way (as described in this section), they can also be provided as `org.eclipse.smarthome.core.binding.BindingInfo`.
Any `BindingInfo` must be registered as service at the *OSGi* service registry. The full Java API for binding definitions can be found in the Java package `org.eclipse.smarthome.core.binding`.

Binding definitions must be placed as XML file(s) (with the ending `.xml`) in the bundle's folder `/ESH-INF/binding/`.


### XML Structure for Binding Definitions

```xml
<?xml version="1.0" encoding="UTF-8"?>
<binding:binding id="bindingID"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:binding="http://eclipse.org/smarthome/schemas/binding/v1.0.0"
    xsi:schemaLocation="http://eclipse.org/smarthome/schemas/binding/v1.0.0
        http://eclipse.org/smarthome/schemas/binding-1.0.0.xsd">

  <name>String</name>
  <description>String</description>
  <author>String</author>

  <config-description>
    ...
  </config-description>
  OR
  <config-description-ref uri="{binding|thing-type|channel-type|any_other}:bindingID:..." />

</binding:binding>
```

<table>
  <tr><td><b>Property</b></td><td><b>Description</b></td></tr>
  <tr><td>binding.id</td><td>An identifier for the binding (mandatory).</td></tr>
  <tr><td>name</td><td>A human-readable name for the binding (mandatory).</td></tr>
  <tr><td>description</td><td>A human-readable description for the binding (optional).</td></tr>
  <tr><td>author</td><td>The author of the binding (optional).</td></tr>
  <tr><td>service-id</td><td>The ID (service.pid or component.name) of the main binding service, which can be configured through OSGi configuration admin service. Should only be used in combination with a config description definition (optional).</td></tr>
  <tr><td>config-description</td><td>The configuration description for the binding within the ConfigDescriptionRegistry (optional).</td></tr>
  <tr><td>config-description-ref</td><td>The reference to a configuration description for the binding within the ConfigDescriptionRegistry (optional).</td></tr>
  <tr><td>config-description-ref.uri</td><td>The URI of the configuration description for the binding within the ConfigDescriptionRegistry (mandatory).</td></tr>
</table>

The full XML schema for binding definitions is specified in the [ESH binding XSD](http://eclipse.org/smarthome/schemas/binding-1.0.0.xsd) file.

**Hints:**

- The attribute `uri` in the section `config-description` is optional, it *should not* be specified in binding definition files because it's an embedded configuration. If the `uri` is *not* specified, the configuration description is registered as `binding:bindingID`, otherwise the given `uri` is used.
- If a configuration description is already specified somewhere else and the binding wants to (re-)use it, a `config-description-ref` should be used instead.
- Normally the service id must not be defined, because it is implicitly set to "binding.&lt;binding.id&gt;". A binding can register an OSGi service which implements the ManagedService interface and define the service.pid as e.g."binding.hue" to receive the configuration.


### Example

The following code gives an example for a binding definition.  

```xml
<?xml version="1.0" encoding="UTF-8"?>
<binding:binding id="hue"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:binding="http://eclipse.org/smarthome/schemas/binding/v1.0.0"
    xsi:schemaLocation="http://eclipse.org/smarthome/schemas/binding/v1.0.0
        http://eclipse.org/smarthome/schemas/binding-1.0.0.xsd">

  <name>hue Binding</name>
  <description>The hue Binding integrates the Philips hue system. It allows to control hue bulbs.</description>
  <author>ACME</author>

</binding:binding>
```

## Bridges and Thing Descriptions

Every binding has to provide meta information about which bridges and/or *Thing*s it provides and how their relations to each other are structured. In that way a binding could describe that it requires specific bridges to be operational or define which channels (e.g. temperature, color, etc.) it provides.

Every bridge or *Thing* has to provide meta information such as label or description. The meta information of all bridges and *Thing*s is accessible through the `org.eclipse.smarthome.core.thing.binding.ThingTypeProvider` service.

Bridge and *Thing* descriptions must be placed as XML file(s) (with the ending `.xml`) in the bundle's folder `/ESH-INF/thing/`. The full Java API for bridge and *Thing* descriptions can be found in the Java package `org.eclipse.smarthome.core.thing.type`.


### XML Structure for Thing Descriptions

```xml
<?xml version="1.0" encoding="UTF-8"?>
<thing:thing-descriptions bindingId="bindingID"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:thing="http://eclipse.org/smarthome/schemas/thing-description/v1.0.0"
    xsi:schemaLocation="http://eclipse.org/smarthome/schemas/thing-description/v1.0.0
        http://eclipse.org/smarthome/schemas/thing-description-1.0.0.xsd">

  <bridge-type id="bridgeTypeID" listed="{true|false}" extensible="channelTypeId1,channelTypeId2,...">
    <supported-bridge-type-refs>
      <bridge-type-ref id="bridgeTypeID" />
      ...
    </supported-bridge-type-refs>

    <label>String</label>
    <description>String</description>
    <category>String</category>

    <channels>
      <channel id="channelID" typeId="channelTypeID" />
      OR
      <channel id="channelID" typeId="channelTypeID">
        <label>String</label>
        <description>String</description>
      </channel>
      ...
    </channels>
    OR
    <channel-groups>
      <channel-group id="channelGroupID" typeId="channelGroupTypeID" />
      OR
      <channel-group id="channelGroupID" typeId="channelGroupTypeID">
        <label>String</label>
        <description>String</description>
      </channel-group>
      ...
    </channel-groups>

    <properties>
        <property name="propertyName">propertyValue</property>
        ...
    </properties>
    <representation-property>propertyName</representation-property>

    <config-description>
      ...
    </config-description>
    OR
    <config-description-ref uri="{binding|thing-type|channel-type|any_other}:bindingID:..." />
  </bridge-type>

  <thing-type id="thingTypeID" listed="{true|false}" extensible="channelTypeId1,channelTypeId2,...">
    <supported-bridge-type-refs>
      <bridge-type-ref id="bridgeTypeID" />
      ...
    </supported-bridge-type-refs>

    <label>String</label>
    <description>String</description>
    <category>String</category>

    <channels>
      <channel id="channelID" typeId="channelTypeID" />
      OR
      <channel id="channelID" typeId="channelTypeID">
        <label>String</label>
        <description>String</description>
      </channel>
      ...
    </channels>
    OR
    <channel-groups>
      <channel-group id="channelGroupID" typeId="channelGroupTypeID" />
      OR
      <channel-group id="channelGroupID" typeId="channelGroupTypeID">
        <label>String</label>
        <description>String</description>
      </channel-group>
      ...
    </channel-groups>

    <properties>
        <property name="propertyName">propertyValue</property>
        ...
    </properties>
    <representation-property>propertyName</representation-property>

    <config-description>
      ...
    </config-description>
    OR
    <config-description-ref uri="{binding|thing-type|channel-type|any_other}:bindingID:..." />
  </thing-type>

  <channel-type id="channelTypeID" advanced="{true|false}">
    <item-type>Dimmer</item-type>
    OR
    <kind>trigger</kind>
    <label>String</label>
    <description>String</description>
    <category>String</category>

    <tags>
      <tag>String</tag>
      ...
    </tags>

    <state min="decimal" max="decimal" step="decimal" pattern="String" readOnly="{true|false}">
      <options>
        <option value="String" />
        OR
        <option value="String">String</option>
        ...
      </options>
    </state>
    OR
    <event>
      <options>
        <option value="String" />
        OR
        <option value="String">String</option>
        ...
      </options>
    </event>

    <config-description>
      ...
    </config-description>
    OR
    <config-description-ref uri="{binding|thing-type|channel-type|any_other}:bindingID:..." />
  </channel-type>   

  <channel-group-type id="channelGroupTypeID" advanced="{true|false}">
    <label>String</label>
    <description>String</description>
    <category>String</category>

    <channels>
      <channel id="channelID" typeId="channelTypeID" />
      ...
    </channels>
  </channel-group-type>   

  ...

</thing:thing-descriptions>
```

<table>
  <tr><td><b>Property</b></td><td><b>Description</b></td></tr>
  <tr><td>thing-descriptions.bindingId</td><td>The identifier of the binding this types belong to (mandatory).</td></tr>
</table>

**Bridges and Things:**
<table>
  <tr><td><b>Property</b></td><td><b>Description</b></td></tr>
  <tr><td>bridge-type.id | thing-type.id</td><td>An identifier for the bridge/<i>Thing</i> type (mandatory).</td></tr>
  <tr><td>bridge-type.listed | thing-type.listed</td><td>Denotes if user interfaces should list the bridge/<i>Thing</i>, e.g. for pairing (optional, defaults to true).</td></tr>
  <tr><td>bridge-type.extensible | thing-type.extensible</td><td>If the bridge/<i>Thing</i> supports a generic number of channels the allowed channelTypeIds can be listed here (optional). This provides a hint for UIs to support adding/removing channels. Channel groups are not supported.</td></tr>
  <tr><td>supported-bridge-type-refs</td><td>The identifiers of the bridges this bridge/<i>Thing</i> can connect to (optional).</td></tr>
  <tr><td>bridge-type-ref.id</td><td>The identifier of a bridge this bridge/<i>Thing</i> can connect to (mandatory).</td></tr>
  <tr><td>label</td><td>A human-readable label for the bridge/<i>Thing</i> (mandatory).</td></tr>
  <tr><td>description</td><td>A human-readable description for the bridge/<i>Thing</i> (optional).</td></tr>
  <tr><td>category</td><td>Category this bridge/<i>Thing</i> belongs to, see <a href="../../concepts/categories.html">categories</a>) (optional).</td></tr>
  <tr><td>channels</td><td>The channels the bridge/<i>Thing</i> provides (optional).</td></tr>
  <tr><td>channel.id</td><td>An identifier of the channel the bridge/<i>Thing</i> provides (mandatory).</td></tr>
  <tr><td>channel.typeId</td><td>An identifier of the channel type definition the bridge/<i>Thing</i> provides (mandatory).</td></tr>
  <tr><td>label</td><td>A human-readable label for the channel (optional).</td></tr>
  <tr><td>description</td><td>A human-readable description for the channel (optional).</td></tr>
  <tr><td>channel-groups</td><td>The channel groups defining the channels the bridge/<i>Thing</i> provides (optional).</td></tr>
  <tr><td>channel-group.id</td><td>An identifier of the channel group the bridge/<i>Thing</i> provides (mandatory).</td></tr>
  <tr><td>channel-group.typeId</td><td>An identifier of the channel group type definition the bridge/<i>Thing</i> provides (mandatory).</td></tr>
  <tr><td>properties</td><td>Name/value pairs for properties to be set to the thing (optional).</td></tr>
  <tr><td>representation-property</td><td>The name of the property that contains a unique identifier of the thing (optional).</td></tr>
  <tr><td>config-description</td><td>The configuration description for the bridge/<i>Thing</i> within the ConfigDescriptionRegistry (optional).</td></tr>
  <tr><td>config-description-ref</td><td>The reference to a configuration description for the bridge/<i>Thing</i> within the ConfigDescriptionRegistry (optional).</td></tr>
  <tr><td>config-description-ref.uri</td><td>The URI of the configuration description for the bridge/<i>Thing</i> within the ConfigDescriptionRegistry (mandatory).</td></tr>
</table>

**Channels:**
<table>
  <tr><td><b>Property</b></td><td><b>Description</b></td></tr>
  <tr><td>channel-type.id</td><td>An identifier for the channel type (mandatory).</td></tr>
  <tr><td>channel-type.advanced</td><td>The flag indicating if this channel contains advanced functionalities which should be typically not shown in the basic view of user interfaces (optional, default: false).</td></tr>
  <tr><td>kind</td><td>The kind of channel. <code>state</code> for channels which have a state, <code>trigger</code> for trigger channels. <code>state</code> is the default.</td></tr>
  <tr><td>item-type</td><td>An item type of the channel (mandatory if kind <code>state</code>, which is the default). All item types are specified in <code>ItemFactory</code> instances. The following items belong to the core: <code>Switch, Rollershutter, Contact, String, Number, Dimmer, DateTime, Color, Image</code>.</td></tr>
  <tr><td>label</td><td>A human-readable label for the channel (mandatory).</td></tr>
  <tr><td>description</td><td>A human-readable description for the channel (optional).</td></tr>
  <tr><td>category</td><td>The category for the channel, e.g. <code>TEMPERATURE</code> (optional).</td></tr>
  <tr><td>tags</td><td>A list of default tags to be assigned to bound items (optional).</td></tr>
  <tr><td>tag</td><td>A tag semantically describes the feature (typical usage) of the channel e.g. <code>AlarmSystem</code>. There are no pre-default tags, they are custom-specific (mandatory).</td></tr>
  <tr><td>state</td><td>The restrictions of an item state which gives information how to interpret it (optional).</td></tr>
  <tr><td>state.min</td><td>The minimum decimal value of the range for the state (optional).</td></tr>
  <tr><td>state.max</td><td>The maximum decimal value of the range for the state (optional).</td></tr>
  <tr><td>state.step</td><td>The increasing/decreasing decimal step size within the defined range, specified by the minimum/maximum values (optional).</td></tr>
  <tr><td>state.pattern</td><td>The pattern following the <code>printf</code> syntax to render the state (optional).</td></tr>
  <tr><td>state.readOnly</td><td>The flag indicating if the state is read-only or can be modified (optional, default: false).</td></tr>
  <tr><td>options</td><td>A list restricting all possible values (optional).</td></tr>
  <tr><td>option</td><td>The description for the option (optional).</td></tr>
  <tr><td>option.value</td><td>The value for the option (mandatory). Note that the value may be outside of the range specified in the min/max if this is specified.</td></tr>
  <tr><td>event</td><td>The restrictions of an trigger event which gives information how to interpret it (optional).</td></tr>
  <tr><td>autoUpdatePolicy</td><td>The auto update policy to use (optional).</td></tr>
  <tr><td>config-description</td><td>The configuration description for the channel within the ConfigDescriptionRegistry (optional).</td></tr>
  <tr><td>config-description-ref</td><td>The reference to a configuration description for the channel within the ConfigDescriptionRegistry (optional).</td></tr>
  <tr><td>config-description-ref.uri</td><td>The URI of the configuration description for the channel within the ConfigDescriptionRegistry (mandatory).</td></tr>
</table>

**Channel Groups:**
<table>
  <tr><td><b>Property</b></td><td><b>Description</b></td></tr>
  <tr><td>channel-group-type.id</td><td>An identifier for the channel group type (mandatory).</td></tr>
  <tr><td>channel-group-type.advanced</td><td>The flag indicating if this channel group contains advanced functionalities which should be typically not shown in the basic view of user interfaces (optional, default: false).</td></tr>
  <tr><td>label</td><td>A human-readable label for the channel group (mandatory).</td></tr>
  <tr><td>description</td><td>A human-readable description for the channel group (optional).</td></tr>
  <tr><td>category</td><td>The category for the channel group, e.g. <code>TEMPERATURE</code> (optional).</td></tr>
  <tr><td>channels</td><td>The channels the bridge/<i>Thing</i> provides (mandatory).</td></tr>
  <tr><td>channel.id</td><td>An identifier of the channel the bridge/<i>Thing</i> provides (mandatory).</td></tr>
  <tr><td>channel.typeId</td><td>An identifier of the channel type definition the bridge/<i>Thing</i> provides (mandatory).</td></tr>
</table>

The full XML schema for Thing type descriptions is specified in the <a href="https://www.eclipse.org/smarthome/schemas/thing-description-1.0.0.xsd">ESH thing description XSD</a> file.

**Hints:**

-  Any identifiers of the types are automatically mapped to unique identifiers: `bindingID:id`.
-  The attribute `uri` in the section `config-description` is optional, it *should not* be specified in bridge/*Thing*/channel type definition files because it's an embedded configuration. If the `uri` is *not* specified, the configuration description is registered as `thing-type:bindingID:id` or `channel-type:bindingID:id` otherwise the given `uri` is used.
-  If a configuration description is already specified somewhere else and the bridge/*Thing*/channel type wants to (re-)use it, a `config-description-ref` should be used instead.

## Config Status Provider
Each entity that has a configuration can provide its current configuration status for UIs or specific services to point to issues or to provide further general information of the current configuration. For this purpose the handler of the entity has to implement the interface `org.eclipse.smarthome.config.core.status.ConfigStatusProvider` that has to be registered as OSGi service. The `org.eclipse.smarthome.config.core.status.ConfigStatusService` tracks each configuration status provider and delivers the corresponding `org.eclipse.smarthome.config.core.status.ConfigStatusInfo` by the operation `getConfigStatus(String, Locale)`.
