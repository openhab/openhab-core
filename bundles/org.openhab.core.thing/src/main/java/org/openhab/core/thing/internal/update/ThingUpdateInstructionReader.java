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

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingHandlerFactory;

/**
 * The {@link ThingUpdateInstructionReader} is used to read instructions for a given {@link ThingHandlerFactory} and
 * create a list of {@link ThingUpdateInstruction}s
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface ThingUpdateInstructionReader {
    Map<UpdateInstructionKey, List<ThingUpdateInstruction>> readForFactory(ThingHandlerFactory factory);

    record UpdateInstructionKey(ThingHandlerFactory factory, ThingTypeUID thingTypeId) {
    }
}
