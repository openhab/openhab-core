/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module.provider;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.automation.AnnotatedActions;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.ActionScope;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.automation.module.provider.AnnotationActionModuleTypeHelper;
import org.openhab.core.automation.module.provider.i18n.ModuleTypeI18nService;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.Output;
import org.openhab.core.config.core.ConfigConstants;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.test.java.JavaTest;

/**
 * Tests for the {@link AnnotatedActionModuleTypeProvider}
 *
 * @author Stefan Triller - Initial contribution
 */
public class AnnotationActionModuleTypeProviderTest extends JavaTest {

    private static final String TEST_ACTION_TYPE_ID = "binding.test.testMethod";
    private static final String ACTION_LABEL = "Test Label";
    private static final String ACTION_DESCRIPTION = "My Description";

    private static final String ACTION_INPUT1 = "input1";
    private static final String ACTION_INPUT1_DESCRIPTION = "input1 description";
    private static final String ACTION_INPUT1_LABEL = "input1 label";
    private static final String ACTION_INPUT1_DEFAULT_VALUE = "input1 default";
    private static final String ACTION_INPUT1_REFERENCE = "input1 reference";
    private static final String ACTION_INPUT2 = "input2";
    private static final String ACTION_OUTPUT1 = "output1";
    private static final String ACTION_OUTPUT1_DESCRIPTION = "output1 description";
    private static final String ACTION_OUTPUT1_LABEL = "output1 label";
    private static final String ACTION_OUTPUT1_DEFAULT_VALUE = "output1 default";
    private static final String ACTION_OUTPUT1_REFERENCE = "output1 reference";
    private static final String ACTION_OUTPUT1_TYPE = "java.lang.Integer";
    private static final String ACTION_OUTPUT2 = "output2";
    private static final String ACTION_OUTPUT2_TYPE = "java.lang.String";

    private ModuleTypeI18nService moduleTypeI18nService;

    private AnnotatedActions actionProviderConf1;
    private AnnotatedActions actionProviderConf2;

    @Before
    public void setUp() {
        actionProviderConf1 = new TestActionProvider();
        actionProviderConf2 = new TestActionProvider();

        moduleTypeI18nService = mock(ModuleTypeI18nService.class);
        when(moduleTypeI18nService.getModuleTypePerLocale(any(ModuleType.class), any(), any()))
                .thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    public void testMultiServiceAnnotationActions() {
        AnnotatedActionModuleTypeProvider prov = new AnnotatedActionModuleTypeProvider(moduleTypeI18nService);

        Map<String, Object> properties1 = new HashMap<>();
        properties1.put(ConfigConstants.SERVICE_CONTEXT, "conf1");
        prov.addActionProvider(actionProviderConf1, properties1);

        Collection<String> types = prov.getTypes();
        assertEquals(1, types.size());
        assertTrue(types.contains(TEST_ACTION_TYPE_ID));

        Map<String, Object> properties2 = new HashMap<>();
        properties2.put(ConfigConstants.SERVICE_CONTEXT, "conf2");
        prov.addActionProvider(actionProviderConf2, properties2);

        // we only have ONE type but TWO configurations for it
        types = prov.getTypes();
        assertEquals(1, types.size());
        assertTrue(types.contains(TEST_ACTION_TYPE_ID));

        ModuleType mt = prov.getModuleType(TEST_ACTION_TYPE_ID, null);
        assertTrue(mt instanceof ActionType);

        ActionType at = (ActionType) mt;

        assertEquals(ACTION_LABEL, at.getLabel());
        assertEquals(ACTION_DESCRIPTION, at.getDescription());
        assertEquals(Visibility.HIDDEN, at.getVisibility());
        assertEquals(TEST_ACTION_TYPE_ID, at.getUID());

        Set<String> tags = at.getTags();
        assertTrue(tags.contains("tag1"));
        assertTrue(tags.contains("tag2"));

        List<Input> inputs = at.getInputs();
        assertEquals(2, inputs.size());

        for (Input in : inputs) {
            if (ACTION_INPUT1.equals(in.getName())) {
                assertEquals(ACTION_INPUT1_LABEL, in.getLabel());
                assertEquals(ACTION_INPUT1_DEFAULT_VALUE, in.getDefaultValue());
                assertEquals(ACTION_INPUT1_DESCRIPTION, in.getDescription());
                assertEquals(ACTION_INPUT1_REFERENCE, in.getReference());
                assertEquals(true, in.isRequired());
                assertEquals("Item", in.getType());

                Set<String> inputTags = in.getTags();
                assertTrue(inputTags.contains("tagIn11"));
                assertTrue(inputTags.contains("tagIn12"));
            } else if (ACTION_INPUT2.equals(in.getName())) {
                // if the annotation does not specify a type, we use the java type
                assertEquals("java.lang.String", in.getType());
            }
        }

        List<Output> outputs = at.getOutputs();
        assertEquals(2, outputs.size());

        for (Output o : outputs) {
            if (ACTION_OUTPUT1.equals(o.getName())) {
                assertEquals(ACTION_OUTPUT1_LABEL, o.getLabel());
                assertEquals(ACTION_OUTPUT1_DEFAULT_VALUE, o.getDefaultValue());
                assertEquals(ACTION_OUTPUT1_DESCRIPTION, o.getDescription());
                assertEquals(ACTION_OUTPUT1_REFERENCE, o.getReference());
                assertEquals(ACTION_OUTPUT1_TYPE, o.getType());

                Set<String> outputTags = o.getTags();
                assertTrue(outputTags.contains("tagOut11"));
                assertTrue(outputTags.contains("tagOut12"));
            } else if (ACTION_INPUT2.equals(o.getName())) {
                assertEquals(ACTION_OUTPUT2_TYPE, o.getType());
            }
        }

        // remove the first configuration
        prov.removeActionProvider(actionProviderConf1, properties1);
        types = prov.getTypes();
        assertEquals(1, types.size());

        // check of the second configuration is still valid
        mt = prov.getModuleType(TEST_ACTION_TYPE_ID, null);
        List<ConfigDescriptionParameter> configParams = mt.getConfigurationDescriptions();
        boolean found = false;
        for (ConfigDescriptionParameter cdp : configParams) {
            if (AnnotationActionModuleTypeHelper.CONFIG_PARAM.equals(cdp.getName())) {
                found = true;
                List<ParameterOption> parameterOptions = cdp.getOptions();
                assertEquals(1, parameterOptions.size());

                ParameterOption po = parameterOptions.get(0);
                assertEquals("conf2", po.getValue());
            }
        }
        assertTrue(found);

        // remove the second configuration and there should be none left
        prov.removeActionProvider(actionProviderConf2, properties2);
        types = prov.getTypes();
        assertEquals(0, types.size());

        mt = prov.getModuleType(TEST_ACTION_TYPE_ID, null);
        assertNull(mt);
    }

    @ActionScope(name = "binding.test")
    private class TestActionProvider implements AnnotatedActions {

        @RuleAction(label = ACTION_LABEL, description = ACTION_DESCRIPTION, visibility = Visibility.HIDDEN, tags = {
                "tag1", "tag2" })
        public @ActionOutput(name = ACTION_OUTPUT1, type = ACTION_OUTPUT1_TYPE, description = ACTION_OUTPUT1_DESCRIPTION, label = ACTION_OUTPUT1_LABEL, defaultValue = ACTION_OUTPUT1_DEFAULT_VALUE, reference = ACTION_OUTPUT1_REFERENCE, tags = {
                "tagOut11",
                "tagOut12" }) @ActionOutput(name = ACTION_OUTPUT2, type = ACTION_OUTPUT2_TYPE) Map<String, Object> testMethod(
                        @ActionInput(name = ACTION_INPUT1, label = ACTION_INPUT1_LABEL, defaultValue = ACTION_INPUT1_DEFAULT_VALUE, description = ACTION_INPUT1_DESCRIPTION, reference = ACTION_INPUT1_REFERENCE, required = true, type = "Item", tags = {
                                "tagIn11", "tagIn12" }) String input1,
                        @ActionInput(name = ACTION_INPUT2) String input2) {
            Map<String, Object> result = new HashMap<>();
            result.put("output1", 23);
            result.put("output2", "hello world");

            return result;
        }
    }
}
