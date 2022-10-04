/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.module;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.internal.RuleEngineImpl;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.test.storage.VolatileStorageService;
import org.openhab.core.types.UnDefType;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the VetoCommandAction
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class VetoCommandModuleTest extends JavaOSGiTest {

    private final Logger logger = LoggerFactory.getLogger(VetoCommandModuleTest.class);
    private final VolatileStorageService volatileStorageService = new VolatileStorageService();

    @BeforeEach
    public void before() {
        registerService(new ItemProvider() {
            @Override
            public void addProviderChangeListener(final ProviderChangeListener<Item> listener) {
            }

            @Override
            public void removeProviderChangeListener(final ProviderChangeListener<Item> listener) {
            }

            @Override
            public Collection<Item> getAll() {
                return List.of(new SwitchItem("switch1"));
            }
        });
        registerService(volatileStorageService);

        // start rule engine
        RuleEngineImpl ruleEngine = Objects.requireNonNull((RuleEngineImpl) getService(RuleManager.class));
        ruleEngine.onReadyMarkerAdded(new ReadyMarker("", ""));
        waitForAssert(() -> assertTrue(ruleEngine.isStarted()));
    }

    private Rule createRule() {
        final Configuration triggerConfig = new Configuration(Map.ofEntries(entry("itemName", "switch1")));

        final Rule vetoRule = RuleBuilder.create("exampleVetoRule")
                .withTriggers(ModuleBuilder.createTrigger().withId("trigger1").withTypeUID("core.ItemCommandTrigger")
                        .withConfiguration(triggerConfig).build())
                .withActions(ModuleBuilder.createAction().withId("action1").withTypeUID("core.VetoCommandAction")
                        .withInputs(Map.ofEntries(entry("event", "trigger1.event"))).build())
                .withName("Example Veto").withSynchronous(true).build();

        return vetoRule;
    }

    @Test
    public void commandVetoed() throws ItemNotFoundException, InterruptedException {
        final RuleRegistry ruleRegistry = getService(RuleRegistry.class);
        final RuleManager ruleEngine = getService(RuleManager.class);
        assertNotNull(ruleRegistry);

        final Rule vetoRule = createRule();
        logger.info("vetoRule created: {}", vetoRule.getUID());

        ruleRegistry.add(vetoRule);
        ruleEngine.setEnabled(vetoRule.getUID(), true);
        waitForAssert(() -> {
            assertEquals(RuleStatus.IDLE, ruleEngine.getStatusInfo(vetoRule.getUID()).getStatus());
        });

        final EventPublisher eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);

        final ItemRegistry itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        final Queue<Event> events = new LinkedList<>();
        subscribeToEvents(ItemCommandEvent.TYPE, events);

        // trigger rule
        eventPublisher.post(ItemEventFactory.createCommandEvent("switch1", OnOffType.ON));
        waitForAssert(() -> {
            assertFalse(events.isEmpty());
            ItemCommandEvent event = (ItemCommandEvent) events.remove();
            assertEquals("openhab/items/switch1/command", event.getTopic());
            assertEquals(OnOffType.ON, event.getItemCommand());
        });

        // wait a few milliseconds for any further commands to process
        Thread.sleep(100);

        final Item switch1 = itemRegistry.get("switch1");
        // ensure the item didn't turn on
        assertEquals(UnDefType.NULL, switch1.getState());
    }

    private void subscribeToEvents(String eventType, final Queue<Event> events) {
        EventSubscriber eventSubscriber = new EventSubscriber() {
            @Override
            public void receive(final Event event) {
                logger.info("Event: {}", event.getTopic());
                events.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(eventType);
            }

            @Override
            public @Nullable EventFilter getEventFilter() {
                return null;
            }
        };

        ServiceReference<?> subscriberReference = registerService(eventSubscriber).getReference();
        assertNotNull(getServices(EventSubscriber.class, (reference) -> reference.equals(subscriberReference)));
    }
}
