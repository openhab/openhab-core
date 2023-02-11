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
package org.openhab.core.automation.thingsupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.automation.module.provider.AnnotationActionModuleTypeHelper;
import org.openhab.core.automation.module.provider.i18n.ModuleTypeI18nService;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.Output;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;

/**
 * Tests for the {@link AnnotatedThingActionModuleTypeProvider}
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class AnnotatedThingActionModuleTypeProviderTest extends JavaTest {

    private static final ThingTypeUID TEST_THING_TYPE_UID = new ThingTypeUID("binding", "thing-type");

    private static final String TEST_ACTION_TYPE_ID = "test.testMethod";
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

    private @Mock @NonNullByDefault({}) ModuleTypeI18nService moduleTypeI18nServiceMock;
    private @Mock @NonNullByDefault({}) ThingHandler handler1Mock;
    private @Mock @NonNullByDefault({}) ThingHandler handler2Mock;

    private ThingActions actionProviderConf1 = new TestThingActionProvider();
    private ThingActions actionProviderConf2 = new TestThingActionProvider();

    @BeforeEach
    public void setUp() {
        when(handler1Mock.getThing()).thenReturn(ThingBuilder.create(TEST_THING_TYPE_UID, "test1").build());

        actionProviderConf1 = new TestThingActionProvider();
        actionProviderConf1.setThingHandler(handler1Mock);

        when(handler2Mock.getThing()).thenReturn(ThingBuilder.create(TEST_THING_TYPE_UID, "test2").build());

        actionProviderConf2 = new TestThingActionProvider();
        actionProviderConf2.setThingHandler(handler2Mock);

        when(moduleTypeI18nServiceMock.getModuleTypePerLocale(any(ModuleType.class), any(), any()))
                .thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    public void testMultiServiceAnnotationActions() {
        AnnotatedThingActionModuleTypeProvider prov = new AnnotatedThingActionModuleTypeProvider(
                moduleTypeI18nServiceMock);

        prov.addAnnotatedThingActions(actionProviderConf1);

        Collection<String> types = prov.getTypes();
        assertEquals(1, types.size());
        assertTrue(types.contains(TEST_ACTION_TYPE_ID));

        prov.addAnnotatedThingActions(actionProviderConf2);

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
        prov.removeAnnotatedThingActions(actionProviderConf1);
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
                assertEquals("binding:thing-type:test2", po.getValue());
            }
        }
        assertTrue(found);

        // remove the second configuration and there should be none left
        prov.removeAnnotatedThingActions(actionProviderConf2);
        types = prov.getTypes();
        assertEquals(0, types.size());

        mt = prov.getModuleType(TEST_ACTION_TYPE_ID, null);
        assertNull(mt);
    }

    @ThingActionsScope(name = "test")
    private class TestThingActionProvider implements ThingActions {

        private @Nullable ThingHandler handler;

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

        @Override
        public void setThingHandler(ThingHandler handler) {
            this.handler = handler;
        }

        @Override
        public @Nullable ThingHandler getThingHandler() {
            return handler;
        }
    }
}
