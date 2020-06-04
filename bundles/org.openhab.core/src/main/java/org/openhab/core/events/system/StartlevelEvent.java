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
package org.openhab.core.events.system;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEvent;

/**
 * {@link StartlevelEvent}s will be delivered through the openHAB event bus if the start level of the system has
 * changed.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class StartlevelEvent extends AbstractEvent {

    public static final String TYPE = StartlevelEvent.class.getSimpleName();

    private final Integer startlevel;

    /**
     * Creates a new system startlevel event object.
     *
     * @param topic the topic
     * @param payload the payload
     * @param source the source
     * @param startlevel the system startlevel
     */
    protected StartlevelEvent(String topic, String payload, @Nullable String source, Integer startlevel) {
        super(topic, payload, source);
        this.startlevel = startlevel;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Gets the system startlevel.
     *
     * @return the startlevel
     */
    public Integer getStartlevel() {
        return startlevel;
    }

    @Override
    public String toString() {
        return String.format("Startlevel '%d' reached.", startlevel);
    }
}
