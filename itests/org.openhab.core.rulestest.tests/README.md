# openHAB Integration test - rulestest

This package is meant to hold user-generated test cases, which are not checked in into this repository.
These tests are intended to provide an integration-test-level test coverage for user-defined automation (rules, scripts, ...) based on the intended behaviour of the automation.

## Usage

This package is intended to be a template for en externally hosted/managed project that should (during the build time) be injected into this project.
The externalized project holds the user-defined test cases (e.g. based on JUnit), which are taken into account when the project is build.
As this integration-test is intended to cover user-defined automation rules and scripts, these configuration must be made available during the build/test of this project.
For that, there's a separate `bndrun` file, which should contain the required statements to configure the test context (see the `conf.bndrun-sample` file as an example).
The configuration file needs to have the name `conf.bndrun`.

Afterwards, the tests can be run by maven, e.g. with the following command line argument:
`mvn -am -pl itests/org.openhab.core.rulestest.tests install`
