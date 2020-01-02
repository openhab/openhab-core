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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AnnotatedActions} with multiple action modules that can have different instances/configurations
 *
 * The {@link MagicMultiActionMarker} holds the configuration URI and shows the system that THIS service can be
 * instantiated multiple times
 *
 * @author Stefan Triller - Initial contribution
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, configurationPid = "org.openhab.magicmultiaction")
@ActionScope(name = "binding.magic")
public class MagicMultiServiceMultiActions implements AnnotatedActions {

    private final Logger logger = LoggerFactory.getLogger(MagicMultiServiceMultiActions.class);

    protected Map<String, Object> config;

    @Activate
    protected void activate(Map<String, Object> config) {
        this.config = config;
    }

    @Modified
    protected synchronized void modified(Map<String, Object> config) {
        this.config = config;
    }

    @RuleAction(label = "@text/module.binding.magic.testMethod.label", description = "Just a text that prints out inputs and config parameters")
    public @ActionOutput(name = "output1", type = "java.lang.Integer") @ActionOutput(name = "output2", type = "java.lang.String") Map<String, Object> testMethod(
            @ActionInput(name = "input1") String input1, @ActionInput(name = "input2") String input2) {
        Map<String, Object> result = new HashMap<>();
        result.put("output1", 23);
        result.put("output2", "hello world");

        boolean boolParam = (Boolean) config.get("boolParam");
        String textParam = (String) config.get("textParam");

        logger.debug(
                "Executed multi action testMethod with inputs: {}, {} and configParams: boolParam={}, textParam={}",
                input1, input2, boolParam, textParam);

        return result;
    }

    @RuleAction(label = "Magic Multi Action boolean", description = "Action method that returns a plain boolean")
    public @ActionOutput(name = "out1", type = "java.lang.Boolean") boolean booleanMethod(
            @ActionInput(name = "in1") String input1, @ActionInput(name = "in2") String input2) {
        Map<String, Object> result = new HashMap<>();
        result.put("output1", 42);
        result.put("output2", "foobar");

        boolean boolParam = (Boolean) config.get("boolParam");
        String textParam = (String) config.get("textParam");

        logger.debug("executed boolean method with: {}, {}", input1, input2);

        logger.debug(
                "Executed multi action booleanMethod with inputs: {}, {} and configParams: boolParam={}, textParam={}",
                input1, input2, boolParam, textParam);

        return true;
    }

    @RuleAction(label = "Magic Multi Action void", description = "Action method with type void, so no outputs")
    public void voidMethod(@ActionInput(name = "inv1") String input1, @ActionInput(name = "inv2") String input2) {
        boolean boolParam = (Boolean) config.get("boolParam");
        String textParam = (String) config.get("textParam");

        logger.debug(
                "Executed multi action voidMethod with inputs: {}, {} and configParams: boolParam={}, textParam={}",
                input1, input2, boolParam, textParam);
    }
}
