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
package org.openhab.core.thing.internal.update;

import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ThingBuilder;

/**
 * The {@link ThingUpdateInstruction} is an interface that can be implemented to perform updates on things when the
 * thing-type changes
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ThingUpdateInstruction {

    /**
     * Get the thing type version for which this update is needed
     *
     * @return the thing-type version (always > 0)
     */
    int getThingTypeVersion();

    /**
     * Perform the update in this instruction for a given {@link Thing} using the given {@link ThingBuilder}
     * <p />
     * Note: the thing type version is not updated as there may be several instructions to perform for a single version.
     *
     * @param thing the thing that should be updated
     * @param thingBuilder the thing builder to use
     */
    void perform(Thing thing, ThingBuilder thingBuilder);

    static Predicate<ThingUpdateInstruction> applies(int currentThingTypeVersion) {
        return i -> i.getThingTypeVersion() > currentThingTypeVersion;
    }
}
