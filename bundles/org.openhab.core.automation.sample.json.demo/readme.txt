# Eclipse SmartHome Automation Json Demo #

## Description ##

The purpose of the demo is to give an example, of how to define a rule in a .json file.
The Structure of the defined rule is as follows.
Triggers:
ItemStateChangeTrigger which is configured to trigger the rule if state of DemoSwitch item is changed.

Actions:
ItemPostCommandAction which is configured to turn on the DemoDimmer item.

To trigger the rule provided by this demo You need to send a command to the DemoSwitch item.
Use smarthome send DemoSwitch <command>