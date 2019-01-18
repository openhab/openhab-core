---
layout: documentation
---

{% include base.html %}

# Feature Overview

Eclipse SmartHome is a framework for building smart home solutions. With its very flexible architecture, it fosters the modularity provided by OSGi for Java applications.
As such, Eclipse SmartHome consists of a rich set of OSGi bundles that serve different purposes. Not all solutions that build on top of Eclipse SmartHome will require all of those bundles - instead they can choose what parts are interesting for them.

There are the following categories of bundles:

 - `config`: everything that is concerned with general configuration of the system like config files, xml parsing, discovery, etc.	
 - `core`: the main bundles for the logical operation of the system - based on the abstract item and event concepts.
 - `io`: all kinds of optional functionality that have to do with I/O like console commands, audio support or HTTP/REST communication.
 - `model`: support for domain specific languages (DSLs). 
 - `designer`: Eclipse RCP support for DSLs and other configuration files.
 - `ui`: user interface related bundles that provide services that can be used by different UIs, such as charting or icons.

## Runtime Services

### Optional Bundles

 - `org.eclipse.smarthome.core.id`: [Unique instance IDs](core/id.html)
 - `org.eclipse.smarthome.ui.icon`: [Icon support](icons.html)

Besides the very core framework that is mandatory for all solutions, there are many optional features like the support for textual configurations (DSLs), the REST API or the sitemap support.

## Extensions

Being a framework, Eclipse SmartHome defines many extension types that allows building modular solutions with pluggable components (extensions). 

The list of extension types will grow over time and you are invited to discuss useful extension types in [our forum](https://www.eclipse.org/forums/eclipse.smarthome).

Note that many "existing" extension types like rule actions, persistence services, TTS modules, etc. are not covered in this documentation as it is planned to address and heavily refactor them in future - the current version is still from the initial contribution which came from openHAB 1 and thus is tight to textual configuration and not usable in a wider context.

#### Extension Service

Eclipse SmartHome comes with an API that allows implementing a service that manages the installed extensions within a solution. All that needs to be done is to register an OSGi service that implements `org.eclipse.smarthome.core.extension.ExtensionService`. Such a service has to provide a list of available extensions and then can be called in order to install and uninstall them.

What kind of implementation is chosen is completely up to the solution. Suitable mechanisms might be Eclipse p2, Apache Felix FileInstall, Apache Karaf FeatureInstaller, etc. For testing purposes, Eclipse SmartHome comes with a sample implementation in the bundle `org.eclipse.smarthome.core.extension.sample`.

Installation and uninstallation requests are executed by a thread pool named "extensionService". If an implementation does not support concurrent execution of such operations, the thread pool size should be set to 1.

### Bindings

A binding is an extension to the Eclipse SmartHome runtime that integrates an external system like a service, a protocol or a single device. Therefore the main purpose of a binding is to translate events from the Eclipse SmartHome event bus to the external system and vice versa. Learn about the internals of a binding in our [binding tutorial](../development/bindings/how-to.html).

Bindings can optionally include [discovery services](../concepts/discovery.html), which allow the system to automatically find accessible devices and services. Furthermore, they can register devices as [audio sources and audio sinks](../concepts/audio.html), so that microphones and speakers can be made available to the audio and voice support features of the framework.

### User Interfaces

User interfaces normally use the REST API for communication, but if they are not client-side, but served from the runtime, they also have the option to use all local Java services.

Currently, there are 3 available user interfaces in Eclipse SmartHome: the Classic UI, the Basic UI and the Paper UI.

All user interfaces can share icon sets, so that these do not have to be included in every single user interface.
Eclipse SmartHome comes with the following iconsets:

 - `org.eclipse.smarthome.ui.iconset.classic`: [Classic Icon Set](ui/iconset/classic/readme.html)

### Voice Services
 
Voice extensions provide implementations for Text-to-Speech, Speech-to-Text and Human Language Interpreter services.
 
These services are often very solution specific, so there is no one-fits-all implementation in Eclipse SmartHome.
For easy demonstration, there is a TTS service available, which uses the built-in "say" command of MacOS (which obviously only works on Macs, though).
Additionally, there is a basic human language interpreter implementation, which supports simple smart home commands like switching lights and controlling music both in English and German.
 
