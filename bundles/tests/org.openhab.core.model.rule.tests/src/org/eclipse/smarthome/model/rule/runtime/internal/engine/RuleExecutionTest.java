/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.rule.runtime.internal.engine;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.events.EventFilter;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.events.EventSubscriber;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.GroupItem;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemEventFactory;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.model.rule.rules.Rule;
import org.eclipse.smarthome.model.rule.runtime.RuleEngine;
import org.eclipse.smarthome.model.rule.runtime.internal.engine.RuleTriggerManager.TriggerTypes;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Kai Kreuzer - initial contribution and API.
 */
public class RuleExecutionTest extends JavaOSGiTest {

    private final static String TESTMODEL_NAME = "testModel.rules";
    private ModelRepository modelRepository;
    private ItemRegistry itemRegistry;
    private EventPublisher eventPublisher;
    private Event resultEvent;

    private final EventSubscriber eventSubscriber = new EventSubscriber() {

        @Override
        public void receive(@NonNull Event e) {
            resultEvent = e;
        }

        @Override
        public @NonNull Set<@NonNull String> getSubscribedEventTypes() {
            return Collections.singleton(ItemCommandEvent.TYPE);
        }

        @Override
        public @Nullable EventFilter getEventFilter() {
            return null;
        }
    };

    @Before
    public void setup() {
        registerVolatileStorageService();
        modelRepository = getService(ModelRepository.class);
        assertNotNull(modelRepository);
        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);
        eventPublisher = getService(EventPublisher.class);
        assertNotNull(eventPublisher);
        registerService(eventSubscriber, EventSubscriber.class.getName());

        CoreItemFactory factory = new CoreItemFactory();
        GenericItem switchItem = factory.createItem("Switch", "TestSwitch");
        GenericItem resultItem = factory.createItem("Switch", "TestResult");
        GenericItem groupItem = new GroupItem("TestGroup");
        if (switchItem == null || resultItem == null) {
            throw new NullPointerException();
        }
        switchItem.addGroupName(groupItem.getName());
        itemRegistry.add(switchItem);
        itemRegistry.add(resultItem);
        itemRegistry.add(groupItem);
        resultEvent = null;
    }

    @Test
    public void testUpdateEventTrigger() throws Exception {
        String model = "rule Test " + //
                "when " + //
                "    Item TestSwitch received update " + //
                "then " + //
                "    TestResult.send(ON) " + //
                "end ";

        assertExecutionWith(model, ItemEventFactory.createStateEvent("TestSwitch", OnOffType.ON), TriggerTypes.UPDATE);
    }

    @Test
    public void testChangedEventTrigger() throws Exception {
        String model = "rule Test " + //
                "when " + //
                "    Item TestSwitch changed " + //
                "then " + //
                "    TestResult.send(ON) " + //
                "end ";

        assertExecutionWith(model, ItemEventFactory.createStateEvent("TestSwitch", OnOffType.ON), TriggerTypes.CHANGE);
    }

    @Test
    public void testMemberUpdateEventTrigger() throws Exception {
        String model = "rule Test " + //
                "when " + //
                "    Member of TestGroup received update " + //
                "then " + //
                "    TestResult.send(ON) " + //
                "end ";

        assertExecutionWith(model, ItemEventFactory.createStateEvent("TestSwitch", OnOffType.ON), TriggerTypes.UPDATE);
    }

    @Test
    public void testMemberChangedEventTrigger() throws Exception {
        String model = "rule Test " + //
                "when " + //
                "    Member of TestGroup changed " + //
                "then " + //
                "    TestResult.send(ON) " + //
                "end ";

        assertExecutionWith(model, ItemEventFactory.createStateEvent("TestSwitch", OnOffType.ON), TriggerTypes.CHANGE);
    }

    private <T> void assertExecutionWith(String model, Event event, TriggerTypes triggerType)
            throws InterruptedException {
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        RuleEngineImpl ruleEngine = (RuleEngineImpl) getService(RuleEngine.class);
        waitForAssert(() -> {
            assertFalse(ruleEngine.starting);
            Iterable<Rule> rules = ruleEngine.getTriggerManager().getRules(triggerType);
            Iterator<Rule> iterator = rules.iterator();
            assertTrue(iterator.hasNext());
        });

        eventPublisher.post(event);

        waitForAssert(() -> {
            assertNotNull(resultEvent);
        });
    }

}
