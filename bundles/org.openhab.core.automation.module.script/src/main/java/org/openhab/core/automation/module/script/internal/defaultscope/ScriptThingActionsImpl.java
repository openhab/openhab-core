/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.internal.defaultscope;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.defaultscope.ScriptThingActions;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;

/**
 * The methods of this class are made available as functions in the scripts.
 *
 * Note: This class is a copy from the {@link org.openhab.core.model.script.internal.engine.action.ThingActionService}
 * class
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Jan N. Klug - Moved implementation to internal class
 */
@NonNullByDefault
public class ScriptThingActionsImpl implements ScriptThingActions {

    private static final Map<String, ThingActions> THING_ACTIONS_MAP = new HashMap<>();
    private @Nullable ThingRegistry thingRegistry;

    ScriptThingActionsImpl(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    public void dispose() {
        this.thingRegistry = null;
    }

    @Override
    public @Nullable ThingActions get(@Nullable String scope, @Nullable String thingUid) {
        ThingRegistry thingRegistry = this.thingRegistry;
        if (thingUid != null && scope != null && thingRegistry != null) {
            ThingUID uid = new ThingUID(thingUid);
            Thing thing = thingRegistry.get(uid);
            if (thing != null) {
                ThingHandler handler = thing.getHandler();
                if (handler != null) {
                    return THING_ACTIONS_MAP.get(getKey(scope, thingUid));
                }
            }
        }
        return null;
    }

    void addThingActions(ThingActions thingActions) {
        String key = getKey(thingActions);
        if (key != null) {
            THING_ACTIONS_MAP.put(key, thingActions);
        }
    }

    void removeThingActions(ThingActions thingActions) {
        String key = getKey(thingActions);
        THING_ACTIONS_MAP.remove(key);
    }

    private static @Nullable String getKey(ThingActions thingActions) {
        String scope = getScope(thingActions);
        String thingUID = getThingUID(thingActions);
        if (thingUID == null) {
            return null;
        } else {
            return getKey(scope, thingUID);
        }
    }

    private static String getKey(String scope, String thingUID) {
        return scope + "-" + thingUID;
    }

    private static @Nullable String getThingUID(ThingActions actions) {
        ThingHandler thingHandler = actions.getThingHandler();
        if (thingHandler == null) {
            return null;
        }
        return thingHandler.getThing().getUID().getAsString();
    }

    private static String getScope(ThingActions actions) {
        ThingActionsScope scopeAnnotation = actions.getClass().getAnnotation(ThingActionsScope.class);
        return scopeAnnotation.name();
    }
}
