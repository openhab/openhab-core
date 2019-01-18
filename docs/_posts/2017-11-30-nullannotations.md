---
layout: post
title: "Less NPE's - Eclipse SmartHome now uses Eclipse null annotations!"
date:   2017-11-30
image: "2017-11-30-nullannotations.png"
published: true
---

`NullPointerException`s (aka NPE's) are pretty nasty since they are `RuntimeException`s and thus developers do not immediately notice them while coding in their IDE.
Hence, data-flow analyses where written that show potential accesses to null pointers before they occur at runtime. Eclipse SmartHome (ESH) decided to make use of these analyses and get rid of (some) null pointers.

<!--more-->

Two popular projects that offer such an analysis are the [Checker Framework](https://checkerframework.org) and the [Eclipse JDT compiler](https://wiki.eclipse.org/JDT_Core/Null_Analysis).
In order to obtain better results from these analyses, developers can make use of annotations in the code.
In the current snapshots and in the next release such annotations are supported in the provided Eclipse IDE setup and Maven configuration.
The Eclipse SmartHome project decided to use the implementation offered by the Eclipse JDT compiler.

#### Making JavaDoc comments explicit

Since ESH is a framework that will be integrated into solutions which are build by others, it has provided proper Javadoc documentation on the public interfaces in the past.
In these Javadoc comments the project states that a method will never return `null`, a parameter should never be `null` or is optional and thus may be `null`.
Maintaining these comments by hand is error-prone and they can be easily overseen by developers.
Therefore ESH has decided to provide these comments in a machine readable way so the IDE can support developers in showing them errors or warnings in case they violate them.

As described in the [Coding Guidelines]({{ site.url }}/documentation/development/guidelines.html#a-code-style), classes are annotated with the `@NonNullByDefault` annotation which declares that every field, return value, parameter defined in this class will always have other values than `null`.
If a specific field, return value, or parameter in the class should be allowed to become `null` is is annotated with `@Nullable`.
Basically what these annotations are doing is translating the contract written in the Javadoc comment into something that can automatically be checked by the compiler.

ESH has already started to annotate the core packages so binding developers can benefit from them.

### Real life examples

One simple example is the [ThingHandler:handleCommand(ChannelUID channelUID, Command command)](https://github.com/eclipse/smarthome/blob/master/bundles/core/org.eclipse.smarthome.core.thing/src/main/java/org/eclipse/smarthome/core/thing/binding/ThingHandler.java#L99).
It guarantees to binding developers that both arguments are **not** `null`.
Therefore they can safely implement their logic by going through the IDs via `switch (channelUID.getId())` without the risk of running into a NPE.

{:.center}
![NonNull annotation]({{ site.url }}/img/blog/2017-11-30-nullannotations_handleCommand.png)

For bindings which use bridge things and call [BaseThingHandler:getBridge()](https://github.com/eclipse/smarthome/blob/master/bundles/core/org.eclipse.smarthome.core.thing/src/main/java/org/eclipse/smarthome/core/thing/binding/BaseThingHandler.java#L584) there now is the `@Nullable` annotation in place that reminds developers to do a proper `null` check before using the return value of this method.
Without the `null` check, implementors will get a warning as shown below.

{:.center}
![Nullable annotation with warning]({{ site.url }}/img/blog/2017-11-30-nullannotations_bridgeNullable.png)

After the `null` check has been implemented, the warning is gone:

{:.center}
![Nullable annotation without warning]({{ site.url }}/img/blog/2017-11-30-nullannotations_bridgeNullableCorrected.png)

If developers violate the `null` specifications, the IDE will immediately show an error.
In the example below, the `BaseThingHandler:updateState(String channelID, State state)` method requires both parameters to be non-`null`.
Since in the `else` branch the variable used as a second argument is set to `null`, the IDE infers that `null` is passed as an argument to the method and shows an error.

{:.center}
![Null violation]({{ site.url }}/img/blog/2017-11-30-nullannotations_error.png)

If you are interested to see a fully annotated binding, please have a look at the [Philips Hue binding](https://github.com/eclipse/smarthome/tree/master/extensions/binding/org.eclipse.smarthome.binding.hue).


### Outlook: Third party code

If ESH integrates external libraries and use their return values in the framework code, the compiler cannot decide whether these values are always non-`null` or might become `null` at some point.
This will generate a warning that is currently switched off by the IDE and Maven setup.
In the future, the plan is to support *external annotations* which are files that contain information about the method signatures of external libraries.
The [Last NPE project](http://www.lastnpe.org) has started a crowd-sourcing initative to create these external annotations and ESH is planing to use them.
