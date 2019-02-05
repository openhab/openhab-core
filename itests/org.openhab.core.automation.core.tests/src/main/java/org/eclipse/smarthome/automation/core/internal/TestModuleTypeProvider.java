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

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.automation.type.ConditionType;
import org.eclipse.smarthome.automation.type.Input;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.automation.type.Output;
import org.eclipse.smarthome.automation.type.TriggerType;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;

/**
 * ModuleTypeManagerMockup is a ModuleTypeManager which returns mockup module types for the following predefined module
 * types: triggerTypeUID, conditionTypeUID and actionTypeUID. The types have preset tags of their input and outputs.
 *
 * @author Yordan Mihaylov - initial version
 */
public class TestModuleTypeProvider implements ModuleTypeProvider {

    public static final String TRIGGER_TYPE = "triggerTypeUID";
    public static final String ACTION_TYPE = "actionTypeUID";
    public static final String CONDITION_TYPE = "conditionTypeUID";

    public TestModuleTypeProvider() {
        super();
    }

    private TriggerType createTriggerType() {
        List<Output> outputs = new ArrayList<Output>(3);
        outputs.add(createOutput("out1", new String[] { "tagA" }));
        outputs.add(createOutput("out2", new String[] { "tagB", "tagC" }));
        outputs.add(createOutput("out3", new String[] { "tagA", "tagB", "tagC" }));
        TriggerType t = new TriggerType(TRIGGER_TYPE, null, outputs);
        return t;
    }

    private ConditionType createConditionType() {
        List<Input> inputs = new ArrayList<Input>(3);
        inputs.add(createInput("in0", new String[] { "tagE" })); // no connection, missing condition tag
        inputs.add(createInput("in1", new String[] { "tagA" })); // conflict in2 -> out1 or in2 -> out3
        inputs.add(createInput("in2", new String[] { "tagA", "tagB" })); // in2 -> out3
        ConditionType t = new ConditionType(CONDITION_TYPE, null, inputs);
        return t;
    }

    private ActionType createActionType() {
        List<Input> inputs = new ArrayList<Input>(3);
        inputs.add(createInput("in3", new String[] { "tagD" })); // conflict in3 -> out4 or in3 -> out5
        inputs.add(createInput("in4", new String[] { "tagD", "tagE" })); // in4 -> out5
        inputs.add(createInput("in5", new String[] { "tagA", "tagB", "tagC" })); // in5 -> out3
        inputs.add(createInput("in6", new String[] { "tagA", "tagB" })); // conflict in6 has user defined connection

        List<Output> outputs = new ArrayList<Output>(3);
        outputs.add(createOutput("out4", new String[] { "tagD" }));
        outputs.add(createOutput("out5", new String[] { "tagD", "tagE" }));
        ActionType t = new ActionType(ACTION_TYPE, null, inputs, outputs);
        return t;
    }

    private Output createOutput(String name, String[] tags) {
        Set<String> tagSet = new HashSet<String>(Arrays.asList(tags));
        return new Output(name, String.class.getName(), null, null, tagSet, null, null);
    }

    private Input createInput(String name, String[] tags) {
        Set<String> tagSet = new HashSet<String>(Arrays.asList(tags));
        return new Input(name, String.class.getName(), null, null, tagSet, false, null, null);
    }

    @Override
    public void addProviderChangeListener(@NonNull ProviderChangeListener<ModuleType> listener) {
    }

    @Override
    public @NonNull Collection<ModuleType> getAll() {
        return Stream.of(createTriggerType(), createConditionType(), createActionType()).collect(toSet());
    }

    @Override
    public void removeProviderChangeListener(@NonNull ProviderChangeListener<ModuleType> listener) {
    }

    @Override
    public <T extends ModuleType> T getModuleType(String UID, Locale locale) {
        return null;
    }

    @Override
    public <T extends ModuleType> Collection<T> getModuleTypes(Locale locale) {
        return null;
    }

}
