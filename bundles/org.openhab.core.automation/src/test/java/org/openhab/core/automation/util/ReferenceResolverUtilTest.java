/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.automation.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Module;
import org.openhab.core.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vasil Ilchev - Initial contribution
 */
@NonNullByDefault
public class ReferenceResolverUtilTest {
    private static final String CONTEXT_PROPERTY1 = "contextProperty1";
    private static final String CONTEXT_PROPERTY2 = "contextProperty2";
    private static final String CONTEXT_PROPERTY3 = "contextProperty3";
    private static final String CONTEXT_PROPERTY4 = "contextProperty4";

    private static final Map<String, Object> CONTEXT = new HashMap<>();
    private static final Map<String, Object> MODULE_CONFIGURATION = new HashMap<>();
    private static final Map<String, @Nullable Object> EXPECTED_MODULE_CONFIGURATION = new HashMap<>();
    private static final Map<String, String> COMPOSITE_CHILD_MODULE_INPUTS_REFERENCES = new HashMap<>();
    private static final Map<String, @Nullable Object> EXPECTED_COMPOSITE_CHILD_MODULE_CONTEXT = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(ReferenceResolverUtilTest.class);

    static {
        // context from where references will be taken
        CONTEXT.put(CONTEXT_PROPERTY1, "value1");
        CONTEXT.put(CONTEXT_PROPERTY2, "value2");
        CONTEXT.put(CONTEXT_PROPERTY3, "value3");
        CONTEXT.put(CONTEXT_PROPERTY4, new BigDecimal(12345));

        // module configuration with references
        MODULE_CONFIGURATION.put("simpleReference", String.format("{{%s}}", CONTEXT_PROPERTY4));
        MODULE_CONFIGURATION.put("complexReference",
                String.format("Hello {{%s}} {{%s}}", CONTEXT_PROPERTY1, CONTEXT_PROPERTY4));
        MODULE_CONFIGURATION.put("complexReferenceWithMissing",
                String.format("Testing {{UNKNOWN}}, {{%s}}", CONTEXT_PROPERTY4));
        MODULE_CONFIGURATION.put("complexReferenceArray",
                String.format("[{{%s}}, {{%s}}, staticText]", CONTEXT_PROPERTY2, CONTEXT_PROPERTY3));
        MODULE_CONFIGURATION.put("complexReferenceArrayWithMissing",
                String.format("[{{UNKNOWN}}, {{%s}}, staticText]", CONTEXT_PROPERTY3));
        MODULE_CONFIGURATION.put("complexReferenceObj",
                String.format("{key1: {{%s}}, key2: staticText, key3: {{%s}}}", CONTEXT_PROPERTY1, CONTEXT_PROPERTY4));
        MODULE_CONFIGURATION.put("complexReferenceObjWithMissing",
                String.format("{key1: {{UNKNOWN}}, key2: {{%s}}, key3: {{UNKNOWN2}}}", CONTEXT_PROPERTY2));

        // expected resolved module configuration
        EXPECTED_MODULE_CONFIGURATION.put("simpleReference", CONTEXT.get(CONTEXT_PROPERTY4));
        EXPECTED_MODULE_CONFIGURATION.put("complexReference",
                String.format("Hello %s %s", CONTEXT.get(CONTEXT_PROPERTY1), CONTEXT.get(CONTEXT_PROPERTY4)));
        EXPECTED_MODULE_CONFIGURATION.put("complexReferenceWithMissing",
                String.format("Testing {{UNKNOWN}}, %s", CONTEXT.get(CONTEXT_PROPERTY4)));
        EXPECTED_MODULE_CONFIGURATION.put("complexReferenceArray",
                String.format("[%s, %s, staticText]", CONTEXT.get(CONTEXT_PROPERTY2), CONTEXT.get(CONTEXT_PROPERTY3)));
        EXPECTED_MODULE_CONFIGURATION.put("complexReferenceArrayWithMissing",
                String.format("[{{UNKNOWN}}, %s, staticText]", CONTEXT.get(CONTEXT_PROPERTY3)));
        EXPECTED_MODULE_CONFIGURATION.put("complexReferenceObj", String.format("{key1: %s, key2: staticText, key3: %s}",
                CONTEXT.get(CONTEXT_PROPERTY1), CONTEXT.get(CONTEXT_PROPERTY4)));
        EXPECTED_MODULE_CONFIGURATION.put("complexReferenceObjWithMissing",
                String.format("{key1: {{UNKNOWN}}, key2: %s, key3: {{UNKNOWN2}}}", CONTEXT.get(CONTEXT_PROPERTY2)));

        // composite child module input with references
        COMPOSITE_CHILD_MODULE_INPUTS_REFERENCES.put("moduleInput", String.format("{{%s}}", CONTEXT_PROPERTY1));
        COMPOSITE_CHILD_MODULE_INPUTS_REFERENCES.put("moduleInputMissing", "{{UNKNOWN}}");
        COMPOSITE_CHILD_MODULE_INPUTS_REFERENCES.put("moduleInput2", String.format("{{%s}}", CONTEXT_PROPERTY2));
        // expected resolved child module context
        EXPECTED_COMPOSITE_CHILD_MODULE_CONTEXT.put("moduleInput", CONTEXT.get(CONTEXT_PROPERTY1));
        EXPECTED_COMPOSITE_CHILD_MODULE_CONTEXT.put("moduleInputMissing", CONTEXT.get("UNKNOWN"));
        EXPECTED_COMPOSITE_CHILD_MODULE_CONTEXT.put("moduleInput2", CONTEXT.get(CONTEXT_PROPERTY2));
    }

    @Test
    public void testModuleConfigurationResolving() {
        // test trigger configuration.
        Module trigger = ModuleBuilder.createTrigger().withId("id1").withTypeUID("typeUID1")
                .withConfiguration(new Configuration(MODULE_CONFIGURATION)).build();
        ReferenceResolver.updateConfiguration(trigger.getConfiguration(), CONTEXT, logger);
        assertEquals(trigger.getConfiguration(), new Configuration(EXPECTED_MODULE_CONFIGURATION));
        // test condition configuration.
        Module condition = ModuleBuilder.createCondition().withId("id2").withTypeUID("typeUID2")
                .withConfiguration(new Configuration(MODULE_CONFIGURATION)).build();
        ReferenceResolver.updateConfiguration(condition.getConfiguration(), CONTEXT, logger);
        assertEquals(condition.getConfiguration(), new Configuration(EXPECTED_MODULE_CONFIGURATION));
        // test action configuration.
        Module action = ModuleBuilder.createAction().withId("id3").withTypeUID("typeUID3")
                .withConfiguration(new Configuration(MODULE_CONFIGURATION)).build();
        ReferenceResolver.updateConfiguration(action.getConfiguration(), CONTEXT, logger);
        assertEquals(action.getConfiguration(), new Configuration(EXPECTED_MODULE_CONFIGURATION));
    }

    @Test
    public void testModuleInputResolving() {
        // test Composite child ModuleImpl(condition) context
        Module condition = ModuleBuilder.createCondition().withId("id1").withTypeUID("typeUID1")
                .withInputs(COMPOSITE_CHILD_MODULE_INPUTS_REFERENCES).build();
        Map<String, Object> conditionContext = ReferenceResolver.getCompositeChildContext(condition, CONTEXT);
        assertEquals(conditionContext, EXPECTED_COMPOSITE_CHILD_MODULE_CONTEXT);
        // test Composite child ModuleImpl(action) context
        Module action = ModuleBuilder.createAction().withId("id2").withTypeUID("typeUID2")
                .withInputs(COMPOSITE_CHILD_MODULE_INPUTS_REFERENCES).build();
        assertEquals(EXPECTED_COMPOSITE_CHILD_MODULE_CONTEXT, conditionContext);
        Map<String, Object> actionContext = ReferenceResolver.getCompositeChildContext(action, CONTEXT);
        assertEquals(actionContext, EXPECTED_COMPOSITE_CHILD_MODULE_CONTEXT);
    }

    @Test
    public void testSplitReferenceToTokens() {
        assertNull(ReferenceResolver.splitReferenceToTokens(null));
        assertTrue(ReferenceResolver.splitReferenceToTokens("").length == 0);
        final String[] referenceTokens = ReferenceResolver
                .splitReferenceToTokens(".module.array[\".na[m}.\"e\"][1].values1");
        assertTrue("module".equals(referenceTokens[0]));
        assertTrue("array".equals(referenceTokens[1]));
        assertTrue(".na[m}.\"e".equals(referenceTokens[2]));
        assertTrue("1".equals(referenceTokens[3]));
        assertTrue("values1".equals(referenceTokens[4]));
    }

    @Test
    public void testResolvingFromNull() {
        String ken = "Ken";
        assertEquals(ken,
                ReferenceResolver.resolveComplexDataReference(ken, ReferenceResolver.splitReferenceToTokens(null)));
    }

    @Test
    public void testResolvingFromEmptyString() {
        String ken = "Ken";
        assertEquals(ken,
                ReferenceResolver.resolveComplexDataReference(ken, ReferenceResolver.splitReferenceToTokens("")));
    }

    @Test
    public void testGetFromList() {
        String ken = "Ken";
        List<String> names = List.of("John", ken, "Sue");
        assertEquals(ken,
                ReferenceResolver.resolveComplexDataReference(names, ReferenceResolver.splitReferenceToTokens("[1]")));
    }

    @Test
    public void testGetFromListInvalidIndexFormat() {
        List<String> names = List.of("John", "Ken", "Sue");
        assertThrows(NumberFormatException.class, () -> ReferenceResolver.resolveComplexDataReference(names,
                ReferenceResolver.splitReferenceToTokens("[Ten]")));
    }

    @Test
    public void getFromMap() {
        String phone = "0331 1387 121";
        Map<String, String> phones = Map.of("John", phone, "Sue", "0222 2184 121", "Mark", "0222 5641 121");
        assertEquals(phone, ReferenceResolver.resolveComplexDataReference(phones,
                ReferenceResolver.splitReferenceToTokens("[\"John\"]")));
    }

    @Test
    public void getFromMapWithKeyThatContainsSpecialCharacters() {
        String phone = "0331 1387 121";
        Map<String, String> phones = Map.of("John[].Smi\"th].", phone, "Sue", "0222 2184 121", "Mark", "0222 5641 121");
        assertEquals(phone, ReferenceResolver.resolveComplexDataReference(phones,
                ReferenceResolver.splitReferenceToTokens("[\"John[].Smi\"th].\"]")));
    }

    @Test
    public void getFromMapUnExistingKey() {
        Map<String, String> phones = Map.of("Sue", "0222 2184 121", "Mark", "0222 5641 121");
        assertNull(ReferenceResolver.resolveComplexDataReference(phones,
                ReferenceResolver.splitReferenceToTokens("[\"John\"]")));
    }

    @Test
    public void getFromList() {
        String ken = "Ken";
        List<String> names = List.of("John", ken, "Sue");
        assertEquals(ken,
                ReferenceResolver.resolveComplexDataReference(names, ReferenceResolver.splitReferenceToTokens("[1]")));
    }

    @Test
    public void testGetFromListInvalidIndex() {
        List<String> names = List.of("John", "Ken", "Sue");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> ReferenceResolver.resolveComplexDataReference(names,
                ReferenceResolver.splitReferenceToTokens("[10]")));
    }

    @Test
    public void testGetFromInvalidIndexFormat() {
        List<String> names = List.of("John", "Ken", "Sue");
        assertThrows(NumberFormatException.class, () -> ReferenceResolver.resolveComplexDataReference(names,
                ReferenceResolver.splitReferenceToTokens("[Ten]")));
    }

    @Test
    public void testGetFromBean() {
        String name = "John";
        B1<String> b3 = new B1<>(name);
        assertEquals(name,
                ReferenceResolver.resolveComplexDataReference(b3, ReferenceResolver.splitReferenceToTokens("value")));
    }

    @Test
    public void testGetFromBeanWithPrivateField() {
        String name = "John";
        B2<String> b4 = new B2<>(name);
        assertEquals(name,
                ReferenceResolver.resolveComplexDataReference(b4, ReferenceResolver.splitReferenceToTokens("value")));
    }

    @Test
    public void testBeanFromBean() {
        String phone = "0331 1387 121";
        Map<String, String> phones = Map.of("John", phone);
        B1<Map<String, String>> b3 = new B1<>(phones);
        B2<B1<Map<String, String>>> b4 = new B2<>(b3);
        assertEquals(phone, ReferenceResolver.resolveComplexDataReference(b4,
                ReferenceResolver.splitReferenceToTokens("value.value[\"John\"]")));
    }

    @Test()
    public void testGetBeanFieldFromList() {
        String name = "John";
        B1<String> b31 = new B1<>("Ken");
        B1<String> b32 = new B1<>("Sue");
        B1<String> b33 = new B1<>(name);
        List<B1<String>> b = List.of(b31, b32, b33);
        assertEquals(name, ReferenceResolver.resolveComplexDataReference(b,
                ReferenceResolver.splitReferenceToTokens("[2].value")));
    }

    public class B1<T> {
        @SuppressWarnings("unused")
        private final T value;

        public B1(T value) {
            this.value = value;
        }
    }

    public class B2<T> {
        public T value;

        public B2(T value) {
            this.value = value;
        }
    }
}
