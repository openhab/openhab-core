# Dashboard

The openHAB dashboard is a landing page for the user where all openHAB UIs can be found.
The Dashboard also supports links to external services.
Links can be added to the dashboard by editing the `$OPENHAB_CONF/services/dashboard.cfg` configuration file.

## Link Configuration

| Parameter Name                | Type    | Description                                   |
|-------------------------------|---------|-----------------------------------------------|
| `<unique-name>.link-name`     | String  | Name which is shown in the openHAB dashboard. |
| `<unique-name>.link-url`      | String  | URL to external service.                      |
| `<unique-name>.link-imageurl` | String  | URL to image which is shown in the dashboard. |

Where `<unique-name>` is link unique identifier (see examples).

Link-url support following special words:

| Special word                | Replaced to                               |
|-----------------------------|-------------------------------------------|
| {HOST}                      | host name including possible port number  |
| {HOSTNAME}                  | host name                                 |


### Image

A png image of 350px width and 200-250px height are recommended.

The `imageurl` property can either be a direct HTTP link or data URIs according to [RFC2397](https://tools.ietf.org/html/rfc2397). If data URIs are used, browser should support them as well. All five major browsers (Chrome, Firefox, IE, Opera and Safari) support data URIs. See e.g. [https://www.base64-image.de](https://www.base64-image.de) to convert images to base64 coded data. 

## Example Configuration File

```
frontail.link-name=openHAB Log Viewer
frontail.link-url=http://<server-adddress>:9001
frontail.link-imageurl=../static/image.png

nodered.link-name=Node-RED
nodered.link-url=http://<server-adddress>:1880
nodered.link-imageurl=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAXwAAACfCAIAAA...QmCC

nodered.link-name=Node-RED
nodered.link-url=http://{HOSTNAME}:1880
nodered.link-imageurl=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAXwAAACfCAIAAA...QmCC

```

Note: **nodered** image data URL is not valid (it's shorten for the sake of clarity).
