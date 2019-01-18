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
package org.eclipse.smarthome.magic.binding.internal.automation.modules;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.automation.AnnotatedActions;
import org.eclipse.smarthome.automation.annotation.ActionInput;
import org.eclipse.smarthome.automation.annotation.ActionOutput;
import org.eclipse.smarthome.automation.annotation.ActionScope;
import org.eclipse.smarthome.automation.annotation.RuleAction;
import org.eclipse.smarthome.config.core.ConfigurableService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AnnotatedActions} for one action module with a configuration
 *
 * @author Stefan Triller - initial contribution
 *
 */
@Component(configurationPid = "org.eclipse.smarthome.automation.action.magicSingleActionService", property = {
        Constants.SERVICE_PID + "=org.eclipse.smarthome.automation.action.magicSingleActionService",
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=automationAction:magicSingleAction",
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=Magic Single Action Service",
        ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=RuleActions" })
@ActionScope(name = "binding.magicService")
public class MagicSingleActionService implements AnnotatedActions {

    private final Logger logger = LoggerFactory.getLogger(MagicSingleActionService.class);

    protected Map<String, Object> config;

    @Activate
    protected void activate(Map<String, Object> config) {
        this.config = config;
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        this.config = config;
    }

    @RuleAction(label = "Magic Single Service", description = "Just a simple Magic Single Service Action")
    public @ActionOutput(name = "output1", type = "java.lang.Integer") @ActionOutput(name = "output2", type = "java.lang.String") Map<String, Object> singleServiceAction(
            @ActionInput(name = "input1") String input1, @ActionInput(name = "input2") String input2, String input3,
            @ActionInput(name = "someNameForInput4") String input4) {

        // do some calculation stuff here and place the outputs into the result map
        Map<String, Object> result = new HashMap<>();
        result.put("output1", 42);
        result.put("output2", "myOutput2 String");

        String configParam = (String) this.config.get("confParam1");

        logger.debug(
                "Magic Magic Single Service method: executed map method with inputs: {}, {}, {}, {} and configuration parameter: {}",
                input1, input2, input3, input4, configParam);

        return result;
    }
}
