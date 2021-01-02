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
package org.openhab.core.config.core.status.events;

import org.openhab.core.config.core.status.ConfigStatusInfo;
import org.openhab.core.events.AbstractEvent;

import com.google.gson.Gson;

/**
 * Event for configuration status information.
 *
 * @author Thomas Höfer - Initial contribution
 */
public final class ConfigStatusInfoEvent extends AbstractEvent {

    static final String TYPE = "ConfigStatusInfoEvent";

    private final ConfigStatusInfo configStatusInfo;

    private static final Gson GSON = new Gson();

    /**
     * Creates a new {@link ConfigStatusInfoEvent}.
     *
     * @param topic the topic of the event
     * @param configStatusInfo the corresponding configuration status information to be put as payload into the event
     */
    public ConfigStatusInfoEvent(String topic, ConfigStatusInfo configStatusInfo) {
        super(topic, GSON.toJson(configStatusInfo), null);
        this.configStatusInfo = configStatusInfo;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return configStatusInfo.toString();
    }
}
