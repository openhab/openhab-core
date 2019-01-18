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
package org.eclipse.smarthome.model.script.internal.engine.action;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingActions;
import org.eclipse.smarthome.core.thing.binding.ThingActionsScope;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.model.script.actions.Things;
import org.eclipse.smarthome.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class provides methods for interacting with Things in scripts.
 *
 * @author Maoliang Huang - Initial contribution
 * @author Kai Kreuzer - Extended for general thing access
 *
 */
@Component(immediate = true)
public class ThingActionService implements ActionService {

    private static ThingRegistry thingRegistry;
    private static final Map<String, ThingActions> thingActionsMap = new HashMap<>();

    @Override
    public String getActionClassName() {
        return Things.class.getCanonicalName();
    }

    @Override
    public Class<?> getActionClass() {
        return Things.class;
    }

    @Reference
    public void setThingRegistry(ThingRegistry thingRegistry) {
        ThingActionService.thingRegistry = thingRegistry;
    }

    public void unsetThingRegistry(ThingRegistry thingRegistry) {
        ThingActionService.thingRegistry = null;
    }

    public static ThingStatusInfo getThingStatusInfo(String thingUid) {
        ThingUID uid = new ThingUID(thingUid);
        Thing thing = thingRegistry.get(uid);

        if (thing != null) {
            return thing.getStatusInfo();
        } else {
            return null;
        }
    }

    public static ThingActions getActions(String scope, String thingUid) {
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
    public void addThingActions(ThingActions thingActions) {
        String key = getKey(thingActions);
        thingActionsMap.put(key, thingActions);
    }

    public void removeThingActions(ThingActions thingActions) {
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
