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
package org.eclipse.smarthome.automation.module.script.rulesupport.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.handler.ModuleHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.ScriptedHandler;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ScriptedPrivateModuleHandlerFactory} is used to provide types for "private" scripted Actions, Triggers and
 * Conditions. These Module Types are meant to be only used inside scripts.
 *
 * This class provides the handlers from the script to the RuleManager. As Jsr223 languages have different needs, it
 * allows these handlers to be defined in different ways.
 *
 * @author Simon Merschjohann
 *
 */
@Component(immediate = true, service = { ScriptedPrivateModuleHandlerFactory.class, ModuleHandlerFactory.class })
public class ScriptedPrivateModuleHandlerFactory extends AbstractScriptedModuleHandlerFactory {
    private static final String PRIV_ID = "privId";
    private static final Collection<String> TYPES = Arrays.asList("jsr223.ScriptedAction", "jsr223.ScriptedCondition",
            "jsr223.ScriptedTrigger");

    private final Logger logger = LoggerFactory.getLogger(ScriptedPrivateModuleHandlerFactory.class);
    private final HashMap<String, ScriptedHandler> privateTypes = new HashMap<>();

    private int nextId = 0;

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    @Override
    protected ModuleHandler internalCreate(Module module, String ruleUID) {
        ModuleHandler moduleHandler = null;

        ScriptedHandler scriptedHandler = null;
        try {
            scriptedHandler = privateTypes.get(module.getConfiguration().get(PRIV_ID));
        } catch (Exception e) {
            logger.warn("ScriptedHandler {} for ruleUID {} not found", module.getConfiguration().get(PRIV_ID), ruleUID);
        }

        if (scriptedHandler != null) {
            moduleHandler = getModuleHandler(module, scriptedHandler);
        }

        return moduleHandler;
    }

    public String addHandler(String privId, ScriptedHandler scriptedHandler) {
        privateTypes.put(privId, scriptedHandler);
        return privId;
    }

    public String addHandler(ScriptedHandler scriptedHandler) {
        String privId = "i" + (nextId++);
        privateTypes.put(privId, scriptedHandler);
        return privId;
    }

    public void removeHandler(String privId) {
        privateTypes.remove(privId);
    }
}
