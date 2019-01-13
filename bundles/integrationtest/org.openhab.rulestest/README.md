# openHAB Integration test - rulestest

The `rulestest` package provides a base test class, which can be used to write tests against openHAB rules.
This plugin should be used in a separate fragment (where this plugin is the Fragment-Host) in order to separate the framework functions of the tests from the tests itself.
See the Usage section for more details.

## Usage

In order to separate the tests from the framework features that provides a way to test rules, you should fork or clone this repository and import this plugin into your IDE.
Afterwards, create a new fragment (e.g. with the name `org.openhab.ruletest.test`) where this plugin is the host.
You can then start to write tests in the `org.openhab.ruletest` java package inside of the `src/test/java` source folder.
See the already existing `org.openhab.ruletest.test` fragment as an example.
