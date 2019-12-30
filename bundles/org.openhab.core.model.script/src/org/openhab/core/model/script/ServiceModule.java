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
package org.openhab.core.model.script;

import org.openhab.core.items.ItemRegistry;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.engine.IActionServiceProvider;
import org.openhab.core.model.script.engine.IThingActionsProvider;
import org.openhab.core.model.script.engine.ScriptEngine;
import org.openhab.core.model.script.internal.engine.ServiceTrackerActionServiceProvider;
import org.openhab.core.model.script.internal.engine.ServiceTrackerThingActionsProvider;
import org.openhab.core.model.script.script.Script;
import org.openhab.core.model.script.script.impl.ScriptImpl;

import com.google.inject.Binder;
import com.google.inject.Module;

/**
 * Guice module that binds openHAB services
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class ServiceModule implements Module {

    private final ScriptServiceUtil scriptServiceUtil;
    private final ScriptEngine scriptEngine;

    public ServiceModule(ScriptServiceUtil scriptServiceUtil, ScriptEngine scriptEngine) {
        this.scriptServiceUtil = scriptServiceUtil;
        this.scriptEngine = scriptEngine;
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ItemRegistry.class).toInstance(scriptServiceUtil.getItemRegistryInstance());
        binder.bind(ThingRegistry.class).toInstance(scriptServiceUtil.getThingRegistryInstance());
        binder.bind(ModelRepository.class).toInstance(scriptServiceUtil.getModelRepositoryInstance());
        binder.bind(ScriptEngine.class).toInstance(scriptEngine);
        binder.bind(IActionServiceProvider.class)
                .toInstance(new ServiceTrackerActionServiceProvider(scriptServiceUtil));
        binder.bind(IThingActionsProvider.class).toInstance(new ServiceTrackerThingActionsProvider(scriptServiceUtil));
        binder.bind(Script.class).to(ScriptImpl.class);
    }

}
