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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.BaseActionModuleHandler;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleActionHandler;

/**
 * The SimpleActionHandlerDelegate allows the registration of {@link SimpleActionHandler}s to the RuleManager.
 *
 * @author Simon Merschjohann - Initial contribution
 */
public class SimpleActionHandlerDelegate extends BaseActionModuleHandler {

    private org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleActionHandler actionHandler;

    public SimpleActionHandlerDelegate(Action module,
            org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleActionHandler actionHandler) {
        super(module);
        this.actionHandler = actionHandler;
    }

    @Override
    public void dispose() {
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> inputs) {
        Set<String> keys = new HashSet<>(inputs.keySet());

        Map<String, Object> extendedInputs = new HashMap<>(inputs);
        for (String key : keys) {
            Object value = extendedInputs.get(key);
            int dotIndex = key.indexOf('.');
            if (dotIndex != -1) {
                String moduleName = key.substring(0, dotIndex);
                extendedInputs.put("module", moduleName);
                String newKey = key.substring(dotIndex + 1);
                extendedInputs.put(newKey, value);
            }
        }

        Object result = actionHandler.execute(module, extendedInputs);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("result", result);
        return resultMap;
    }
}
