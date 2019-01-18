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
package org.eclipse.smarthome.automation.sample.extension.java.internal.handler;

import java.util.Map;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.handler.BaseModuleHandler;
import org.eclipse.smarthome.automation.sample.extension.java.internal.type.WelcomeHomeActionType;
import org.eclipse.smarthome.config.core.Configuration;

/**
 * This class serves to handle the Action types provided by this application. It is used to help the RuleManager
 * to execute the {@link Action}s.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class WelcomeHomeActionHandler extends BaseModuleHandler<Action> implements ActionHandler {

    public WelcomeHomeActionHandler(Action module) {
        super(module);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> context) {
        getDevice(module.getConfiguration());
        getResult(module.getConfiguration());
        return null;
    }

    /**
     * This method provides the way to configure which device to execute the action.
     *
     * @param configration
     *            of the {@link Action} module.
     * @return
     *         the string representing the device.
     */
    private String getDevice(Configuration configration) {
        return (String) (configration != null ? configration.get(WelcomeHomeActionType.CONFIG_DEVICE) : null);
    }

    /**
     * This method provides the way to configure the command which to be executed.
     *
     * @param configration
     *            of the {@link Action} module.
     * @return
     *         the command which to be executed.
     */
    private String getResult(Configuration configration) {
        return (String) (configration != null ? configration.get(WelcomeHomeActionType.CONFIG_RESULT) : null);
    }
}
