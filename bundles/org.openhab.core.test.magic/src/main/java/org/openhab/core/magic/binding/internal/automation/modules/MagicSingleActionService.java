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
package org.openhab.core.magic.binding.internal.automation.modules;

import java.util.HashMap;
import java.util.Map;

import org.openhab.core.automation.AnnotatedActions;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.ActionScope;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.config.core.ConfigurableService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AnnotatedActions} for one action module with a configuration
 *
 * @author Stefan Triller - Initial contribution
 */
@Component(configurationPid = "org.openhab.magicsingleaction", //
        property = Constants.SERVICE_PID + "=org.openhab.automation.action.magicSingleActionService")
@ConfigurableService(category = "RuleActions", label = "Magic Single Action Service", description_uri = "automationAction:magicSingleAction")
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
