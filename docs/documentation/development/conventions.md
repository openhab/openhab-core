---
layout: documentation
---

{% include base.html %}

# Conventions

## Null Annotations

[Null annotations](https://wiki.eclipse.org/JDT_Core/Null_Analysis) are used from the Eclipse JDT project.
The intention of these annotations is to transfer a method's contract written in its JavaDoc into the code to be processed by tools.
These annotations can be used for **static** checks, but **not** at runtime.

Thus for publicly exposed methods that belong to the API and are (potentially) called by external callers, a `null` check cannot be omitted, although a method parameter is marked to be not `null` via an annotation.
There will be a warning in the IDE for this check, but that is fine.
For private methods or methods in an internal package the annotations are respected and additional `null` checks are omitted.

To use the annotations, every bundle must have an **optional** `Import-Package` dependency to `org.eclipse.jdt.annotation`.
Classes should be annotated with `@NonNullByDefault`:

```java
@NonNullByDefault
public class MyClass(){}
```

Return types, parameter types, generic types etc. are annotated with `@Nullable` only.
The annotation should be written in front of the type.

Fields should be annotated like this:

```java
private @Nullable MyType myField;
```

Methods should be annotated as follows:

```java
private @Nullable MyReturnType myMethod(){};
```

Fields that get a static and mandatory reference injected through OSGi Declarative Services can be annotated with

```java
private @NonNullByDefault({}) MyService injectedService;
```

to skip the nullevaluation for these fields.
Fields within `ThingHandler` classes that are initialized within the `initialize()` method may also be annotated like this, because the framework ensures that `initialize()` will be called before any other method.
However, please watch the scenario where the initialization of the handler fails, because fields might not have been initialized and using them should be prepended by a `null` check.

There is **no need** for a `@NonNull` annotation because it is set as default.
Test classes do not have to be annotated (the usage of `SuppressWarnings("null")` is allowed, too).

The transition of existing classes can be a longer process, but using nullness annotations in a class / interface requires to set the default for the whole class and annotations on all types that differ from the default.
