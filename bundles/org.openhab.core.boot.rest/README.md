# openHAB About distribution

This Bundle provides a REST resource to query
the core and distribution name, version, build-date and similar information.

A distribution optionally wants to provide a file in the openHAB
system directory `{OPENHAB_DIR}/etc/distribution.properties`:

An example could look like:

```
name=openhabian
version=1.4.1
abouturl=https://www.openhab.org/docs/installation/openhabian.html
```
