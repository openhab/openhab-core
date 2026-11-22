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
package org.openhab.core.smoke.test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.core.service.StartLevelService.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.validation.ConfigValidationException;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.config.discovery.inbox.Inbox;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.DateTimeItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.State;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Exercises binding discovery, item commands, binding updates and rule execution in the same OSGi runtime.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Timeout(value = 90, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
public class MagicBindingSmokeTest extends JavaOSGiTest {

    private static final ThingTypeUID LIGHT_TYPE = new ThingTypeUID("magic:onoff-light");
    private static final ThingUID LIGHT = new ThingUID(LIGHT_TYPE, "smoke");
    private static final ChannelUID SWITCH = new ChannelUID(LIGHT, "switch");
    private static final ChannelUID TIMESTAMP = new ChannelUID(LIGHT, "timestamp");
    private static final ChannelUID TRIGGER = new ChannelUID(LIGHT, "trigger");
    private static final String RULE = "magicBindingSmoke";
    private static final long WAIT_MS = 15_000;
    private static final long POLL_MS = 50;

    @Test
    public void discoveredThingCanBeApprovedAndConfigured() {
        ThingRegistry things = requireNonNull(getService(ThingRegistry.class));
        ((AbstractRegistry<?, ?, ?>) things).waitForCompletedAsyncActivationTasks();
        ThingTypeRegistry thingTypes = requireNonNull(getService(ThingTypeRegistry.class));
        DiscoveryServiceRegistry discovery = requireNonNull(getService(DiscoveryServiceRegistry.class));
        Inbox inbox = requireNonNull(getService(Inbox.class));
        ThingTypeUID discoverableType = new ThingTypeUID("magic:configurable-thing");
        waitForAssert(() -> assertNotNull(thingTypes.getThingType(discoverableType)), WAIT_MS, POLL_MS);
        Set<ThingUID> existingResults = inbox.getAll().stream().map(DiscoveryResult::getThingUID)
                .collect(Collectors.toSet());

        assertTrue(discovery.startScan(discoverableType, null, null));
        DiscoveryResult result = waitForAssert(() -> {
            List<DiscoveryResult> newResults = inbox.getAll().stream()
                    .filter(entry -> discoverableType.equals(entry.getThingTypeUID()))
                    .filter(entry -> !existingResults.contains(entry.getThingUID())).toList();
            assertFalse(newResults.isEmpty(), "Magic scan result did not arrive in the inbox");
            return newResults.getFirst();
        }, WAIT_MS, POLL_MS);
        ThingUID uid = result.getThingUID();
        try {
            Object serialNumber = requireNonNull(result.getProperties().get(Thing.PROPERTY_SERIAL_NUMBER));
            Thing approved = inbox.approve(uid, null, null);
            assertNotNull(approved);
            waitForAssert(() -> {
                Thing registered = things.get(uid);
                assertNotNull(registered);
                assertEquals(ThingStatus.ONLINE, registered.getStatus());
                assertEquals(result.getLabel(), registered.getLabel());
                assertEquals(serialNumber, registered.getProperties().get(Thing.PROPERTY_SERIAL_NUMBER));
                assertTrue(inbox.getAll().stream().noneMatch(entry -> uid.equals(entry.getThingUID())));
            }, WAIT_MS, POLL_MS);

            BigDecimal interval = BigDecimal.valueOf(30);
            things.updateConfiguration(uid, Map.of("interval", interval));
            assertThrows(ConfigValidationException.class,
                    () -> things.updateConfiguration(uid, Map.of("interval", BigDecimal.ZERO)));
            Thing configured = requireNonNull(things.get(uid));
            assertEquals(interval, configured.getConfiguration().get("interval"));
            assertEquals(ThingStatus.ONLINE, configured.getStatus());
        } finally {
            inbox.remove(uid);
            things.forceRemove(uid);
        }
    }

    @Test
    public void routesCommandsAndRunsRulesAfterBindingRestart() throws Exception {
        ThingRegistry things = requireNonNull(getService(ThingRegistry.class));
        ItemRegistry items = requireNonNull(getService(ItemRegistry.class));
        ItemChannelLinkRegistry links = requireNonNull(getService(ItemChannelLinkRegistry.class));
        RuleRegistry rules = requireNonNull(getService(RuleRegistry.class));
        ThingTypeRegistry thingTypes = requireNonNull(getService(ThingTypeRegistry.class));
        EventPublisher events = requireNonNull(getService(EventPublisher.class));
        ReadyService ready = requireNonNull(getService(ReadyService.class));
        ConfigurationAdmin configAdmin = requireNonNull(getService(ConfigurationAdmin.class));
        Bundle magic = Arrays.stream(getBundleContext().getBundles())
                .filter(bundle -> "org.openhab.core.test.magic".equals(bundle.getSymbolicName())).findFirst()
                .orElseThrow();

        for (AbstractRegistry<?, ?, ?> registry : List.of((AbstractRegistry<?, ?, ?>) things,
                (AbstractRegistry<?, ?, ?>) items, links, (AbstractRegistry<?, ?, ?>) rules)) {
            registry.waitForCompletedAsyncActivationTasks();
        }

        // Use the normal start-level service with only the levels needed by this runtime.
        var startLevels = configAdmin.getConfiguration("org.openhab.startlevel", "?");
        SwitchItem lightSwitch = new SwitchItem("SmokeLightSwitch");
        DateTimeItem timestamp = new DateTimeItem("SmokeLightTimestamp");
        SwitchItem ruleResult = new SwitchItem("SmokeLightRuleResult");
        ItemChannelLink switchLink = new ItemChannelLink(lightSwitch.getName(), SWITCH, new Configuration());
        ItemChannelLink timestampLink = new ItemChannelLink(timestamp.getName(), TIMESTAMP, new Configuration());
        ReadyMarker ruleEngineReady = new ReadyMarker(STARTLEVEL_MARKER_TYPE, Integer.toString(STARTLEVEL_RULEENGINE));
        try {
            startLevels.update(new Hashtable<>(Map.of(Integer.toString(STARTLEVEL_STATES), "managed:item,managed:thing",
                    Integer.toString(STARTLEVEL_RULES), "things:handler", Integer.toString(STARTLEVEL_RULEENGINE),
                    "ruleengine:start")));
            waitForAssert(() -> assertTrue(ready.isReady(ruleEngineReady), "Rule engine start level was not reached"),
                    30_000, POLL_MS);
            RuleManager ruleManager = requireNonNull(getService(RuleManager.class));
            waitForAssert(() -> assertNotNull(thingTypes.getThingType(LIGHT_TYPE)), WAIT_MS, POLL_MS);
            Thing light = things.createThingOfType(LIGHT_TYPE, LIGHT, null, null, new Configuration());
            assertNotNull(light);
            assertNotNull(light.getChannel(SWITCH.getId()));
            assertNotNull(light.getChannel(TIMESTAMP.getId()));
            assertNotNull(light.getChannel(TRIGGER.getId()));
            items.add(lightSwitch);
            items.add(timestamp);
            items.add(ruleResult);
            // Install the links before the handler can issue a REFRESH on link creation.
            links.add(switchLink);
            links.add(timestampLink);
            things.add(light);

            rules.add(RuleBuilder.create(RULE)
                    .withTriggers(ModuleBuilder.createTrigger().withId("event").withTypeUID("core.ChannelEventTrigger")
                            .withConfiguration(
                                    new Configuration(Map.of("channelUID", TRIGGER.getAsString(), "event", "ON")))
                            .build())
                    .withActions(ModuleBuilder.createAction().withId("record").withTypeUID("core.ItemStateUpdateAction")
                            .withConfiguration(
                                    new Configuration(Map.of("itemName", ruleResult.getName(), "state", "ON")))
                            .build())
                    .build());
            waitForAssert(() -> assertEquals(RuleStatus.IDLE, ruleManager.getStatus(RULE)), WAIT_MS, POLL_MS);
            waitForAssert(() -> assertEquals(ThingStatus.ONLINE, light.getStatus()), WAIT_MS, POLL_MS);
            assertCommandRoundTrip(events, lightSwitch, timestamp, ruleResult);

            try {
                magic.stop();
                waitForAssert(() -> {
                    assertEquals(ThingStatus.UNINITIALIZED, light.getStatus());
                    assertNull(light.getHandler());
                }, WAIT_MS, POLL_MS);
                events.post(ItemEventFactory.createStateEvent(ruleResult.getName(), OnOffType.OFF));
                waitForAssert(() -> assertEquals(OnOffType.OFF, ruleResult.getState()), WAIT_MS, POLL_MS);
            } finally {
                magic.start();
            }
            waitForAssert(() -> assertEquals(ThingStatus.ONLINE, light.getStatus()), WAIT_MS, POLL_MS);
            assertCommandRoundTrip(events, lightSwitch, timestamp, ruleResult);
        } finally {
            rules.remove(RULE);
            links.remove(switchLink.getUID());
            links.remove(timestampLink.getUID());
            things.forceRemove(LIGHT);
            items.remove(ruleResult.getName());
            items.remove(timestamp.getName());
            items.remove(lightSwitch.getName());
            startLevels.delete();
        }
    }

    private void assertCommandRoundTrip(EventPublisher events, SwitchItem lightSwitch, DateTimeItem timestamp,
            SwitchItem ruleResult) {
        State previousTimestamp = timestamp.getState();
        events.post(ItemEventFactory.createCommandEvent(lightSwitch.getName(), OnOffType.ON));
        waitForAssert(() -> {
            DateTimeType updatedTimestamp = assertInstanceOf(DateTimeType.class, timestamp.getState());
            assertNotEquals(previousTimestamp, updatedTimestamp);
        }, WAIT_MS, POLL_MS);
        waitForAssert(() -> assertEquals(OnOffType.ON, ruleResult.getState()), WAIT_MS, POLL_MS);
    }
}
