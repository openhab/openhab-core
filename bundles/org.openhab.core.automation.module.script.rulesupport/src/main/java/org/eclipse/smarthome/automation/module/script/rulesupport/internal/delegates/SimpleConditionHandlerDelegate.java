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
package org.eclipse.smarthome.automation.module.script.rulesupport.internal.delegates;

import java.util.Map;

import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.automation.handler.ConditionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleConditionHandler;

/**
 * The SimpleConditionHandlerDelegate allows the registration of {@link SimpleConditionHandler}s to the RuleManager.
 *
 * @author Simon Merschjohann
 *
 */
public class SimpleConditionHandlerDelegate extends BaseModuleHandler<Condition> implements ConditionHandler {

    private SimpleConditionHandler conditionHandler;

    public SimpleConditionHandlerDelegate(Condition condition, SimpleConditionHandler scriptedHandler) {
        super(condition);
        this.conditionHandler = scriptedHandler;
        scriptedHandler.init(condition);
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {
        return conditionHandler.isSatisfied(module, inputs);
    }
}
