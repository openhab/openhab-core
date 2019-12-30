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
package org.openhab.core.thing;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;

/**
 * The {@link ThingTypeMigrationService} describes a service to change the thing type
 * of a given {@link Thing}.
 *
 * @author Andre Fuechsel - Initial contribution
 */
@NonNullByDefault
public interface ThingTypeMigrationService {

    /**
     * Changes the type of a given {@link Thing}.
     *
     * @param thing {@link Thing} whose type should be changed
     * @param thingTypeUID new {@link ThingTypeUID}
     * @param configuration new configuration
     * @throws RuntimeException, if the new thing type is not registered in the registry
     */
    void migrateThingType(Thing thing, ThingTypeUID thingTypeUID, @Nullable Configuration configuration);

}
