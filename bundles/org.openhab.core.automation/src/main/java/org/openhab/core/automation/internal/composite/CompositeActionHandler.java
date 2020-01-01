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
package org.openhab.core.automation.internal.composite;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.ActionHandler;
import org.openhab.core.automation.type.CompositeActionType;
import org.openhab.core.automation.type.Output;
import org.openhab.core.automation.util.ReferenceResolver;

/**
 * This class is a handler implementation for {@link CompositeActionType}. The action of type
 * {@link CompositeActionType} has to execute handlers of all child actions. The handler has to return outputs of the
 * action, base on the outputs of the child actions, into rule context. The outputs of the child actions are not
 * visible out of the context of the action.
 *
 * @author Yordan Mihaylov - Initial contribution
 */
public class CompositeActionHandler extends AbstractCompositeModuleHandler<Action, CompositeActionType, ActionHandler>
        implements ActionHandler {

    public static final String REFERENCE = "reference";

    private final Map<String, Output> compositeOutputs;

    /**
     * Create a system handler for modules of {@link CompositeActionType} type.
     *
     * @param action parent action module instance. The action which has {@link CompositeActionType} type.
     * @param mt {@link CompositeActionType} instance of the parent module
     * @param mapModuleToHandler map of pairs child action module to its action handler
     * @param ruleUID UID of rule where the parent action is part of.
     */
    public CompositeActionHandler(Action action, CompositeActionType mt,
            LinkedHashMap<Action, ActionHandler> mapModuleToHandler, String ruleUID) {
        super(action, mt, mapModuleToHandler);
        compositeOutputs = getCompositeOutputMap(moduleType.getOutputs());
    }

    /**
     * The method calls handlers of child action, collect their outputs and sets the output of the parent action.
     *
     * @see org.openhab.core.automation.handler.ActionHandler#execute(java.util.Map)
     */
    @Override
    public Map<String, Object> execute(Map<String, Object> context) {
        final Map<String, Object> result = new HashMap<>();
        final List<Action> children = getChildren();
        final Map<String, Object> compositeContext = getCompositeContext(context);
        for (Action child : children) {
            ActionHandler childHandler = moduleHandlerMap.get(child);
            Map<String, Object> childContext = Collections.unmodifiableMap(getChildContext(child, compositeContext));
            Map<String, Object> childResults = childHandler.execute(childContext);
            if (childResults != null) {
                for (Entry<String, Object> childResult : childResults.entrySet()) {
                    String childOuputName = child.getId() + "." + childResult.getKey();
                    Output output = compositeOutputs.get(childOuputName);
                    if (output != null) {
                        String childOuputRef = output.getReference();
                        if (childOuputRef != null && childOuputRef.length() > childOuputName.length()) {
                            childOuputRef = childOuputRef.substring(childOuputName.length());
                            result.put(output.getName(), ReferenceResolver
                                    .resolveComplexDataReference(childResult.getValue(), childOuputRef));
                        } else {
                            result.put(output.getName(), childResult.getValue());
                        }
                    }
                }
            }

        }
        return !result.isEmpty() ? result : null;
    }

    /**
     * Create a map of links between child outputs and parent outputs. These links are base on the refecences defined in
     * the outputs of parent action.
     *
     * @param outputs outputs of the parent action. The action of {@link CompositeActionType}
     * @return map of links between child action outputs and parent output
     */
    protected Map<String, Output> getCompositeOutputMap(List<Output> outputs) {
        Map<String, Output> result = new HashMap<>(11);
        if (outputs != null) {
            for (Output output : outputs) {
                String refs = output.getReference();
                if (refs != null) {
                    String ref;
                    StringTokenizer st = new StringTokenizer(refs, ",");
                    while (st.hasMoreTokens()) {
                        ref = st.nextToken().trim();
                        int i = ref.indexOf('.');
                        if (i != -1) {
                            int j = ReferenceResolver.getNextRefToken(ref, i + 1);
                            if (j != -1) {
                                ref = ref.substring(0, j);
                            }
                        }
                        result.put(ref, output);
                    }
                }
            }
        }
        return result;
    }

    @Override
    protected List<Action> getChildren() {
        return moduleType.getChildren();
    }
}
