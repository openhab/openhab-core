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
package org.eclipse.smarthome.automation.module.script.defaultscope.internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingActions;
import org.eclipse.smarthome.core.thing.binding.ThingActionsScope;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The methods of this class are made available as functions in the scripts.
 *
 * Note: This class is a copy from the {@link ThingActions} class, which resides in the model.script bundle.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class ScriptThingActions {

    private static final Map<String, ThingActions> thingActionsMap = new HashMap<>();

    ScriptThingActions(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
    }

    private ThingRegistry thingRegistry;

    public void dispose() {
        this.thingRegistry = null;
    }

    /**
     * Gets an actions instance of a certain scope for a given thing UID
     *
     * @param scope the action scope
     * @param thingUid the UID of the thing
     *
     * @return actions the actions instance or null, if not available
     */
    public ThingActions get(String scope, String thingUid) {
        ThingUID uid = new ThingUID(thingUid);
        Thing thing = thingRegistry.get(uid);
        if (thing != null) {
            ThingHandler handler = thing.getHandler();
            if (handler != null) {
                ThingActions thingActions = thingActionsMap.get(getKey(scope, thingUid));
                return thingActions;
            }
        }
        return null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    void addThingActions(ThingActions thingActions) {
        String key = getKey(thingActions);
        thingActionsMap.put(key, thingActions);
    }

    void removeThingActions(ThingActions thingActions) {
        String key = getKey(thingActions);
        thingActionsMap.remove(key);
    }

    private static String getKey(ThingActions thingActions) {
        String scope = getScope(thingActions);
        String thingUID = getThingUID(thingActions);
        return getKey(scope, thingUID);
    }

    private static String getKey(String scope, String thingUID) {
        return scope + "-" + thingUID;
    }

    private static String getThingUID(ThingActions actions) {
        return actions.getThingHandler().getThing().getUID().getAsString();
    }

    private static String getScope(ThingActions actions) {
        ThingActionsScope scopeAnnotation = actions.getClass().getAnnotation(ThingActionsScope.class);
        return scopeAnnotation.name();
    }

}
