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
package org.openhab.core.events;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Abstract implementation of the {@link Event} interface.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractEvent implements Event {

    private final String topic;

    private final String payload;

    private final @Nullable String source;

    /**
     * Must be called in subclass constructor to create a new event.
     *
     * @param topic the topic
     * @param payload the payload
     * @param source the source
     */
    public AbstractEvent(String topic, String payload, @Nullable String source) {
        this.topic = topic;
        this.payload = payload;
        this.source = source;
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public String getPayload() {
        return payload;
    }

    @Override
    public @Nullable String getSource() {
        return source;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((payload == null) ? 0 : payload.hashCode());
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((topic == null) ? 0 : topic.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractEvent other = (AbstractEvent) obj;
        if (payload == null) {
            if (other.payload != null) {
                return false;
            }
        } else if (!payload.equals(other.payload)) {
            return false;
        }
        if (source == null) {
            if (other.source != null) {
                return false;
            }
        } else if (!source.equals(other.source)) {
            return false;
        }
        if (topic == null) {
            if (other.topic != null) {
                return false;
            }
        } else if (!topic.equals(other.topic)) {
            return false;
        }
        return true;
    }
}
