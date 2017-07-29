# Home Builder

> Boilerplate for the [Items](http://docs.openhab.org/configuration/items.html), [sitemap](http://docs.openhab.org/configuration/sitemaps.html) files and [HABPanel](http://docs.openhab.org/addons/uis/habpanel/readme.html) dashboard.

## Items

The tool allows you to generate openHAB Items for your home structure.
You can choose to generate textual `*.items` file content or construct a request directly to the REST API that'll create the items for you.


### Features

- Classifies the objects within each room and creates groups for them
- Optionally adds icons from [Classic Icon Set](http://docs.openhab.org/addons/iconsets/classic/readme.html) to the items
- Optionally adds Tags to the items - convenient for [HomeKit](http://docs.openhab.org/addons/io/homekit/readme.html)/[Hue Emulation](http://docs.openhab.org/addons/io/hueemulation/readme.html#device-tagging) add-ons users
- Automatically aligns the items vertically
- Generates a [Sitemap](http://docs.openhab.org/configuration/sitemaps.html) file
- Generates a set of HABPanel Dashboards corresponding with the Items