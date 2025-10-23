/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
    public static final String ACTOR_SEPARATOR = "$";
    public static final String DELEGATION_SEPARATOR = "=>";

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
    protected AbstractEvent(String topic, String payload, @Nullable String source) {
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
        result = prime * result + payload.hashCode();
        result = prime * result + (source instanceof String local ? local.hashCode() : 0);
        result = prime * result + topic.hashCode();
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
        if (!payload.equals(other.payload)) {
            return false;
        }
        String localSource = source;
        if (localSource == null) {
            if (other.source != null) {
                return false;
            }
        } else if (!localSource.equals(other.source)) {
            return false;
        }
        if (!topic.equals(other.topic)) {
            return false;
        }
        return true;
    }

    /**
     * Utility method to build a source string from a package and an optional actor.
     *
     * @param packageName the package (such as org.openhab.core.thing or org.openhab.binding.matter)
     * @param actor the actor
     * @return the final source string
     */
    public static String buildSource(String packageName, @Nullable String actor) {
        if (actor == null || actor.isEmpty()) {
            return packageName;
        }
        return packageName + ACTOR_SEPARATOR + actor;
    }

    /**
     * Utility method to build a delegated source string from an original source and a package
     *
     * @param originalSource the original source (may be null)
     * @param packageName the package (such as org.openhab.core.thing or org.openhab.binding.matter)
     * @return the final source string
     */
    public static String buildDelegatedSource(@Nullable String originalSource, String packageName) {
        if (originalSource == null || originalSource.isEmpty()) {
            return packageName;
        }
        return originalSource + DELEGATION_SEPARATOR + packageName;
    }

    /**
     * Utility method to build a delegated source string from an original source, a package and an optional actor.
     *
     * @param originalSource the original source (may be null)
     * @param packageName the package (such as org.openhab.core.thing or org.openhab.binding.matter)
     * @param actor the actor
     * @return the final source string
     */
    public static String buildDelegatedSource(@Nullable String originalSource, String packageName,
            @Nullable String actor) {
        return buildDelegatedSource(originalSource, buildSource(packageName, actor));
    }
}
