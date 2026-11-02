# openHAB Core

[![GitHub Actions Build Status](https://github.com/openhab/openhab-core/actions/workflows/ci-build.yml/badge.svg?branch=main)](https://github.com/openhab/openhab-core/actions/workflows/ci-build.yml)
[![Jenkins Build Status](https://ci.openhab.org/job/openHAB-Core/badge/icon)](https://ci.openhab.org/job/openHAB-Core/)
[![JavaDoc Build Status](https://ci.openhab.org/view/Documentation/job/openHAB-JavaDoc/badge/icon?subject=javadoc)](https://ci.openhab.org/view/Documentation/job/openHAB-JavaDoc/)
[![EPL-2.0](https://img.shields.io/badge/license-EPL%202-green.svg)](https://opensource.org/licenses/EPL-2.0)
[![Crowdin](https://badges.crowdin.net/openhab-core/localized.svg)](https://crowdin.com/project/openhab-core)

This project contains core bundles of the openHAB runtime.

Building and running the project is fairly easy if you follow the steps detailed below.

Please note that openHAB Core is not a product itself, but a framework to build solutions on top.
It is picked up by the main [openHAB distribution](https://github.com/openhab/openhab-distro).

This means that what you build is primarily an artifact repository of OSGi bundles that can be used within smart home products.

## Development / Repository Organization

The build is based on Maven and produces OSGi bundles, dependency BOMs (Bills of Materials), and Karaf feature definitions.
The main directories are organized as follows:

```text
.
+-- bom/         Dependency management and bundle indexes for core and downstream builds
|   +-- compile/            Compile-time dependencies
|   +-- openhab-core/       Core bundle dependencies
|   +-- openhab-core-index/ Core bundle indexes for OSGi integration tests
|   +-- ...
|
+-- bundles/     Runtime bundles, public APIs, implementations, and unit tests
|   +-- org.openhab.core
|   +-- org.openhab.core.thing
|   +-- ...
|
+-- features/    Karaf feature definitions for core and third-party bundles
|
+-- itests/      OSGi integration tests and their bndrun configurations
|   +-- org.openhab.core.tests
|   +-- org.openhab.core.thing.tests
|   +-- ...
|
+-- tools/       Development tools, upgrade tools, and static analysis configuration
|   +-- archetype/          Add-on project templates
|   +-- i18n-plugin/        Translation generation tooling
|   +-- upgradetool/        Configuration and database upgrade tooling
|   +-- static-code-analysis/
|
+-- licenses/    License headers and configuration for automated checks
+-- CODEOWNERS   Assigns pull requests to the core maintainers
```

Unit tests normally live in the affected bundle's `src/test/java/` directory.
Integration tests that require an OSGi runtime live in separate modules under `itests/`.

## Prerequisites

What you need before you start:

- Java SE Development Kit 21
- Maven 3 (version 3.9.6 or newer) from <https://maven.apache.org/download.html>

Make sure that the `mvn` command is available on your path.
Alternatively, use the included Maven Wrapper by replacing `mvn` with `./mvnw` on Linux or macOS, or `mvnw.cmd` on Windows, in the commands below.

## Checkout

Checkout the source code from GitHub, e.g. by running:

```shell
git clone https://github.com/openhab/openhab-core.git
cd openhab-core
```

## Building with Maven

Run the following commands from the repository root.
Set `MAVEN_OPTS` to `-Xms512m -Xmx1024m` before building, as appropriate for your shell.

### Command line build

To compile and package all sources, run the unit and integration tests, and install the artifacts into your local Maven repository:

```shell
mvn clean install
```

The build generates a `.jar` file per bundle in that bundle's `target/` directory.

For a change to a single bundle, use `-pl :<artifactId>` to select the module by its Maven artifact ID and `-am` to also build its reactor dependencies.
For example, to build the Thing bundle and run its unit tests:

```shell
mvn clean install -pl :org.openhab.core.thing -am
```

This does not select the separate OSGi integration-test module; see [Integration Tests](#integration-tests) below.

### Build options

The following options can be useful during development:

| Option                        | Description                                             |
| ----------------------------- | ------------------------------------------------------- |
| `-DskipChecks`                | Skip static analysis (Checkstyle, PMD, and SpotBugs)    |
| `-DskipTests`                 | Skip test execution                                     |
| `-Dmaven.test.skip=true`      | Skip the compilation and execution of tests             |
| `-Dfeatures.verify.skip=true` | Skip Karaf feature verification                         |
| `-Dspotless.check.skip=true`  | Skip Spotless formatting checks                         |
| `-am`                         | Also build reactor dependencies of the selected modules |
| `-o`                          | Work offline using artifacts already available locally  |
| `-T 1C`                       | Build in parallel, using one thread per CPU core        |
| `-pl :<artifactId>`           | Select a module by its Maven artifact ID                |

For example, to skip static analysis and tests while working on the Thing bundle:

```shell
mvn clean install -pl :org.openhab.core.thing -am -DskipChecks -DskipTests
```

For a faster full build that also skips formatting checks:

```shell
mvn clean install -T 1C -DskipChecks -DskipTests -Dspotless.check.skip=true
```

Skipping checks or tests can hide problems, so run the relevant validation before submitting a change.
Parallel builds increase system load and can affect timing-sensitive tests.

### Translations

Core translations are managed through [Crowdin](https://crowdin.com/project/openhab-core).
English source strings are maintained in this repository and synchronized to Crowdin; locale-specific translations are synchronized back to the repository.
Add or correct translations in Crowdin rather than editing locale-specific properties files here, as those edits can be overwritten by synchronization.

When adding or changing user-visible text, update the English source properties and any related metadata in the affected bundle.
Source files typically live in `src/main/resources/OH-INF/i18n/`, with some bundles also using properties files directly under `src/main/resources/`.
See [crowdin.yml](crowdin.yml) for the source and translation file patterns.

For bundles with supported configuration, Thing, or automation metadata in `OH-INF`, the i18n plugin can generate the corresponding English labels and descriptions.
For example, to update the audio bundle's `audio.properties` from its configuration metadata:

```shell
mvn i18n:generate-default-translations -pl :org.openhab.core.audio
```

If Maven cannot resolve the `i18n` prefix, use the fully qualified plugin goal; the version is managed by the repository POM:

```shell
mvn org.openhab.core.tools:i18n-maven-plugin:generate-default-translations -pl :org.openhab.core.audio
```

The generator does not extract arbitrary runtime messages from Java code.
Maintain those English strings in the existing source properties files, and review generated changes before committing them.

### Code quality

To check formatting for the bundle you are working on without changing files:

```shell
mvn spotless:check -pl :org.openhab.core.thing
```

To apply the repository formatter, including import organization:

```shell
mvn spotless:apply -pl :org.openhab.core.thing
```

Omit `-pl` to check or format all modules.
Formatting checks and static analysis also run as part of the normal build.

### Integration Tests

To run the Thing OSGi integration tests and build their reactor dependencies:

```shell
mvn clean install -pl :org.openhab.core.thing.tests -am
```

Integration tests use shared core BOMs and bundle indexes, so `-am` can select a substantial part of core even when only one test module is requested.

When Maven dependencies or OSGi bundle requirements change, the run bundles in the integration tests' `itest.bndrun` files may need updating.
To resolve those dependencies and run the full build:

```shell
mvn clean install -DwithResolver
```

Review any changes to the `*.bndrun` files produced by the resolver.

## How to contribute

If you want to become a contributor to the project, please read about [contributing](https://www.openhab.org/docs/developer/contributing.html) and check our [guidelines](https://www.openhab.org/docs/developer/guidelines.html) first.
