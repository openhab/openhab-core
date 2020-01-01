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
package org.openhab.core.automation.module.script.rulesupport.internal;

import java.util.Collection;
import java.util.HashMap;

import org.openhab.core.automation.Module;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.handler.ModuleHandlerFactory;
import org.openhab.core.automation.module.script.rulesupport.shared.ScriptedHandler;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link ScriptedCustomModuleHandlerFactory} is used in combination with the
 * {@link ScriptedCustomModuleTypeProvider} to allow scripts to define custom types in the RuleManager. These
 * registered types can then be used publicly from any Rule-Editor.
 *
 * This class provides the handlers from the script to the RuleManager. As Jsr223 languages have different needs, it
 * allows these handlers to be defined in different ways.
 *
 * @author Simon Merschjohann - Initial contribution
 */
@Component(immediate = true, service = { ScriptedCustomModuleHandlerFactory.class, ModuleHandlerFactory.class })
public class ScriptedCustomModuleHandlerFactory extends AbstractScriptedModuleHandlerFactory {
    private final HashMap<String, ScriptedHandler> typesHandlers = new HashMap<>();

    @Override
    public Collection<String> getTypes() {
        return typesHandlers.keySet();
    }

    @Override
    protected ModuleHandler internalCreate(Module module, String ruleUID) {
        ScriptedHandler scriptedHandler = typesHandlers.get(module.getTypeUID());

        return getModuleHandler(module, scriptedHandler);
    }

    public void addModuleHandler(String uid, ScriptedHandler scriptedHandler) {
        typesHandlers.put(uid, scriptedHandler);
    }

    public void removeModuleHandler(String uid) {
        typesHandlers.remove(uid);
    }
}
