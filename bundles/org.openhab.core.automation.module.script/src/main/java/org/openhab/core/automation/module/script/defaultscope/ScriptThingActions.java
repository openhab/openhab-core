/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.defaultscope;

import org.openhab.core.thing.binding.ThingActions;

/**
 * The methods of this class are made available as functions in the scripts.
 *
 * Note: This class is a copy from the {@link ThingActions} class, which resides in the model.script bundle.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public interface ScriptThingActions {
    /**
     * Gets an actions instance of a certain scope for a given thing UID
     *
     * @param scope the action scope
     * @param thingUid the UID of the thing
     * @return actions the actions instance or null, if not available
     */
    ThingActions get(String scope, String thingUid);
}
