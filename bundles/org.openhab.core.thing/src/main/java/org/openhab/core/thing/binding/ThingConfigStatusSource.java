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
package org.openhab.core.thing.binding;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.core.status.ConfigStatusSource;
import org.openhab.core.thing.Thing;

/**
 * An implementation of {@link ConfigStatusSource} for the {@link Thing} entity.
 *
 * @author Thomas Höfer - Initial contribution
 */
@NonNullByDefault
public final class ThingConfigStatusSource extends ConfigStatusSource {

    private static final String TOPIC = "smarthome/things/{thingUID}/config/status";

    /**
     * Creates a new {@link ThingConfigStatusSource} for the given thing UID.
     *
     * @param thingUID the UID of the thing
     */
    public ThingConfigStatusSource(String thingUID) {
        super(thingUID);
    }

    @Override
    public String getTopic() {
        return TOPIC.replace("{thingUID}", this.entityId);
    }
}
