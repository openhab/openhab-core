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
package org.openhab.core.model.script.internal.engine.action;

import java.util.HashMap;
import java.util.Map;

import org.openhab.core.model.script.actions.Things;
import org.openhab.core.model.script.engine.action.ActionService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class provides methods for interacting with Things in scripts.
 *
 * @author Maoliang Huang - Initial contribution
 * @author Kai Kreuzer - Extended for general thing access
 */
@Component(immediate = true)
public class ThingActionService implements ActionService {

    private static final Map<String, ThingActions> THING_ACTIONS_MAP = new HashMap<>();
    private static ThingRegistry thingRegistry;

    @Activate
    public ThingActionService(final @Reference ThingRegistry thingRegistry) {
        ThingActionService.thingRegistry = thingRegistry;
    }

    @Override
    public Class<?> getActionClass() {
        return Things.class;
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

    /**
     * Gets an actions instance of a certain scope for a given thing UID
     *
     * @param scope the action scope
     * @param thingUid the UID of the thing
     *
     * @return actions the actions instance or null, if not available
     */
    public static ThingActions getActions(String scope, String thingUid) {
        ThingUID uid = new ThingUID(thingUid);
        Thing thing = thingRegistry.get(uid);
        if (thing != null) {
            ThingHandler handler = thing.getHandler();
            if (handler != null) {
                ThingActions thingActions = THING_ACTIONS_MAP.get(getKey(scope, thingUid));
                return thingActions;
            }
        }
        return null;
    }

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void addThingActions(ThingActions thingActions) {
        String key = getKey(thingActions);
        THING_ACTIONS_MAP.put(key, thingActions);
    }

    public void removeThingActions(ThingActions thingActions) {
        String key = getKey(thingActions);
        THING_ACTIONS_MAP.remove(key);
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
