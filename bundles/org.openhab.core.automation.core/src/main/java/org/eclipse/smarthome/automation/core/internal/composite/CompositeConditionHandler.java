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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.handler.ConditionHandler;
import org.eclipse.smarthome.automation.type.CompositeConditionType;

/**
 * This class is a handler implementation for {@link CompositeConditionType}. The condition which has
 * {@link CompositeConditionType} module type will be satisfied only when all child conditions (defined
 * by its {@link CompositeConditionType}) are satisfied.
 *
 * @author Yordan Mihaylov - Initial Contribution
 *
 */
public class CompositeConditionHandler
        extends AbstractCompositeModuleHandler<Condition, CompositeConditionType, ConditionHandler>
        implements ConditionHandler {

    public CompositeConditionHandler(Condition condition, CompositeConditionType mt,
            LinkedHashMap<Condition, ConditionHandler> mapModuleToHandler, String ruleUID) {
        super(condition, mt, mapModuleToHandler);
    }

    /**
     * The method calls handlers of child modules and return true only when they all are satisfied.
     *
     * @see org.eclipse.smarthome.automation.handler.ConditionHandler#isSatisfied(java.util.Map)
     */
    @Override
    public boolean isSatisfied(Map<String, Object> context) {
        List<Condition> children = getChildren();
        Map<String, Object> compositeContext = getCompositeContext(context);
        for (Condition child : children) {
            Map<String, Object> childContext = Collections.unmodifiableMap(getChildContext(child, compositeContext));
            ConditionHandler childHandler = moduleHandlerMap.get(child);
            boolean isSatisfied = childHandler.isSatisfied(childContext);
            if (!isSatisfied) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected List<Condition> getChildren() {
        return moduleType.getChildren();
    }
}
