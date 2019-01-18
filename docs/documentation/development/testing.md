---
layout: documentation
---

{% include base.html %}

Testing Eclipse SmartHome
===
There are two different kinds of approaches for testing Eclipse SmartHome. One is to use JUnit Plug-in tests with simple JUnit test classes. The other is to extend the test class from the `JavaOSGiTest` class and have the full OSGi environment available to test OSGi services and dynamic behaviour. Both approaches are supported through a simple infrastructure, which allows to easily write and execute tests.

Test fragment
---

In general tests are implemented in a separate fragment bundle, which host is the bundle that should be tested. The name of the test fragment bundle should be the same as the bundle to test with a ".test" suffix. The MANIFEST.MF file must contain a `Fragment-Host` entry pointing to the bundle under test. Fragment bundles inherit all imported packages from the host bundle. In addition the fragment bundle must import the `org.junit` package with a minimum version of 4.0.0 specified. The following code snippet shows the MANIFEST.MF file of the test fragment for the `org.eclipse.smarthome.core` bundle. This way all test dependencies are available in the test classes (JUnit, hamcrest, mockito).

    Manifest-Version: 1.0
    Bundle-ManifestVersion: 2
    Bundle-Name: Tests for the Eclipse SmartHome Core
    Bundle-SymbolicName: org.eclipse.smarthome.core.test
    Bundle-Version: 0.11.0.qualifier
    Bundle-Vendor: Eclipse.org/SmartHome
    Fragment-Host: org.eclipse.smarthome.core
    Bundle-RequiredExecutionEnvironment: JavaSE-1.8
    Import-Package: groovy.lang,
     org.codehaus.groovy.reflection,
     org.codehaus.groovy.runtime,
     org.codehaus.groovy.runtime.callsite,
     org.codehaus.groovy.runtime.typehandling,
     org.eclipse.smarthome.core.library.items,
     org.eclipse.smarthome.core.library.types,
     org.eclipse.smarthome.test,
     org.eclipse.smarthome.test.java,
     org.hamcrest;core=split,
     org.junit;version="4.0.0",
     org.junit.runner,
     org.junit.runners,
     org.mockito,
     org.osgi.service.cm

Tests are typically placed inside the folder `src/test/java`. 

Unit tests
---

Each class with a name which ends with `Test`, inside the *test* folder will have all public methods with a `@Test` annotation  automatically executed as a test. Inside the class one can refer to all classes from the host bundle and all imported classes. The following code snippet shows a simple JUnit test which tests the `toString` conversation of a PercentType.

```(java)
public class PercentTypeTest {
    @Test
    public void DoubleValue() {
        PercentType pt = new PercentType("0.0001");
        assertEquals("0.0001", pt.toString());
    }
}
```

Using the [hamcrest matcher library](http://hamcrest.org/JavaHamcrest/) is a good way to write expressive assertions. In contrast to the original assertion statements from JUnit the hamcrest matcher library allows to define the assertion in a more natural order:

```(java)
PercentType pt = new PercentType("0.0001");
assertThat(pt.toString(), is(equalTo("0.0001")));
```

Assertions
---

Here is small example on when to use Hamcrest or JUnit assertions.
In general Hamcrest should be favoured over JUnit as for the more advanced and detailed error output:

```(java)
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
...
@Test
public void assertionsToBeUsed() {
    // use JUnit assertions for very basic checks:
    assertNotNull(new Object());
    assertNull(null);

    boolean booleanValue = true;
    assertTrue(booleanValue); // test boolean values only, no conditions or constraints

    // use Hamcrest assertions for everything else:
    assertThat("myString", is("myString"));
    assertThat("myString", is(instanceOf(String.class)));
    assertThat("myString", containsString("yS"));
    assertThat(Arrays.asList("one", "two"), hasItem("two"));
    assertThat(Arrays.asList("one", "two"), hasSize(2));

    // also valuable for null/boolean checks as the error output is advanced:
    assertThat(null, is(nullValue()));
    assertThat(new Object(), is(not(nullValue())));
    assertThat(true, is(not(false)));
} 
```

OSGi Tests
---

In addition to plain unit tests more advanced OSGi tests are provided.
Those should be used sparingly as the setup is more complex and introduces execution overhead.
Most situations can be tested using mocks (see [Mockito](#mockito)).

In the OSGi context of Eclipse SmartHome test execution must always be run inside an OSGi runtime. Eclipse PDE Plugin allows to run the JUnit test classes as `Plug-in test` but the required bundles have to be selected first:
The most easy way to execute tests in a test fragment is to use the provided launch configuration from the binding test archetype. This may be modified in the `Run Configurations...` menu to match the required bundles/plug-ins.
Another way is to create a test/package/bundle specific lanuch configuration with the following steps:
- Select the test class/package or bundle
- In context menu select `Run as -> JUnit Plug-in Test`
- after test run (which might fail due to unmet dependencies) select the configuration in `Run Configurations...`
- select `plug-ins selected below only` in the `Plug-ins` tab then `Deselect all`
- search for your test fragment bundle and enable it, then clear the search field (important to enable the action buttons again)
- select `Add Required Plug-ins`, then `Apply`, then `Run`
- since the `Add Required Plug-ins` action is a little overeager it will also select other `.test` fragments which you may be required to deselect manually.

From maven one can execute the test with `mvn install` command from the folder of the test fragment bundle.

Mockito
---
In order to keep unit tests as focused as possible we use the mocking framework [https://github.com/mockito/mockito Mockito]. Mockito lets us verify interactions between supporting classes and the unit under test and additionally supports stubbing of method calls for these classes. Please read the very short but expressive introduction on the [http://site.mockito.org/ Mockito homepage] in addition to this small example:

```(java)
public class MyBindingHandlerTest {

    private ThingHandler handler;

    @Mock
    private ThingHandlerCallback callback;

    @Mock
    private Thing thing;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new MyBindingHandler(thing);
        handler.setCallback(callback);
    }
    
    @After
    public void tearDown() {
        // Free any resources, like open database connections, files etc.
        handler.dispose();
    }

    @Test
    public void initializeShouldCallTheCallback() {
        // we expect the handler#initialize method to call the callback during execution and
        // pass it the thing and a ThingStatusInfo object containing the ThingStatus of the thing.
        handler.initialize();

        // verify the interaction with the callback.
        // Check that the ThingStatusInfo given as second parameter to the callback was build with the ONLINE status:
        verify(callback).statusUpdated(eq(thing), argThat(arg -> arg.getStatus().equals(ThingStatus.ONLINE)));
    }

}
```

The code shown above will be created for you once you run the extensions/binding/create_binding_skeleton.[sh|cmd] script. See the OSGi-Tests section for another example.

_Groovy - DEPRECATED_
---
_The use of groovy is deprecated and should not be further extended. The existing groovy tests should be migrated over time. This way we want to reduce complexity in project setup and tooling. The mocking capabilities of groovy will be replaced by the java framework mockito._

OSGi-Tests
---
Some components of Eclipse SmartHome are heavily bound to the OSGi runtime, because they use OSGi core services like the EventAdmin or the ConfigurationAdmin. That makes it hard to test those components outside of the OSGi container. Equinox provides a possibility to execute a JUnit test inside the OSGi environment, where the test has access to OSGi services.

Eclipse SmartHome comes with an abstract base class `JavaOSGiTest` for OSGi tests. The base class sets up a bundle context and has convenience methods for registering mocks as OSGi services and the retrieval of registered OSGi services. Public methods with a @Test annotation will automatically be executed as OSGi tests, as long as the class-name ends with `Test`. The following JUnit/Mockito test class shows how to test the `ItemRegistry` by providing a mocked `ItemProvider`.

```(java)
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemProvider;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.collect.Lists;

public class JavaItemRegistryOSGiTest extends JavaOSGiTest {

    private static String ITEM_NAME = "switchItem";
    private ItemRegistry itemRegistry;

    @Mock
    private ItemProvider itemProvider;

    @Before
    public void setUp() {
        initMocks(this);
        itemRegistry = getService(ItemRegistry.class);
        when(itemProvider.getAll()).thenReturn(Lists.newArrayList(new SwitchItem(ITEM_NAME)));
    }

    @Test
    public void getItemsShouldReturnItemsFromRegisteredItemProvider() {
        assertThat(itemRegistry.getItems(), hasSize(0));

        registerService(itemProvider);

        List<Item> items = new ArrayList<>(itemRegistry.getItems());
        assertThat(items, hasSize(1));
        assertThat(items.get(0).getName(), is(equalTo(ITEM_NAME)));

        unregisterService(itemProvider);

        assertThat(itemRegistry.getItems(), hasSize(0));
    }
}
```

In the `setUp` method all mocks (annotated with @Mock) are created. This is `itemProvider` for this test. Then the `ItemRegistry` OSGi service is retrieved through the method `getService` from the base class `OSGiTest` and assigned to a private variable. Then the `ItemProvider` mock is configured to return a list with one SwitchItem when `itemProvider#getAll` gets called. The test method first checks that the registry delivers no items by default. Afterwards it registers the mocked `ItemProvider` as OSGi service with the method `registerService` and checks if the `ItemRegistry` returns one item now. At the end the mock is unregistered again.

In Eclipse the tests can be executed by right-clicking the test file and clicking on `Run As => JUnit Plug-In Test`. The launch config must be adapted, by selecting the bundle to test under the `Plug-Ins` tab and by clicking on `Add Required Plug-Ins`. Moreover you have to set the Auto-Start option to `true`. If the bundle that should be tested makes use of declarative services (has xml files in OSGI-INF folder), the bundle `org.eclipse.equinox.ds` must also be selected and also the required Plug-Ins of it. The `Validate Plug-Ins` button can be used to check if the launch config is valid. To avoid the manual selection of bundles, one can also choose `all workspace and enabled target plug-ins` with default `Default Auto-Start` set to `true`. The disadvantage is that this will start all bundles, which makes the test execution really slow and will produce a lot of errors on the OSGi console. It is a good practice to store a launch configuration file that launches all test cases for a test fragment.

From maven the test can be executed by calling `mvn integration-test`. For executing the test in maven, tycho calculates the list of depended bundles automatically from package imports. Only if there is no dependency to a bundle, the bundle must be added manually to the test execution environment. For example Eclipse SmartHome makes use of OSGi declarative services. That allows to define service components through XML files. In order to support declarative services in the test environment the according bundle `org.eclipse.equinox.ds` must be added in the pom file within the `tycho-surefire-plugin` configuration section as dependency and furthermore the startlevel has to be defined as shown below. The snippet also shows how to enable `logging` during the test-execution with maven. Therefor you have to add the bundles `ch.qos.logback.classic, ch.qos.logback.core ch.qos.logback.slf4j` as dependency to your tycho-surefire configuration.

    ...
    <build>
     <plugins>
         <plugin>
             <groupId>org.eclipse.tycho</groupId>
             <artifactId>tycho-surefire-plugin</artifactId>
             <version>${tycho-version}</version>
             <configuration>
                 <dependencies>
                     <dependency>
                         <type>eclipse-plugin</type>
                         <artifactId>org.eclipse.equinox.ds</artifactId>
                         <version>0.0.0</version>
                     </dependency>
                     <!-- Required Bundles to enable LOGGING -->
                     <dependency>
                         <type>eclipse-plugin</type>
                         <artifactId>ch.qos.logback.classic</artifactId>
                         <version>0.0.0</version>
                     </dependency>
                     <dependency>
                         <type>eclipse-plugin</type>
                         <artifactId>ch.qos.logback.core</artifactId>
                         <version>0.0.0</version>
                     </dependency>
                     <dependency>
                         <type>eclipse-plugin</type>
                         <artifactId>ch.qos.logback.slf4j</artifactId>
                         <version>0.0.0</version>
                     </dependency>
                 </dependencies>
                 <bundleStartLevel>
                     <bundle>
                         <id>org.eclipse.equinox.ds</id>
                         <level>1</level>
                         <autoStart>true</autoStart>
                     </bundle>
                 </bundleStartLevel>
             </configuration>
         </plugin>
     </plugins>
    </build>
    ...
    
In the dependency definition the `artifactId` is the name of the required bundle, where the version can always be `0.0.0`. Within the `bundleStartLevel` definition the start level and auto start of the depended bundles can be configured. The `org.eclipse.equinox.ds` bundle must have level 1 and must be started automatically.

Common errors
---

### Failed to execute goal org.eclipse.tycho:tycho-surefire-plugin:XXX:test (default-test) on project XXX: No tests found.

Maven might report this error when building your project, it means that the surefire plugin cannot find any tests to execute, please check the following details:

* Did you add any test classes with a class-name which ends with `Test` (singular)
* Did you annotate any methods with `@Test`
