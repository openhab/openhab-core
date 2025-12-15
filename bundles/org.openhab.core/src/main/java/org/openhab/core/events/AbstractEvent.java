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
    public static final String DELEGATION_ESCAPE = "__";

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
     * Utility method to build a source string from a bundle and an optional actor.
     *
     * Bundle names may not contain the actor separator.
     *
     * The actor, if present, will be replaced with `__` to disallow the delegation separator.
     * Consequently, `__` will be doubled as an escape sequence.
     *
     * @param bundle the bundle (such as org.openhab.core.thing or org.openhab.binding.matter)
     * @param actor the actor
     * @return the final source string
     */
    public static String buildSource(String bundle, @Nullable String actor) {
        if (bundle.contains(ACTOR_SEPARATOR)) {
            throw new IllegalArgumentException("Bundle must not contain the actor separator '" + ACTOR_SEPARATOR + "'");
        }
        if (bundle.contains(DELEGATION_SEPARATOR)) {
            throw new IllegalArgumentException(
                    "Bundle must not contain the delegation separator '" + DELEGATION_SEPARATOR + "'");
        }

        if (actor == null || actor.isEmpty()) {
            return bundle;
        }

        actor = actor.replace(DELEGATION_ESCAPE, DELEGATION_ESCAPE + DELEGATION_ESCAPE);
        actor = actor.replace(DELEGATION_SEPARATOR, DELEGATION_ESCAPE);
        return bundle + ACTOR_SEPARATOR + actor;
    }

    /**
     * Utility method to build a delegated source string from an original source and a bundle
     *
     * @param originalSource the original source (may be null)
     * @param bundle the bundle (such as org.openhab.core.thing or org.openhab.binding.matter)
     * @return the final source string
     */
    public static String buildDelegatedSource(@Nullable String originalSource, String bundle) {
        if (originalSource == null || originalSource.isEmpty()) {
            return bundle;
        }
        return originalSource + DELEGATION_SEPARATOR + bundle;
    }

    /**
     * Utility method to build a delegated source string from an original source, a bundle and an optional actor.
     *
     * @param originalSource the original source (may be null)
     * @param bundle the bundle (such as org.openhab.core.thing or org.openhab.binding.matter)
     * @param actor the actor
     * @return the final source string
     */
    public static String buildDelegatedSource(@Nullable String originalSource, String bundle, @Nullable String actor) {
        return buildDelegatedSource(originalSource, buildSource(bundle, actor));
    }
}
