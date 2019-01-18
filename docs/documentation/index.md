---
layout: documentation
---

{% include base.html %}

# Documentation Overview

As Eclipse SmartHome is not an end user product, but  a framework to build end user solutions on top, you will only find technical documentation here. If you are an end user, please rather check out any of the [offers that use Eclipse SmartHome](https://www.eclipse.org/smarthome/index.html#references).

The technical documentation is split into the following sections:

 - [Concepts](concepts/items.html): Here you will find information about the fundamental concepts of Eclipse SmartHome and the used vocabulary.
 - [Features](features/index.html): This section explains what features Eclipse SmartHome offers and how they are configured and used.
 - [Development](development/ide.html): Everything you need to know when you want to develop code - be it for the core platform or for your own extensions.
 - [Community](community/contributing.html): Learn how to get in touch with the community and how you can contribute back to the project.
  
## Background 

### Why Eclipse SmartHome?

Since the emergence of broadband internet connections, smartphones and tablets the smart home market shows a remarkable upsurge. This has led to a very fragmented market, which makes it difficult for customers to "bet on the right horse". In fact, there is not one system, protocol or standard that could possibly fulfill all potential requirements. There is hence a need for platforms that allow the integration of different systems, protocols or standards and that provide a uniform way of user interaction and higher level services.

### How does Eclipse SmartHome help?

The goals of the Eclipse SmartHome project can be summarized as:

* Provide a flexible framework for smart home and ambient assisted living (AAL) solutions. This framework focuses on the use cases of this domain, e.g. on easy automation and visualization aspects.
* Specify extension points for integration possibilities and higher-level services. Extending and thus customizing the solution must be as simple as possible and this requires concise and dedicated interfaces.
* Provide implementations of extensions for relevant systems, protocols or standards. Many of them can be useful to many smart home solutions, so this project will provide a set of extensions that can be included if desired. They can also be in the shape of a general Java library or an OSGi bundle, so that these implementations can be used independently of the rest of the project as well.
* Provide a development environment and tools to foster implementations of extensions. The right tooling can support the emergence of further extensions and thus stimulates future contributions to the project.
* Create a packaging and demo setups. Although the focus is on the framework, it needs to be shown how to package a real solution from it, which can be used as a starting point and for demo purposes.
Description
* The Eclipse SmartHome project is a framework that allows building smart home solutions that have a strong focus on heterogeneous environments, i.e. solutions that deal with the integration of different protocols or standards. Its purpose is to provide a uniform access to devices and information and to facilitate different kinds of interactions with them. This framework consists out of a set of OSGi bundles that can be deployed on an OSGi runtime and which defines OSGi services as extension points.

The stack is meant to be usable on any kind of system that can run an OSGi stack - be it a multi-core server, a residential gateway or a Raspberry Pi.

The project focuses on services and APIs for the following topics:

1. _Data Handling_: This includes a basic but extensible type system for smart home data and commands that provides a common ground for an abstracted data and device access as well as event mechanisms to send this information around. It is the most important topic for integrating with other systems, which is done through so called bindings, which are a special type of extension.
1. _Rule Engines_: A flexible rule engine that allows changing rules during runtime and which defines extension types that allow breaking down rules into smaller pieces like triggers, actions, logic modules and templates.
1. _Declarative User Interfaces_: A framework with extensions for describing user interface content in a declarative way. This includes widgets, icons, charts etc.
1. _Persistence Management_: Infrastructure that allows automatic data processing based on a simple and unified configuration. Persistence services are pluggable extensions, which can be anything from a log writer to an IoT cloud service.

Besides the runtime framework and implementation, the Eclipse SmartHome projects also provides different kinds of tools and samples:

* Language Server Protocol (LSP) support for content assist and syntax validation. This may be used by solutions to provide full IDE support for editing configuration models and rules.
* Maven archetypes to easily create skeletons for extensions
* Demo packaging with other Eclipse IoT projects
