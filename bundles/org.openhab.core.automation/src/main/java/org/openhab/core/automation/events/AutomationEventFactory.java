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
package org.openhab.core.automation.events;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEventFactory;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a factory that creates Timer and Execution Events.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = EventFactory.class, immediate = true)
public class AutomationEventFactory extends AbstractEventFactory {
    private static final String MODULE_IDENTIFIER = "{moduleId}";
    private static final String TIMER_EVENT_TOPIC = "openhab/timer/" + MODULE_IDENTIFIER + "/triggered";
    private static final String EXECUTION_EVENT_TOPIC = "openhab/execution/" + MODULE_IDENTIFIER + "/triggered";

    private final Logger logger = LoggerFactory.getLogger(AutomationEventFactory.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of(TimerEvent.TYPE, ExecutionEvent.TYPE);

    public AutomationEventFactory() {
        super(SUPPORTED_TYPES);
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, @Nullable String source)
            throws Exception {
        logger.trace("creating ruleEvent of type: {}", eventType);
        if (TimerEvent.TYPE.equals(eventType)) {
            return createTimerEvent(topic, payload, Objects.requireNonNullElse(source, "<unknown>"));
        } else if (ExecutionEvent.TYPE.equals(eventType)) {
            if (source == null) {
                throw new IllegalArgumentException("'source' must not be null for execution events");
            }
            return createExecutionEvent(topic, payload, source);
        }
        throw new IllegalArgumentException("The event type '" + eventType + "' is not supported by this factory.");
    }

    private Event createTimerEvent(String topic, String payload, String source) {
        return new TimerEvent(topic, payload, source);
    }

    private Event createExecutionEvent(String topic, String payload, String source) {
        return new ExecutionEvent(topic, payload, source);
    }

    /**
     * Creates a {@link TimerEvent}
     *
     * @param moduleId the module type id of this event
     * @param label The label (or id) of this object
     * @param configuration the configuration of the trigger
     * @return the created event
     */
    public static TimerEvent createTimerEvent(String moduleId, @Nullable String label,
            Map<String, Object> configuration) {
        String topic = TIMER_EVENT_TOPIC.replace(MODULE_IDENTIFIER, moduleId);
        String payload = serializePayload(configuration);
        return new TimerEvent(topic, payload, label);
    }

    /**
     * Creates an {@link ExecutionEvent}
     *
     * @param moduleId the module type id of this event
     * @param payload A map with additional information like preceding events when rules are called from other rules
     *            (optional)
     * @param source The source of this event (e.g. "script" or "manual")
     * @return the created event
     */
    public static ExecutionEvent createExecutionEvent(String moduleId, @Nullable Map<String, Object> payload,
            String source) {
        String topic = EXECUTION_EVENT_TOPIC.replace(MODULE_IDENTIFIER, moduleId);
        String serializedPayload = serializePayload(Objects.requireNonNullElse(payload, Map.of()));
        return new ExecutionEvent(topic, serializedPayload, source);
    }
}
