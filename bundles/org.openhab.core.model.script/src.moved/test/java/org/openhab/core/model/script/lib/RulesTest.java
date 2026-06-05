/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.script.lib;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.scheduler.Scheduler;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;

/**
 * Tests for {@link Rules}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class RulesTest {

    private static final String EXISTING_RULE_UID = "existing-rule";
    private static final String UNKNOWN_RULE_UID = "unknown-rule";

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistry;
    private @Mock @NonNullByDefault({}) ThingRegistry thingRegistry;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisher;
    private @Mock @NonNullByDefault({}) ModelRepository modelRepository;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistry;
    private @Mock @NonNullByDefault({}) RuleRegistry ruleRegistry;
    private @Mock @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @Mock @NonNullByDefault({}) TimeZoneProvider timeZoneProvider;
    private @Mock @NonNullByDefault({}) LocaleProvider localeProvider;
    private @Mock @NonNullByDefault({}) Scheduler scheduler;
    private @Mock @NonNullByDefault({}) Rule existingRule;

    private @Mock @NonNullByDefault({}) RuleManager ruleManager;

    private @NonNullByDefault({}) ScriptServiceUtil serviceUtil;

    private @NonNullByDefault({}) Map<@NonNull String, @Nullable Object> threeArgMap;

    @BeforeEach
    public void setUp() throws Exception {
        when(existingRule.getUID()).thenReturn(EXISTING_RULE_UID);
        when(ruleRegistry.get(EXISTING_RULE_UID)).thenReturn(existingRule);
        when(ruleManager.isEnabled(EXISTING_RULE_UID)).thenReturn(Boolean.TRUE);
        when(ruleManager.isEnabled(UNKNOWN_RULE_UID)).thenReturn(null);
        when(ruleManager.getStatus(EXISTING_RULE_UID)).thenReturn(RuleStatus.IDLE);
        when(ruleManager.runNow(EXISTING_RULE_UID)).thenReturn(Map.of("result", "Success"));
        when(ruleManager.runNow(EXISTING_RULE_UID, true, null)).thenReturn(Map.of("result", "ConditionalSuccess"));
        when(ruleManager.runNow(EXISTING_RULE_UID, false, Map.of("arg1", "yes")))
                .thenReturn(Map.of("result", "OneArgSuccess"));
        when(ruleManager.runNow(EXISTING_RULE_UID, false, Map.of("arg1", "yes", "arg2", Boolean.FALSE)))
                .thenReturn(Map.of("result", "TwoArgSuccess"));
        threeArgMap = new HashMap<>(Map.of("arg1", "yes", "arg2", Boolean.FALSE));
        threeArgMap.put("arg3", null);
        when(ruleManager.runNow(EXISTING_RULE_UID, false, threeArgMap)).thenReturn(Map.of("result", "ThreeArgSuccess"));

        nullScriptServiceUtilInstance();
        serviceUtil = new ScriptServiceUtil(itemRegistry, thingRegistry, eventPublisher, modelRepository,
                metadataRegistry, ruleRegistry, itemChannelLinkRegistry, timeZoneProvider, localeProvider, scheduler);
        setRuleManagerField(serviceUtil, ruleManager);
    }

    @Test
    public void testGetRule() {
        assertNull(Rules.getRule(UNKNOWN_RULE_UID));
        assertThat(Rules.getRule(EXISTING_RULE_UID), is(existingRule));
    }

    @Test
    public void testGetRuleManager() {
        assertThat(Rules.getRuleManager(), is(ruleManager));
    }

    @Test
    public void testRunRule() throws Exception {
        Map<String, @Nullable Object> result = Rules.runRule(EXISTING_RULE_UID);
        assertThat(result.get("result"), is("Success"));
        assertThrows(IllegalArgumentException.class, () -> Rules.runRule(UNKNOWN_RULE_UID));

        result = Rules.runRule(EXISTING_RULE_UID, true);
        assertThat(result.get("result"), is("ConditionalSuccess"));
        assertThrows(IllegalArgumentException.class, () -> Rules.runRule(UNKNOWN_RULE_UID, true));

        result = Rules.runRule(EXISTING_RULE_UID, Map.of("arg1", "yes"));
        assertThat(result.get("result"), is("OneArgSuccess"));
        assertThrows(IllegalArgumentException.class, () -> Rules.runRule(UNKNOWN_RULE_UID, Map.of("arg1", "yes")));

        result = Rules.runRule(EXISTING_RULE_UID, false, "arg1", "yes");
        assertThat(result.get("result"), is("OneArgSuccess"));
        result = Rules.runRule(EXISTING_RULE_UID, false, "arg1", "yes", "arg2", Boolean.FALSE);
        assertThat(result.get("result"), is("TwoArgSuccess"));
        result = Rules.runRule(EXISTING_RULE_UID, false, "arg1", "yes", "arg2", Boolean.FALSE, "arg3", null);
        assertThat(result.get("result"), is("ThreeArgSuccess"));
        assertThrows(IllegalArgumentException.class, () -> Rules.runRule(UNKNOWN_RULE_UID, false, "arg1", "yes"));

        result = Rules.runRule(EXISTING_RULE_UID, false, Map.of("arg1", "yes"));
        assertThat(result.get("result"), is("OneArgSuccess"));
        result = Rules.runRule(EXISTING_RULE_UID, false, Map.of("arg1", "yes", "arg2", Boolean.FALSE));
        assertThat(result.get("result"), is("TwoArgSuccess"));
        result = Rules.runRule(EXISTING_RULE_UID, false, threeArgMap);
        assertThat(result.get("result"), is("ThreeArgSuccess"));
        assertThrows(IllegalArgumentException.class,
                () -> Rules.runRule(UNKNOWN_RULE_UID, false, Map.of("arg1", "yes")));

        setRuleManagerField(serviceUtil, null);
        assertThrows(IllegalStateException.class, () -> Rules.runRule(EXISTING_RULE_UID));
        assertThrows(IllegalStateException.class, () -> Rules.runRule(EXISTING_RULE_UID, true));
        assertThrows(IllegalStateException.class, () -> Rules.runRule(EXISTING_RULE_UID, Map.of("arg1", "yes")));
        assertThrows(IllegalStateException.class, () -> Rules.runRule(EXISTING_RULE_UID, false, "arg1", "yes"));
        assertThrows(IllegalStateException.class, () -> Rules.runRule(EXISTING_RULE_UID, true, Map.of("arg1", "yes")));
    }

    @Test
    public void testIsRuleEnabled() throws Exception {
        assertThat(Rules.isRuleEnabled(EXISTING_RULE_UID), is(true));
        assertThrows(IllegalArgumentException.class, () -> Rules.isRuleEnabled(UNKNOWN_RULE_UID));
        setRuleManagerField(serviceUtil, null);
        assertThrows(IllegalStateException.class, () -> Rules.isRuleEnabled(EXISTING_RULE_UID));
    }

    @Test
    public void testSetRuleEnabled() throws Exception {
        assertDoesNotThrow(() -> Rules.setRuleEnabled(EXISTING_RULE_UID, false));
        setRuleManagerField(serviceUtil, null);
        assertThrows(IllegalStateException.class, () -> Rules.setRuleEnabled(EXISTING_RULE_UID, false));
    }

    private void nullScriptServiceUtilInstance() throws Exception {
        Field field = ScriptServiceUtil.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    private void setRuleManagerField(ScriptServiceUtil instance, @Nullable RuleManager ruleManager) throws Exception {
        Field field = ScriptServiceUtil.class.getDeclaredField("ruleManager");
        field.setAccessible(true);
        field.set(instance, ruleManager);
    }
}
