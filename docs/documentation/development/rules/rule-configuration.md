---
layout: documentation
---

{% include base.html %}

# SmartHome Rule Configuration

This document intends to describe the JSON meta definitions for the commonly used module types `ItemStateChangeTrigger`, `ItemStateCondition`, and `ItemCommandAction` in a more textual and intuitive way.

## Item State Change Trigger Configuration

Item state change triggers fire on state changes of a specified item defined in the `itemName` attribute. A trigger's type is to be set to `ItemStateChangeTrigger` when used as a state change trigger. Unlike the related `ItemStateUpdateTrigger`, this trigger requires the triggering state to have changed to a different value.

    {
      "id": "trigger_1",
      "label": "Item State Change Trigger",
      "description": "This triggers a rule if an items state changed",
      "configuration": {
        "itemName": "switchA"
      },
      "type": "ItemStateChangeTrigger"
    }

## Item State Condition Configuration

Rule conditions are usually represented by the following JSON object:

    {
      "inputs": {},
      "id": "condition_1",
      "label": "Item state condition",
      "description": "compares the items current state with the given",
      "configuration": {
        "itemName": "switchA",
        "state": "ON",
        "operator": "="
      },
      "type": "ItemStateCondition"
    }

`itemName` again holds the unique identifier of the polled item. `state` is one of the corresponding item type's supported state strings. The state string is automatically converted to a state object that fits its value and is supported by the corresponding item. For example, `ON` will be converted to an `OnOffType` and `120,100,100` will be converted to an `HSBType`. `operator` specifies a comparative operator, namely one of the following: `=`, `!=`, `<`, `>`

## Action Command Configuration

Similarly to `ItemStateCondition`s, action command configurations reference an item by name and an action string:

    {
      "inputs": {},
      "id": "action_1",
      "label": "Post command to an item",
      "description": "posts commands on items",
      "configuration": {
        "itemName": "switchB",
        "command": "OFF"
      },
      "type": "ItemPostCommandAction"
    }

The string used as the command depends on the item type and its corresponding supported command types, e.g. an HSB value of `120,100,100` to set a colored light's color to green. Similar to state change triggers, the correct state/action type is chosen automatically.

## Item Types and Command Type Formatting

See [Items](../../concepts/items.html)
