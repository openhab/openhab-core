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
package org.openhab.core.automation.module.script.internal.defaultscope;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.automation.RuleStatus;
import org.openhab.core.automation.RuleStatusInfo;
import org.openhab.core.automation.Trigger;
import org.openhab.core.common.registry.ProviderChangeListener;
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
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.test.storage.VolatileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This tests the script modules
 *
 * @author Kai Kreuzer - Initial contribution
 */

public class ScriptRuleOSGiTest extends JavaOSGiTest {

    private final Logger logger = LoggerFactory.getLogger(ScriptRuleOSGiTest.class);
    private VolatileStorageService volatileStorageService = new VolatileStorageService();

    private ItemCommandEvent receivedEvent;

    public ScriptRuleOSGiTest() {
    }

    @BeforeEach
    public void before() {
        @NonNullByDefault
        ItemProvider itemProvider = new ItemProvider() {
            @Override
            public void addProviderChangeListener(ProviderChangeListener<Item> listener) {
            }

            @Override
            public Collection<Item> getAll() {
                return List.of(new SwitchItem("MyTrigger"), new SwitchItem("ScriptItem"));
            }

            @Override
            public void removeProviderChangeListener(ProviderChangeListener<Item> listener) {
            }
        };
        registerService(itemProvider);
        registerService(volatileStorageService);

        EventSubscriber eventSubscriber = new EventSubscriber() {
            @Override
            public void receive(Event event) {
                receivedEvent = (ItemCommandEvent) event;
                logger.info("received event from item {}, command {}", receivedEvent.getItemName(),
                        receivedEvent.getItemCommand());
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                return Set.of(ItemCommandEvent.TYPE);
            }

            @Override
            public EventFilter getEventFilter() {
                return null;
            }
        };
        registerService(eventSubscriber);
    }

    // ignore - wip @Test
    public void testPredefinedRule() throws ItemNotFoundException {
        EventPublisher eventPublisher = getService(EventPublisher.class);
        ItemRegistry itemRegistry = getService(ItemRegistry.class);
        RuleRegistry ruleRegistry = getService(RuleRegistry.class);
        RuleManager ruleEngine = getService(RuleManager.class);

        // WAIT until Rule modules types are parsed and the rule becomes IDLE
        waitForAssert(() -> {
            assertThat(ruleRegistry.getAll().isEmpty(), is(false));
            Rule rule2 = ruleRegistry.get("javascript.rule1");
            assertThat(rule2, is(notNullValue()));
            RuleStatusInfo ruleStatus2 = ruleEngine.getStatusInfo(rule2.getUID());
            assertThat(ruleStatus2, is(notNullValue()));
            assertThat(ruleStatus2.getStatus(), is(RuleStatus.IDLE));
        }, 5000, 200);
        Rule rule = ruleRegistry.get("javascript.rule1");
        assertThat(rule, is(notNullValue()));
        assertThat(rule.getName(), is("DemoScriptRule"));
        Optional<? extends Trigger> trigger = rule.getTriggers().stream().filter(t -> "trigger".equals(t.getId()))
                .findFirst();
        assertThat(trigger.isPresent(), is(true));
        assertThat(trigger.get().getTypeUID(), is("core.GenericEventTrigger"));
        assertThat(trigger.get().getConfiguration().get("eventSource"), is("MyTrigger"));
        assertThat(trigger.get().getConfiguration().get("eventTopic"), is("openhab/items/MyTrigger/state"));
        assertThat(trigger.get().getConfiguration().get("eventTypes"), is("ItemStateEvent"));
        Optional<? extends Condition> condition1 = rule.getConditions().stream()
                .filter(c -> "condition".equals(c.getId())).findFirst();
        assertThat(condition1.isPresent(), is(true));
        assertThat(condition1.get().getTypeUID(), is("script.ScriptCondition"));
        assertThat(condition1.get().getConfiguration().get("type"), is("application/javascript"));
        assertThat(condition1.get().getConfiguration().get("script"), is("event.itemState==ON"));
        Optional<? extends Action> action = rule.getActions().stream().filter(a -> "action".equals(a.getId()))
                .findFirst();
        assertThat(action.isPresent(), is(true));
        assertThat(action.get().getTypeUID(), is("script.ScriptAction"));
        assertThat(action.get().getConfiguration().get("type"), is("application/javascript"));
        assertThat(action.get().getConfiguration().get("script"), is(
                "print(items.MyTrigger), print(things.getAll()), print(ctx.get('trigger.event')), events.sendCommand('ScriptItem', 'ON')"));
        RuleStatusInfo ruleStatus = ruleEngine.getStatusInfo(rule.getUID());
        assertThat(ruleStatus.getStatus(), is(RuleStatus.IDLE));

        SwitchItem myTriggerItem = (SwitchItem) itemRegistry.getItem("MyTrigger");
        logger.info("Triggering item: {}", myTriggerItem.getName());
        eventPublisher.post(ItemEventFactory.createStateEvent("MyTrigger", OnOffType.ON));

        waitForAssert(() -> {
            assertThat(receivedEvent, notNullValue());
        });
        assertThat(receivedEvent.getItemName(), is(equalTo("ScriptItem")));
        assertThat(receivedEvent.getItemCommand(), is(OnOffType.ON));
    }
}
