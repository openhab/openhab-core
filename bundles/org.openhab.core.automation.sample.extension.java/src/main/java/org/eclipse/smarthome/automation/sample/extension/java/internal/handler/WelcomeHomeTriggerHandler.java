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

import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseTriggerModuleHandler;
import org.eclipse.smarthome.automation.handler.TriggerHandlerCallback;

/**
 * This class serves to handle the Trigger types provided by this application. It is used to notify the RuleManager
 * about
 * firing the {@link Trigger}s.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class WelcomeHomeTriggerHandler extends BaseTriggerModuleHandler {

    public WelcomeHomeTriggerHandler(Trigger module) {
        super(module);
    }

    /**
     * This method is used to notify the RuleManager about firing the {@link Trigger}s.
     *
     * @param context
     *            is used to provide the output of the {@link Trigger}.
     */
    public void trigger(Map<String, ?> context) {
        ((TriggerHandlerCallback) callback).triggered(module, context);
    }
}
