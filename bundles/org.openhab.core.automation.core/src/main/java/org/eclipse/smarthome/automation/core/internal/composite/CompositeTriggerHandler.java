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
package org.eclipse.smarthome.automation.core.internal.composite;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.automation.ModuleHandlerCallback;
import org.eclipse.smarthome.automation.RuleStatus;
import org.eclipse.smarthome.automation.RuleStatusInfo;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.core.util.ReferenceResolver;
import org.eclipse.smarthome.automation.handler.TriggerHandler;
import org.eclipse.smarthome.automation.handler.TriggerHandlerCallback;
import org.eclipse.smarthome.automation.type.CompositeTriggerType;
import org.eclipse.smarthome.automation.type.Output;

/**
 * This class is a handler implementation for {@link CompositeTriggerType}. The trigger which has
 * {@link CompositeTriggerType} has to be notified by the handlers of child triggers and it will be triggered when some
 * of them is triggered. The handler has to put outputs of the trigger, base on the outputs of the child triggers, into
 * rule context. The outputs of the child triggers are not visible out of context of the trigger.
 *
 * @author Yordan Mihaylov - Initial Contribution
 *
 */
public class CompositeTriggerHandler
        extends AbstractCompositeModuleHandler<Trigger, CompositeTriggerType, TriggerHandler>
        implements TriggerHandler, TriggerHandlerCallback {

    private TriggerHandlerCallback callback;

    /**
     * Constructor of this system handler.
     *
     * @param trigger trigger of composite type (parent trigger).
     * @param mt module type of parent trigger
     * @param mapModuleToHandler map of pairs child triggers to their handlers
     * @param ruleUID UID of rule where the parent trigger is part of
     */
    public CompositeTriggerHandler(Trigger trigger, CompositeTriggerType mt,
            LinkedHashMap<Trigger, TriggerHandler> mapModuleToHandler, String ruleUID) {
        super(trigger, mt, mapModuleToHandler);
    }

    /**
     * This method is called by the child triggers defined by the {@link CompositeTriggerType} of parent trigger.
     * The method goes through the outputs of the parent trigger and fill them base on the ouput's reference value.
     * The ouput's reference value can contain more then one references to the child outputs separated by comma. In this
     * case the method will try to fill the output value in sequence defined in the reference value. The letter
     * reference can be overwritten by the previous ones.
     *
     * @see org.eclipse.smarthome.automation.handler.TriggerHandlerCallback#triggered(org.eclipse.smarthome.automation.Trigger,
     *      java.util.Map)
     */
    @Override
    public void triggered(Trigger trigger, Map<String, ?> context) {
        if (callback != null) {
            List<Output> outputs = moduleType.getOutputs();
            Map<String, Object> result = new HashMap<String, Object>(11);
            for (Output output : outputs) {
                String refs = output.getReference();
                if (refs != null) {
                    String ref;
                    StringTokenizer st = new StringTokenizer(refs, ",");
                    while (st.hasMoreTokens()) {
                        ref = st.nextToken().trim();
                        int i = ref.indexOf('.');
                        if (i != -1) {
                            String childModuleId = ref.substring(0, i);
                            if (trigger.getId().equals(childModuleId)) {
                                ref = ref.substring(i + 1);
                            }
                        }
                        Object value = null;
                        int idx = ReferenceResolver.getNextRefToken(ref, 1);
                        if (idx < ref.length()) {
                            String outputId = ref.substring(0, idx);
                            value = ReferenceResolver.resolveComplexDataReference(context.get(outputId),
                                    ref.substring(idx + 1));
                        } else {
                            value = context.get(ref);
                        }
                        if (value != null) {
                            result.put(output.getName(), value);
                        }
                    }
                }
            }
            callback.triggered(module, result);
        }
    }

    /**
     * The {@link CompositeTriggerHandler} sets itself as callback to the child triggers and store the callback to the
     * rule engine. In this way the trigger of composite type will be notified always when some of the child triggers
     * are triggered and has an opportunity to set the outputs of parent trigger to the rule context.
     *
     * @see org.eclipse.smarthome.automation.handler.TriggerHandler#setTriggerHandlerCallback(org.eclipse.smarthome.automation.handler.TriggerHandlerCallback)
     */
    @Override
    public void setCallback(@Nullable ModuleHandlerCallback callback) {
        this.callback = (TriggerHandlerCallback) callback;
        if (callback instanceof TriggerHandlerCallback) {// could be called with 'null' from dispose and might not be a
                                                         // trigger callback
            List<Trigger> children = getChildren();
            for (Trigger child : children) {
                TriggerHandler handler = moduleHandlerMap.get(child);
                handler.setCallback(this);
            }
        }
    }

    @Override
    public void dispose() {
        setCallback(null);
        super.dispose();
    }

    @Override
    protected List<Trigger> getChildren() {
        return moduleType.getChildren();
    }

    @Override
    public Boolean isEnabled(String ruleUID) {
        return callback.isEnabled(ruleUID);
    }

    @Override
    public void setEnabled(String uid, boolean isEnabled) {
        callback.setEnabled(uid, isEnabled);
    }

    @Override
    public RuleStatusInfo getStatusInfo(String ruleUID) {
        return callback.getStatusInfo(ruleUID);
    }

    @Override
    public RuleStatus getStatus(String ruleUID) {
        return callback.getStatus(ruleUID);
    }

    @Override
    public void runNow(String uid) {
        callback.runNow(uid);
    }

    @Override
    public void runNow(String uid, boolean considerConditions, Map<String, Object> context) {
        callback.runNow(uid, considerConditions, context);
    }

}
