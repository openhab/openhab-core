---
layout: documentation
---

{% include base.html %}

# Frequently Asked Questions (FAQs)

Here is a list of frequently asked questions around the development of bindings. If you do not find an answer to your question, do not hesitate to ask it on the support forum.

### Structuring Things and Thing Types

1. _I am implementing a binding for system X. Shall I design this as one Thing or as a Bridge with multiple Things for the different functionalities?_ 
  
    In general, both options are valid:

    1. You have one Thing which has channels for all your actors, sensors and other functions
    2. You have one Bridge and an additional Thing for every actor and sensor and they would hold the channels

    The preferred architecture is the latter, if this is feasible. This means that the physical devices should be represented by a Thing each. This only makes sense if your system allows you to identify the different physical devices at all. Especially, such an architecture is useful if you can do a discovery of new devices that could then be presented to the user/admin.
    
    If your system does not provide you any possibility to get hold of such information, but rather only shows you a "logical" view of it, then 1) is also a valid option to pursue.
  
2. _Do I have to create XML files in `ESH-INF/thing` for all devices or is there any other option?_

    No, the XML files are only one way to describe your devices. Alternatively, you can implement your own [ThingTypeProvider](https://github.com/eclipse/smarthome/blob/master/bundles/core/org.eclipse.smarthome.core.thing/src/main/java/org/eclipse/smarthome/core/thing/binding/ThingTypeProvider.java), through which you can provide thing descriptions in a programmatic way. Nonetheless, the static XML descriptions of thing types can be picked up for documentation generation and other purposes. So whenever possible, static XML descriptions should be provided. 

3. _For my system XY, there are so many different variants of devices. Do I really need to define a thing type for every single one of them?_

    Thing types are important if you have no chance to request any structural information about the devices from your system and if you need users to manually chose a thing to add or configure (i.e. there is also no automatic discovery). The thing types that you provide will be the list the user can choose from. If your system supports auto-discovery and you can also dynamically construct things (and their channels) from structural information that you can access during runtime, there is in theory no need to provide any thing type description at all. Nonetheless, static descriptions of thing types have the advantage that the user knows which kind of devices are supported, no matter if he has a device at home or not - so you should at least have static XML descriptions for the devices that are known to you at implementation time.
     
4. _I have a device that can have different firmware versions with slightly different functionality. Should I create one or two thing types for it?_
   
    If the firmware version makes a huge difference for the device (and can be seen as a different model of it), then it is ok to have different things defined. If the list of channels can be determined by knowing the firmware revision, this is good. If you can only determine the existing channels by querying the device itself, it might be the better option to have only one thing type and construct its channels upon first access.

5. _When creating a Thing through my ThingHanderFactory, does it exactly have to have the channels that are defined in the thing type description?_
 
    It must at least have the channels that are defined in its thing type (and they are already automatically added by the super() implementation). Nonetheless, you are free to add any number of additional channels to the thing.

### State Updates of Channels

1. I have an image in my binding and want to pass it to the framework. What is the best way to achieve this?

    The Thing that wants to provide the image should have a Channel defined with `<item-type>Image</item-type>`.
The `ThingHandler` can update this Channel with a state of type `RawType` that represents the raw bytes of the image.
If the image should be downloaded from a URL, the helper method `HttpUtil.downloadImage(URL url)` can be used.
A user may link this Channel to an Item of type `Image` which can then be displayed.
Please note that data put as a `RawType` in a Channel will stay in **memory only**, i.e., this data will **not** be persisted anywhere.
Also keep in mind that the memory needed for these images will be consumed on the server running the framework, so creating a lot of `RawType` channels is not recommended.
