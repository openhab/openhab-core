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
package org.openhab.core.automation.internal;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.dto.RuleDTO;
import org.openhab.core.automation.dto.RuleDTOMapper;
import org.openhab.core.automation.events.RuleAddedEvent;
import org.openhab.core.automation.events.RuleRemovedEvent;
import org.openhab.core.automation.events.RuleStatusInfoEvent;
import org.openhab.core.automation.events.RuleUpdatedEvent;
import org.openhab.core.events.AbstractEventFactory;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a factory that creates Rule Events.
 *
 * @author Benedikt Niehues - Initial contribution
 * @author Markus Rathgeb - Use the DTO for the Rule representation
 */
@Component(service = EventFactory.class, immediate = true)
public class RuleEventFactory extends AbstractEventFactory {

    private final Logger logger = LoggerFactory.getLogger(RuleEventFactory.class);

    private static final String RULE_STATE_EVENT_TOPIC = "openhab/rules/{ruleID}/state";

    private static final String RULE_ADDED_EVENT_TOPIC = "openhab/rules/{ruleID}/added";

    private static final String RULE_REMOVED_EVENT_TOPIC = "openhab/rules/{ruleID}/removed";

    private static final String RULE_UPDATED_EVENT_TOPIC = "openhab/rules/{ruleID}/updated";

    private static final Set<String> SUPPORTED_TYPES = new HashSet<>();

    static {
        SUPPORTED_TYPES.add(RuleAddedEvent.TYPE);
        SUPPORTED_TYPES.add(RuleRemovedEvent.TYPE);
        SUPPORTED_TYPES.add(RuleStatusInfoEvent.TYPE);
        SUPPORTED_TYPES.add(RuleUpdatedEvent.TYPE);
    }

    public RuleEventFactory() {
        super(SUPPORTED_TYPES);
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, String source) throws Exception {
        logger.trace("creating ruleEvent of type: {}", eventType);
        if (RuleAddedEvent.TYPE.equals(eventType)) {
            return createRuleAddedEvent(topic, payload, source);
        } else if (RuleRemovedEvent.TYPE.equals(eventType)) {
            return createRuleRemovedEvent(topic, payload, source);
        } else if (RuleStatusInfoEvent.TYPE.equals(eventType)) {
            return createRuleStatusInfoEvent(topic, payload, source);
        } else if (RuleUpdatedEvent.TYPE.equals(eventType)) {
            return createRuleUpdatedEvent(topic, payload, source);
        }
        throw new IllegalArgumentException("The event type '" + eventType + "' is not supported by this factory.");
    }

    private Event createRuleUpdatedEvent(String topic, String payload, String source) {
        RuleDTO[] ruleDTO = deserializePayload(payload, RuleDTO[].class);
        if (ruleDTO.length != 2) {
            throw new IllegalArgumentException("Creation of RuleUpdatedEvent failed: invalid payload: " + payload);
        }
        return new RuleUpdatedEvent(topic, payload, source, ruleDTO[0], ruleDTO[1]);
    }

    private Event createRuleStatusInfoEvent(String topic, String payload, String source) {
        RuleStatusInfo statusInfo = deserializePayload(payload, RuleStatusInfo.class);
        return new RuleStatusInfoEvent(topic, payload, source, statusInfo, getRuleId(topic));
    }

    private Event createRuleRemovedEvent(String topic, String payload, String source) {
        RuleDTO ruleDTO = deserializePayload(payload, RuleDTO.class);
        return new RuleRemovedEvent(topic, payload, source, ruleDTO);
    }

    private Event createRuleAddedEvent(String topic, String payload, String source) {
        RuleDTO ruleDTO = deserializePayload(payload, RuleDTO.class);
        return new RuleAddedEvent(topic, payload, source, ruleDTO);
    }

    private String getRuleId(String topic) {
        String[] topicElements = getTopicElements(topic);
        if (topicElements.length != 4) {
            throw new IllegalArgumentException("Event creation failed, invalid topic: " + topic);
        }
        return topicElements[2];
    }

    /**
     * Creates a rule updated event.
     *
     * @param rule the new rule.
     * @param oldRule the rule that has been updated.
     * @param source the source of the event.
     * @return {@link RuleUpdatedEvent} instance.
     */
    public static RuleUpdatedEvent createRuleUpdatedEvent(Rule rule, Rule oldRule, String source) {
        String topic = buildTopic(RULE_UPDATED_EVENT_TOPIC, rule);
        final RuleDTO ruleDto = RuleDTOMapper.map(rule);
        final RuleDTO oldRuleDto = RuleDTOMapper.map(oldRule);
        List<RuleDTO> rules = new LinkedList<>();
        rules.add(ruleDto);
        rules.add(oldRuleDto);
        String payload = serializePayload(rules);
        return new RuleUpdatedEvent(topic, payload, source, ruleDto, oldRuleDto);
    }

    /**
     * Creates a rule status info event.
     *
     * @param statusInfo the status info of the event.
     * @param ruleUID the UID of the rule for which the event is created.
     * @param source the source of the event.
     * @return {@link RuleStatusInfoEvent} instance.
     */
    public static RuleStatusInfoEvent createRuleStatusInfoEvent(RuleStatusInfo statusInfo, String ruleUID,
            String source) {
        String topic = buildTopic(RULE_STATE_EVENT_TOPIC, ruleUID);
        String payload = serializePayload(statusInfo);
        return new RuleStatusInfoEvent(topic, payload, source, statusInfo, ruleUID);
    }

    /**
     * Creates a rule removed event.
     *
     * @param rule the rule for which this event is created.
     * @param source the source of the event.
     * @return {@link RuleRemovedEvent} instance.
     */
    public static RuleRemovedEvent createRuleRemovedEvent(Rule rule, String source) {
        String topic = buildTopic(RULE_REMOVED_EVENT_TOPIC, rule);
        final RuleDTO ruleDto = RuleDTOMapper.map(rule);
        String payload = serializePayload(ruleDto);
        return new RuleRemovedEvent(topic, payload, source, ruleDto);
    }

    /**
     * Creates a rule added event.
     *
     * @param rule the rule for which this event is created.
     * @param source the source of the event.
     * @return {@link RuleAddedEvent} instance.
     */
    public static RuleAddedEvent createRuleAddedEvent(Rule rule, String source) {
        String topic = buildTopic(RULE_ADDED_EVENT_TOPIC, rule);
        final RuleDTO ruleDto = RuleDTOMapper.map(rule);
        String payload = serializePayload(ruleDto);
        return new RuleAddedEvent(topic, payload, source, ruleDto);
    }

    private static String buildTopic(String topic, String ruleUID) {
        return topic.replace("{ruleID}", ruleUID);
    }

    private static String buildTopic(String topic, Rule rule) {
        return buildTopic(topic, rule.getUID());
    }
}
