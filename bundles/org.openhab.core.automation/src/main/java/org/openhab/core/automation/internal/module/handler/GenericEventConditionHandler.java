/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module.handler;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.handler.BaseConditionModuleHandler;
import org.openhab.core.events.Event;
import org.openhab.core.events.TopicGlobEventFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the implementation of an event condition which checks if inputs matches configured values.
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Kai Kreuzer - refactored and simplified customized module handling
 * @author Cody Cutrer - refactored to match configuration and semantics of GenericEventTriggerHandler
 */
@NonNullByDefault
public class GenericEventConditionHandler extends BaseConditionModuleHandler {

    public static final String MODULETYPE_ID = "core.GenericEventCondition";

    public static final String CFG_TOPIC = "topic";
    public static final String CFG_TYPES = "types";
    public static final String CFG_SOURCE = "source";
    public static final String CFG_PAYLOAD = "payload";

    public final Logger logger = LoggerFactory.getLogger(GenericEventConditionHandler.class);

    private final String source;
    private final @Nullable TopicGlobEventFilter topicFilter;
    private final Set<String> types;
    private final @Nullable Pattern payloadPattern;

    public GenericEventConditionHandler(Condition module) {
        super(module);

        this.source = (String) module.getConfiguration().get(CFG_SOURCE);
        String topic = (String) module.getConfiguration().get(CFG_TOPIC);
        if (!topic.isBlank()) {
            topicFilter = new TopicGlobEventFilter(topic);
        } else {
            topicFilter = null;
        }
        if (module.getConfiguration().get(CFG_TYPES) != null) {
            this.types = Set.of(((String) module.getConfiguration().get(CFG_TYPES)).split(","));
        } else {
            this.types = Set.of();
        }
        String payload = (String) module.getConfiguration().get(CFG_PAYLOAD);
        if (!payload.isBlank()) {
            payloadPattern = Pattern.compile(payload);
        } else {
            payloadPattern = null;
        }
    }

    @Override
    public boolean isSatisfied(Map<String, Object> inputs) {
        Event event = inputs.get("event") instanceof Event ? (Event) inputs.get("event") : null;
        if (event == null) {
            return false;
        }
        if (!types.isEmpty() && !types.contains(event.getType())) {
            return false;
        }
        TopicGlobEventFilter localTopicFilter = topicFilter;
        if (localTopicFilter != null && !localTopicFilter.apply(event)) {
            return false;
        }
        if (!source.isEmpty() && !source.equals(event.getSource())) {
            return false;
        }
        Pattern localPayloadPattern = payloadPattern;
        if (localPayloadPattern != null && !localPayloadPattern.matcher(event.getPayload()).find()) {
            return false;
        }

        return true;
    }
}
