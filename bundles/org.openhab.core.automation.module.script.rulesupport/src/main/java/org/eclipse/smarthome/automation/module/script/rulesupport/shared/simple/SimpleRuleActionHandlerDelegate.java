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
package org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple;

import java.util.Map;

import org.eclipse.smarthome.automation.Action;

/**
 *
 * @author Simon Merschjohann - Initial contribution
 */
public class SimpleRuleActionHandlerDelegate extends SimpleActionHandler {

    private SimpleRuleActionHandler handler;

    public SimpleRuleActionHandlerDelegate(SimpleRuleActionHandler handler) {
        super();
        this.handler = handler;
    }

    @Override
    public Object execute(Action module, Map<String, ?> inputs) {
        return handler.execute(module, inputs);
    }

}
