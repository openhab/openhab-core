# Dashboard tiles

OpenHAB dashboard is landing page for the user where all openHAB UI's can be found. Dashboard support also links to external services. Links can be added to dashboard by ```conf/services/dashboard.cfg``` configuration file.

## Link Configuration

| Parameter name  | Type    | Description                                                                             |
|-----------------|---------|-----------------------------------------------------------------------------------------|
| link-nameX      | String  | Name which is shown in the openHAB dashboard.                                           |
| link-urlX       | String  | URL to external service.                                                                |
| link-overlayX   | String  | Image overlay icon. Supported values are empty (no icon), "html5", "android" or "apple" |
| link-imageurlX  | String  | URL to image.                                                                           |

Where X is link unique number (see examples). All configuration parameters need to start with ```org.openhab.ui.dashboard:``` prefix.

## Image URL

Image URL support several URL formats. URL support direct http links to local or Internet servers, but also the "data" URL scheme (RFC2397) is supported. See e.g. [https://www.base64-image.de](https://www.base64-image.de) to convert images to base64 coded data.

## Example configuration file
```
org.openhab.ui.dashboard:link-name1=openHAB Log Viewer
org.openhab.ui.dashboard:link-url1=http://localhost:9001
org.openhab.ui.dashboard:link-overlay1=
org.openhab.ui.dashboard:link-imageurl1=http://localhost:8080/static/image.png

org.openhab.ui.dashboard:link-name2=Node-RED
org.openhab.ui.dashboard:link-url2=http://localhost:1880
org.openhab.ui.dashboard:link-overlay2=
org.openhab.ui.dashboard:link-imageurl2=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAXwAAACfCAIAAA...QmCC

```

Note: **Link 2** image data URL is not valid (it's shorten for the sake of clarity).
