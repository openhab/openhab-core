/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.script.actions;

import org.openhab.core.model.script.internal.engine.action.ThingActionService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingActions;

/**
 * This class provides static methods that can be used in automation rules for
 * getting thing's status info.
 *
 * @author Maoliang Huang - Initial contribution
 * @author Kai Kreuzer - Extended for general thing access
 */
public class Things {

    /**
     * Retrieves the status info of a Thing
     *
     * @param thingUid The uid of the thing
     * @return <code>ThingStatusInfo</code>
     */
    public static ThingStatusInfo getThingStatusInfo(String thingUid) {
        return ThingActionService.getThingStatusInfo(thingUid);
    }

    /**
     * Get the actions instance for a Thing of a given scope
     *
     * @param scope The action scope
     * @param thingUid The uid of the thing
     * @return the <code>ThingActions</code> instance
     */
    public static ThingActions getActions(String scope, String thingUid) {
        return ThingActionService.getActions(scope, thingUid);
    }

    /**
     * Gets the actions instance of a certain scope for a given {@link Thing}.
     *
     * @param thing the {@link Thing}.
     * @param scope the action scope.
     *
     * @return The {@link ThingActions} instance or {@code null}.
     */
    public static ThingActions getActions(Thing thing, String scope) {
        return ThingActionService.getActions(thing, scope);
    }

    /**
     * Get a {@link Thing} from the {@link ThingRegistry}.
     *
     * @param registry the {@link ThingRegistry}.
     * @param thingUid the Thing UID string.
     * @return The {@link Thing} instance of {@code null}.
     */
    public static Thing get(ThingRegistry registry, String thingUid) {
        if (registry == null || thingUid == null) {
            return null;
        }
        return registry.get(new ThingUID(thingUid));
    }
}
