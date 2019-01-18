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
package org.eclipse.smarthome.core.thing.binding;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.core.status.ConfigStatusSource;
import org.eclipse.smarthome.core.thing.Thing;

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
