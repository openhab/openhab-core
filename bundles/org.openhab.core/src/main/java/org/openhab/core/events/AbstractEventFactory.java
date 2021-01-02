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
package org.openhab.core.events;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.Gson;

/**
 * The {@link AbstractEventFactory} defines an abstract implementation of the {@link EventFactory} interface. Subclasses
 * must implement the abstract method {@link #createEventByType(String, String, String, String)} in order to create
 * event
 * instances based on the event type.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractEventFactory implements EventFactory {

    private final Set<String> supportedEventTypes;

    private static final Gson JSONCONVERTER = new Gson();

    /**
     * Must be called in subclass constructor to define the supported event types.
     *
     * @param supportedEventTypes the supported event types
     */
    public AbstractEventFactory(Set<String> supportedEventTypes) {
        this.supportedEventTypes = Set.copyOf(supportedEventTypes);
    }

    @Override
    public Event createEvent(String eventType, String topic, String payload, @Nullable String source) throws Exception {
        assertValidArguments(eventType, topic, payload);

        if (!getSupportedEventTypes().contains(eventType)) {
            throw new IllegalArgumentException("The event type '" + eventType + "' is not supported by this factory.");
        } else {
            return createEventByType(eventType, topic, payload, source);
        }
    }

    @Override
    public Set<String> getSupportedEventTypes() {
        return supportedEventTypes;
    }

    private void assertValidArguments(String eventType, String topic, String payload) {
        checkNotNullOrEmpty(eventType, "eventType");
        checkNotNullOrEmpty(topic, "topic");
        checkNotNullOrEmpty(payload, "payload");
    }

    /**
     * Create a new event instance based on the event type.
     *
     * @param eventType the event type
     * @param topic the topic
     * @param payload the payload
     * @param source the source, can be null
     * @return the created event instance
     * @throws Exception if the creation of the event fails
     */
    protected abstract Event createEventByType(String eventType, String topic, String payload, @Nullable String source)
            throws Exception;

    /**
     * Serializes the payload object into its equivalent Json representation.
     *
     * @param payloadObject the payload object to serialize
     * @return a serialized Json representation
     */
    protected static String serializePayload(Object payloadObject) {
        return JSONCONVERTER.toJson(payloadObject);
    }

    /**
     * Deserializes the Json-payload into an object of the specified class.
     *
     * @param payload the payload from which the object is to be deserialized
     * @param classOfPayload the class T of the payload object
     * @param <T> the type of the returned object
     * @return an object of type T from the payload
     */
    protected static <T> T deserializePayload(String payload, Class<T> classOfPayload) {
        return JSONCONVERTER.fromJson(payload, classOfPayload);
    }

    /**
     * Gets the elements of the topic (splitted by '/').
     *
     * @param topic the topic
     * @return the topic elements
     */
    protected String[] getTopicElements(String topic) {
        return topic.split("/");
    }

    protected static void checkNotNull(@Nullable Object object, String argumentName) {
        if (object == null) {
            throw new IllegalArgumentException("The argument '" + argumentName + "' must not be null.");
        }
    }

    protected static void checkNotNullOrEmpty(@Nullable String string, String argumentName) {
        if (string == null || string.isEmpty()) {
            throw new IllegalArgumentException("The argument '" + argumentName + "' must not be null or empty.");
        }
    }
}
