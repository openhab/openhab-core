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
package org.openhab.core.model.script.actions;

import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.model.script.internal.engine.action.ThingActionService;

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
}
