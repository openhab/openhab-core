---
layout: documentation
---

{% include base.html %}

# Coding Guidelines

The following guidelines apply to all code of the Eclipse SmartHome project.
They must be followed to ensure a consistent code base for easy readability and maintainability.
Exceptions can certainly be made, but they should be discussed and approved by a project committer upfront.

Note that this list also serves as a checklist for code reviews on pull requests.
To speed up the contribution process, we therefore advice to go through this checklist yourself before creating a pull request.

## A. Code Style

1. The [Java naming conventions](http://java.about.com/od/javasyntax/a/nameconventions.htm) should be used.
1. Every Java file must have a license header. You can run `mvn license:format` on the root of the repo to automatically add missing headers.
1. Every class, interface and enumeration should have JavaDoc describing its purpose and usage.
1. Every class, interface and enumeration must have an `@author` tag in its JavaDoc for every author that wrote a substantial part of the file.
1. Every constant, field and method with default, protected or public visibility should have JavaDoc (optional, but encouraged for private visibility as well).
1. Code must be formatted using the provided code formatter and clean up settings. They are set up automatically by the official [IDE setup](ide.html).
1. Generics must be used where applicable.
1. Code should not show any warnings. Warnings that cannot be circumvented should be suppressed by using the `@SuppressWarnings` annotation. 
1. For dependency injection, OSGi Declarative Services should be used.
1. OSGi Declarative Services should be declared using annotations. The IDE will take care of the service *.xml file creation. See the official OSGi documentation for an [example here](http://enroute.osgi.org/services/org.osgi.service.component.html). We always use `@Activate`, `@Deactivate` and `@Modified` if we define these methods, even if they exist in a super class, to make the code more readable.
1. Packages that contain classes that are not meant to be used by other bundles should have "internal" in their package name.
1. [Null annotations](https://wiki.eclipse.org/JDT_Core/Null_Analysis) are used from the Eclipse JDT project. `@NonNullByDefault` and `@Nullable` should be used, for details see [Null annotation conventions](conventions.html#null-annotations).

## B. OSGi Bundles

1. Every bundle must contain a Maven pom.xml with a version and artifact name that is in sync with the manifest entry. The pom.xml must reference the correct parent pom (which is usually in the parent folder).
1. Every bundle must contain a [NOTICE](https://www.eclipse.org/projects/handbook/#legaldoc) file, providing meta information about the bundle and license information about 3rd party content.
1. Every bundle must contain a build.properties file, which lists all resources that should end up in the binary under `bin.includes`.
1. The manifest must not contain any "Require-Bundle" entries. Instead, "Import-Package" must be used.
1. The manifest must not export any internal package.
1. The manifest must not have any version constraint on package imports, unless this is thoughtfully added. Note that Eclipse automatically adds these constraints based on the version in the target platform, which might be too high in many cases.
1. The manifest must include all services in the Service-Component entry. A good approach is to put OSGI-INF/*.xml in there.
1. Every exported package of a bundle must be imported by the bundle itself again.
1. Any 3rd party content has to be added thoughtfully and version/license information has to be given in the NOTICE file.

## C. Language Levels and Libraries

1. Eclipse SmartHome generally targets JavaSE 8 with the following restrictions:
 * To allow optimized JavaSE 8 runtimes, the set of Java packages to be used is furthermore restricted to [Compact Profile 2](http://www.oracle.com/technetwork/java/embedded/resources/tech/compact-profiles-overview-2157132.html)
 * Java 5 for org.eclipse.smarthome.protocols.enocean.*
1. The minimum OSGi framework version supported is [OSGi R4.2](http://www.osgi.org/Download/Release4V42), no newer features must be used.
1. For logging, slf4j (v1.7.2) is used.
1. A few common utility libraries are available that every Eclipse SmartHome based solution has to provide and which can be used throughout the code (and which are made available in the target platform):
 - Apache Commons IO (v2.2)
 - Apache Commons Lang (v2.6)
 - ~~Google Guava (v10.0.1)~~ (historically allowed, to be avoided in new contributions)

## D. Runtime Behavior

1. Overridden methods from abstract classes or interfaces are expected to return fast unless otherwise stated in their JavaDoc. Expensive operations should therefore rather be scheduled as a job.
1. Creation of threads must be avoided. Instead, resort into using existing schedulers which use pre-configured thread pools. If there is no suitable scheduler available, start a discussion in the forum about it rather than creating a thread by yourself. For periodically executed jobs that do not require a fixed rate [scheduleWithFixedDelay](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ScheduledExecutorService.html#scheduleWithFixedDelay(java.lang.Runnable,%20long,%20long,%20java.util.concurrent.TimeUnit)) should be preferred over [scheduleAtFixedRate](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ScheduledExecutorService.html#scheduleAtFixedRate(java.lang.Runnable,%20long,%20long,%20java.util.concurrent.TimeUnit)).
1. Bundles need to cleanly start and stop without throwing exceptions or malfunctioning. This can be tested by manually starting and stopping the bundle from the console (`stop <bundle-id>` resp. `start <bundle-id>`).
1. Bundles must not require any substantial CPU time. Test this e.g. using "top" or VisualVM and compare CPU utilization with your bundle stopped vs. started.

## E. Logging

1. As we are in a dynamic OSGi environment, loggers should be [non-static](http://slf4j.org/faq.html#declared_static), when ever possible and have the name `logger`.
1. Parametrized logging must be used (instead of string concatenation).
1. Where ever unchecked exceptions are caught and logged, the exception should be added as a last parameter to the logging. For checked exceptions, this is normally not recommended, unless it can be considered an error situation and the stacktrace would hold additional important information for the analysis.
1. Logging levels should focus on the system itself and describe its state. As every bundle is only one out of many, logging should be done very scarce. It should be up to the user to increase the logging level for specific bundles, packages or classes if necessary. This means in detail:
 - Most logging should be done in `debug` level. `trace` can be used for even more details, where necessary.
 - Only few important things should be logged in `info` level, e.g. a newly started component or a user file that has been loaded.
 - `warn` logging should only be used to inform the user that something seems to be wrong in his overall setup, but the system can nonetheless function as normal, while possibly ignoring some faulty configuration/situation. It can also be used in situations, where a code section is reached, which is not expected by the implementation under normal circumstances (while being able to automatically recover from it).
 - `error` logging should only be used to inform the user that something is tremendously wrong in his setup, the system cannot function normally anymore, and there is a need for immediate action. It should also be used if some code fails irrecoverably and the user should report it as a severe bug.
1. For bindings, you should NOT log errors, if e.g. connections are dropped - this is considered to be an external problem and from a system perspective to be a normal and expected situation. The correct way to inform users about such events is to update the Thing status accordingly. Note that all events (including Thing status events) are anyhow already logged.
1. Likewise, bundles that accept external requests (such as servlets) must not log errors or warnings if incoming requests are incorrect. Instead, appropriate error responses should be returned to the caller.

## Static Code Analysis

The Eclipse SmartHome Maven build includes [tooling for static code analysis](https://github.com/openhab/static-code-analysis) that will validate your code against the coding guidelines and some additional best practices. Information about the checks can be found [here](https://github.com/openhab/static-code-analysis/blob/master/docs/included-checks.md).

The tool will generate an individual report for each bundle that you can find in `path/to/bundle/target/code-analysis/report.html` file and a report for the whole build that contains links to the individual reports in the `target/summary_report.html`.
The tool categorizes the found issues by priority: 1(error),2(warning) or 3(info).
If any error is found within your code the Maven build will end with failure.
You will receive detailed information (path to the file, line and message) listing all problems with Priority 1 on the console.

Please fix all the priority 1 issues and all issues with priority 2 and 3 that are relevant (if you have any doubt don't hesitate to ask).
Re-run the build to confirm that the checks are passing.
