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
package org.openhab.core.automation.module.script.defaultscope;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.binding.ThingActions;

/**
 * The methods of this class are made available as functions in the scripts.
 *
 * Note: This class is a copy from the {@link org.openhab.core.model.script.internal.engine.action.ThingActionService}
 * class
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Jan N. Klug - Refactored to interface
 */
@NonNullByDefault
public interface ScriptThingActions {
    /**
     * Gets an actions instance of a certain scope for a given thing UID
     *
     * @param scope the action scope
     * @param thingUid the UID of the thing
     * @return actions the actions instance or null, if not available
     */
    @Nullable
    ThingActions get(@Nullable String scope, @Nullable String thingUid);
}
