---
layout: documentation
---

{% include base.html %}

# Icon Support

Bundle: `org.eclipse.smarthome.ui.icon`

## Icon Servlet

Eclipse SmartHome comes with a flexible infrastructure for handling icons that are to be used within user interfaces.
This bunde registers a servlet under the url `/icon/`, which can then be easily queried for icons using GET requests of the form `/icon/<category>?state=<state>&format=[png|svg]&iconset=<iconsetid>`, where 
- `category` is one from the [list of channel categories](../concepts/categories.html#channel-categories) or any custom category that might be used within the solution
-`state` (optional) is the string-representation of an item state
- `format` (optional) defines the requested format to be either PNG or SVG 
- `iconset` (optional) specifies an iconset to use

If no icon set is specified in the request, "classic" will be used as a default. This default setting can be configured by the setting:

```
org.eclipse.smarthome.iconset:default=<iconsetId>
```

## Icon Sets

Icon sets can either provide icons in PNG or SVG format or both. All standard channel categories should be covered by the icon set in the supported format.
Icon sets can easily be added as additional bundles. All that has to be done is to register a service, which implements the `IconProvider` interface. Icons are provided as byte streams, so they do not have to be static resources, but can also be dynamically generated at runtime (e.g. for providing specific icons for certain states).
 
