/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.core.internal;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.core.util.ModuleBuilder;
import org.eclipse.smarthome.automation.core.util.ReferenceResolver;
import org.eclipse.smarthome.config.core.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceResolverUtilTest {
    private static final String CONTEXT_PROPERTY1 = "contextProperty1";
    private static final String CONTEXT_PROPERTY2 = "contextProperty2";
    private static final String CONTEXT_PROPERTY3 = "contextProperty3";
    private static final String CONTEXT_PROPERTY4 = "contextProperty4";

    private static final Map<String, Object> context = new HashMap<>();
    private static final Map<String, Object> moduleConfiguration = new HashMap<>();
    private static final Map<String, Object> expectedModuleConfiguration = new HashMap<>();
    private static final Map<String, String> compositeChildModuleInputsReferences = new HashMap<>();
    private static final Map<String, Object> expectedCompositeChildModuleContext = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    static {
        // context from where references will be taken
        context.put(CONTEXT_PROPERTY1, "value1");
        context.put(CONTEXT_PROPERTY2, "value2");
        context.put(CONTEXT_PROPERTY3, "value3");
        context.put(CONTEXT_PROPERTY4, new BigDecimal(12345));

        // module configuration with references
        moduleConfiguration.put("simpleReference", String.format("${%s}", CONTEXT_PROPERTY4));
        moduleConfiguration.put("complexReference",
                String.format("Hello ${%s} ${%s}", CONTEXT_PROPERTY1, CONTEXT_PROPERTY4));
        moduleConfiguration.put("complexReferenceWithMissing",
                String.format("Testing ${UNKNOWN}, ${%s}", CONTEXT_PROPERTY4));
        moduleConfiguration.put("complexReferenceArray",
                String.format("[${%s}, ${%s}, staticText]", CONTEXT_PROPERTY2, CONTEXT_PROPERTY3));
        moduleConfiguration.put("complexReferenceArrayWithMissing",
                String.format("[${UNKNOWN}, ${%s}, staticText]", CONTEXT_PROPERTY3));
        moduleConfiguration.put("complexReferenceObj",
                String.format("{key1: ${%s}, key2: staticText, key3: ${%s}}", CONTEXT_PROPERTY1, CONTEXT_PROPERTY4));
        moduleConfiguration.put("complexReferenceObjWithMissing",
                String.format("{key1: ${UNKNOWN}, key2: ${%s}, key3: ${UNKNOWN2}}", CONTEXT_PROPERTY2));

        // expected resolved module configuration
        expectedModuleConfiguration.put("simpleReference", context.get(CONTEXT_PROPERTY4));
        expectedModuleConfiguration.put("complexReference",
                String.format("Hello %s %s", context.get(CONTEXT_PROPERTY1), context.get(CONTEXT_PROPERTY4)));
        expectedModuleConfiguration.put("complexReferenceWithMissing",
                String.format("Testing ${UNKNOWN}, %s", context.get(CONTEXT_PROPERTY4)));
        expectedModuleConfiguration.put("complexReferenceArray",
                String.format("[%s, %s, staticText]", context.get(CONTEXT_PROPERTY2), context.get(CONTEXT_PROPERTY3)));
        expectedModuleConfiguration.put("complexReferenceArrayWithMissing",
                String.format("[${UNKNOWN}, %s, staticText]", context.get(CONTEXT_PROPERTY3)));
        expectedModuleConfiguration.put("complexReferenceObj", String.format("{key1: %s, key2: staticText, key3: %s}",
                context.get(CONTEXT_PROPERTY1), context.get(CONTEXT_PROPERTY4)));
        expectedModuleConfiguration.put("complexReferenceObjWithMissing",
                String.format("{key1: ${UNKNOWN}, key2: %s, key3: ${UNKNOWN2}}", context.get(CONTEXT_PROPERTY2)));

        // composite child module input with references
        compositeChildModuleInputsReferences.put("moduleInput", String.format("${%s}", CONTEXT_PROPERTY1));
        compositeChildModuleInputsReferences.put("moduleInputMissing", "${UNKNOWN}");
        compositeChildModuleInputsReferences.put("moduleInput2", String.format("${%s}", CONTEXT_PROPERTY2));
        // expected resolved child module context
        expectedCompositeChildModuleContext.put("moduleInput", context.get(CONTEXT_PROPERTY1));
        expectedCompositeChildModuleContext.put("moduleInputMissing", context.get("UNKNOWN"));
        expectedCompositeChildModuleContext.put("moduleInput2", context.get(CONTEXT_PROPERTY2));
    }

    Logger log = LoggerFactory.getLogger(ReferenceResolverUtilTest.class);

    @Test
    public void testModuleConfigurationResolving() {
        // test trigger configuration.
        Module trigger = ModuleBuilder.createTrigger().withId("id1").withTypeUID("typeUID1")
                .withConfiguration(new Configuration(moduleConfiguration)).build();
        ReferenceResolver.updateConfiguration(trigger.getConfiguration(), context, logger);
        Assert.assertEquals(trigger.getConfiguration(), new Configuration(expectedModuleConfiguration));
        // test condition configuration.
        Module condition = ModuleBuilder.createCondition().withId("id2").withTypeUID("typeUID2")
                .withConfiguration(new Configuration(moduleConfiguration)).build();
        ReferenceResolver.updateConfiguration(condition.getConfiguration(), context, logger);
        Assert.assertEquals(condition.getConfiguration(), new Configuration(expectedModuleConfiguration));
        // test action configuration.
        Module action = ModuleBuilder.createAction().withId("id3").withTypeUID("typeUID3")
                .withConfiguration(new Configuration(moduleConfiguration)).build();
        ReferenceResolver.updateConfiguration(action.getConfiguration(), context, logger);
        Assert.assertEquals(action.getConfiguration(), new Configuration(expectedModuleConfiguration));
    }

    @Test
    public void testModuleInputResolving() {
        // test Composite child ModuleImpl(condition) context
        Module condition = ModuleBuilder.createCondition().withId("id1").withTypeUID("typeUID1")
                .withInputs(compositeChildModuleInputsReferences).build();
        Map<String, Object> conditionContext = ReferenceResolver.getCompositeChildContext(condition, context);
        Assert.assertEquals(conditionContext, expectedCompositeChildModuleContext);
        // test Composite child ModuleImpl(action) context
        Module action = ModuleBuilder.createAction().withId("id2").withTypeUID("typeUID2")
                .withInputs(compositeChildModuleInputsReferences).build();
        Assert.assertEquals(expectedCompositeChildModuleContext, conditionContext);
        Map<String, Object> actionContext = ReferenceResolver.getCompositeChildContext(action, context);
        Assert.assertEquals(actionContext, expectedCompositeChildModuleContext);
    }

    @Test
    public void testSplitReferenceToTokens() {
        Assert.assertNull(ReferenceResolver.splitReferenceToTokens(null));
        Assert.assertTrue(ReferenceResolver.splitReferenceToTokens("").length == 0);
        final String[] referenceTokens = ReferenceResolver
                .splitReferenceToTokens(".module.array[\".na[m}.\"e\"][1].values1");
        Assert.assertTrue(referenceTokens[0].equals("module"));
        Assert.assertTrue(referenceTokens[1].equals("array"));
        Assert.assertTrue(referenceTokens[2].equals(".na[m}.\"e"));
        Assert.assertTrue(referenceTokens[3].equals("1"));
        Assert.assertTrue(referenceTokens[4].equals("values1"));
    }

    @Test
    public void testResolvingFromNull() {
        String ken = "Ken";
        Assert.assertEquals(ken,
                ReferenceResolver.resolveComplexDataReference(ken, ReferenceResolver.splitReferenceToTokens(null)));
    }

    @Test
    public void testResolvingFromEmptyString() {
        String ken = "Ken";
        Assert.assertEquals(ken,
                ReferenceResolver.resolveComplexDataReference(ken, ReferenceResolver.splitReferenceToTokens("")));
    }

    @Test
    public void testGetFromList() {
        String ken = "Ken";
        List<String> names = Arrays.asList("John", ken, "Sue");
        Assert.assertEquals(ken,
                ReferenceResolver.resolveComplexDataReference(names, ReferenceResolver.splitReferenceToTokens("[1]")));
    }

    @Test(expected = NumberFormatException.class)
    public void testGetFromListInvalidIndexFormat() {
        List<String> names = Arrays.asList("John", "Ken", "Sue");
        ReferenceResolver.resolveComplexDataReference(names, ReferenceResolver.splitReferenceToTokens("[Ten]"));
    }

    @Test
    public void getFromMap() {
        String phone = "0331 1387 121";
        Map<String, String> phones = new HashMap<>();
        phones.put("John", phone);
        phones.put("Sue", "0222 2184 121");
        phones.put("Mark", "0222 5641 121");
        Assert.assertEquals(phone, ReferenceResolver.resolveComplexDataReference(phones,
                ReferenceResolver.splitReferenceToTokens("[\"John\"]")));
    }

    @Test
    public void getFromMapWithKeyThatContainsSpecialCharacters() {
        String phone = "0331 1387 121";
        Map<String, String> phones = new HashMap<>();
        phones.put("John[].Smi\"th].", phone);
        phones.put("Sue", "0222 2184 121");
        phones.put("Mark", "0222 5641 121");
        Assert.assertEquals(phone, ReferenceResolver.resolveComplexDataReference(phones,
                ReferenceResolver.splitReferenceToTokens("[\"John[].Smi\"th].\"]")));
    }

    @Test
    public void getFromMapUnExistingKey() {
        Map<String, String> phones = new HashMap<>();
        phones.put("Sue", "0222 2184 121");
        phones.put("Mark", "0222 5641 121");
        Assert.assertNull(ReferenceResolver.resolveComplexDataReference(phones,
                ReferenceResolver.splitReferenceToTokens("[\"John\"]")));
    }

    @Test
    public void getFromList() {
        String ken = "Ken";
        List<String> names = Arrays.asList(new String[] { "John", ken, "Sue" });
        Assert.assertEquals(ken,
                ReferenceResolver.resolveComplexDataReference(names, ReferenceResolver.splitReferenceToTokens("[1]")));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testGetFromListInvalidIndex() {
        List<String> names = Arrays.asList(new String[] { "John", "Ken", "Sue" });
        ReferenceResolver.resolveComplexDataReference(names, ReferenceResolver.splitReferenceToTokens("[10]"));
    }

    @Test(expected = NumberFormatException.class)
    public void testGetFromInvalidIndexFormat() {
        List<String> names = Arrays.asList(new String[] { "John", "Ken", "Sue" });
        ReferenceResolver.resolveComplexDataReference(names, ReferenceResolver.splitReferenceToTokens("[Ten]"));
    }

    @Test
    public void testGetFromBean() {
        String name = "John";
        B1<String> b3 = new B1<>(name);
        Assert.assertEquals(name,
                ReferenceResolver.resolveComplexDataReference(b3, ReferenceResolver.splitReferenceToTokens("value")));
    }

    @Test
    public void testGetFromBeanWithPrivateField() {
        String name = "John";
        B2<String> b4 = new B2<>(name);
        Assert.assertEquals(name,
                ReferenceResolver.resolveComplexDataReference(b4, ReferenceResolver.splitReferenceToTokens("value")));
    }

    @Test
    public void testBeanFromBean() {
        String phone = "0331 1387 121";
        Map<String, String> phones = new HashMap<>();
        phones.put("John", phone);
        B1<Map<String, String>> b3 = new B1<>(phones);
        B2<B1<Map<String, String>>> b4 = new B2<>(b3);
        Assert.assertEquals(phone, ReferenceResolver.resolveComplexDataReference(b4,
                ReferenceResolver.splitReferenceToTokens("value.value[\"John\"]")));
    }

    @Test()
    public void testGetBeanFieldFromList() {
        String name = "John";
        B1<String> b31 = new B1<>("Ken");
        B1<String> b32 = new B1<>("Sue");
        B1<String> b33 = new B1<>(name);
        List<B1<String>> b = Arrays.asList(b31, b32, b33);
        Assert.assertEquals(name, ReferenceResolver.resolveComplexDataReference(b,
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
