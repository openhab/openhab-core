---
layout: documentation
---

{% include base.html %}

# Scenes

In the general context of home automation, a scene is a defined set of states of one or more home devices. An example of
such a scene could be a **night scene**, which turns on all the indoor lights. The notion of scenes is not directly
available in Eclipse SmartHome, but scenes can be created by the means of using rules.

## Concept

In Eclipse SmartHome, a scene is created by defining states for different items inside the action section of a rule. The
triggers and conditions of the rule are left empty. Once the rule is activated, the actions set the state of the items
thereby activating a specific scene.

A scene can only be activated and no deactivation is possible. It can be activated either manually or automatically. To
manually activate a scene, a **runnow** request must be sent to the rest endpoint. To automate the scene activation, a
second rule must be created. The action section of this second rule is responsible for executing the scene with the
`core.RunRuleAction` module type.

The `core.RunRuleAction` module type, used for running the scenes, supports the direct execution of the actions without
requiring to evaluate the conditions. This way all actions are executed even if an action before returns an error.


## Scene creation and activation

A scene can be created by making a POST request to the following endpoint provided by the rule-engine:

`POST /rest/rules`


 * **Sample scene instance created using rule-engine:**

```
{
  "uid": "light.scene1",
  "name": "IndoorLightScene",
  "tags": [
    "night",
    "scene"
  ],
  "description": "A night scene for indoor lights.",
  "triggers": [],
  "conditions": [],
  "actions": [
    {
      "id": "ItemPostCommandActionID1",
      "type": "core.ItemCommandAction",
      "configuration": {
        "itemName": "corridorLightItem",
        "command": "ON"
      }
    },
    {
      "id": "ItemPostCommandActionID2",
      "type": "core.ItemCommandAction",
      "configuration": {
        "itemName": "roomLampItem",
        "command": "ON"
      }
    }
  ]
}
```

The sample scene shown above turns on the `corridorLightItem` and `roomLampItem` by sending the ON command to both of
these items. Notice that this rule contains a tag called **scene**. It is advisable to use such a tag on a rule-scene to
express its purpose.

* **The above scene can be activated by sending a PUT request to the following endpoint:**
 `/rest/rules/sample.scene1/runnow`

* **It can also be activated by defining an action within another rule as shown in the code block below:**

```
{
  "name": "SceneActivationSampleRule",
  "uid": "SceneActivationSampleRule_1",
  "tags": [
    "sample",
    "item",
    "rule"
  ],
  "configuration": {},
  "description": "Sample Rule for activating scenes.",
  "triggers": [
    {
      "id": "ItemStateChangeTriggerID",
      "type": "core.GenericEventTrigger",
      "configuration": {
        "eventSource": "myAstroItem",
        "eventTopic": "smarthome/items/*",
        "eventTypes": "ItemStateEvent"
      }
    }
  ],
  "actions": [
    {
      "id": "enableSceneAction",
      "type": "core.RunRuleAction",
      "configuration": {
        "ruleUIDs": "light.scene1"
      }
    }
  ]
}
```
## REST API
You will need the rule REST endpoints provided by the rule-engine to work with the scenes. The complete list of the REST
operations can be found [here](rules.html#rest-api).
