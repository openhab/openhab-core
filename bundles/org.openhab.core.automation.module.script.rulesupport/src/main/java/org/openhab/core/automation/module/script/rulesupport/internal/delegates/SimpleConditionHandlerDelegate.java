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
package org.openhab.core.automation.module.script.rulesupport.internal.delegates;

import java.util.Map;

import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseConditionModuleHandler;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleConditionHandler;

/**
 * The SimpleConditionHandlerDelegate allows the registration of {@link SimpleConditionHandler}s to the RuleManager.
 *
 * @author Simon Merschjohann - Initial contribution
 */
public class SimpleConditionHandlerDelegate extends BaseConditionModuleHandler {

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
