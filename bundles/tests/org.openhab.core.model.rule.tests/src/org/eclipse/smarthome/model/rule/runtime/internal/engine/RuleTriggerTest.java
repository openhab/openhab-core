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
import java.util.Iterator;
import java.util.function.Function;

import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.model.rule.rules.ChangedEventTrigger;
import org.eclipse.smarthome.model.rule.rules.CommandEventTrigger;
import org.eclipse.smarthome.model.rule.rules.EventEmittedTrigger;
import org.eclipse.smarthome.model.rule.rules.EventTrigger;
import org.eclipse.smarthome.model.rule.rules.Rule;
import org.eclipse.smarthome.model.rule.runtime.RuleEngine;
import org.eclipse.smarthome.model.rule.runtime.internal.engine.RuleTriggerManager.TriggerTypes;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Simon Kaufmann - initial contribution and API.
 */
public class RuleTriggerTest extends JavaOSGiTest {

    private final static String TESTMODEL_NAME = "testModel.rules";
    private ModelRepository modelRepository;

    @Before
    public void setup() {
        modelRepository = getService(ModelRepository.class);
        assertNotNull(modelRepository);
    }

    @Test
    public void testChangedEventTrigger_withoutQuotes() throws Exception {
        String model = "rule\"State Machine Rule 1\" " + //
                "when " + //
                "    Item test changed to world " + //
                "then " + //
                "    logInfo(\"test says\", \"Boo!\") " + //
                "end ";

        assertTriggerWith(model, TriggerTypes.CHANGE, ChangedEventTrigger.class,
                trigger -> trigger.getNewState().getValue());
    }

    @Test
    public void testChangedEventTrigger_withQuotes() throws Exception {
        String model = "rule\"State Machine Rule 1\" " + //
                "when " + //
                "    Item test changed to \"world\" " + //
                "then " + //
                "    logInfo(\"test says\", \"Boo!\") " + //
                "end ";

        assertTriggerWith(model, TriggerTypes.CHANGE, ChangedEventTrigger.class,
                trigger -> trigger.getNewState().getValue());
    }

    @Test
    public void testCommandEventTrigger_withoutQuotes() throws Exception {
        String model = "rule\"State Machine Rule 1\" " + //
                "when " + //
                "    Item test received command world " + //
                "then " + //
                "    logInfo(\"test says\", \"Boo!\") " + //
                "end ";

        assertTriggerWith(model, TriggerTypes.COMMAND, CommandEventTrigger.class,
                trigger -> trigger.getCommand().getValue());
    }

    @Test
    public void testCommandEventTrigger_withQuotes() throws Exception {
        String model = "rule\"State Machine Rule 1\" " + //
                "when " + //
                "    Item test received command \"world\" " + //
                "then " + //
                "    logInfo(\"test says\", \"Boo!\") " + //
                "end ";

        assertTriggerWith(model, TriggerTypes.COMMAND, CommandEventTrigger.class,
                trigger -> trigger.getCommand().getValue());
    }

    @Test
    public void testEventEmittedTrigger_withoutQuotes() throws Exception {
        String model = "rule\"State Machine Rule 1\" " + //
                "when " + //
                "    Channel test triggered world " + //
                "then " + //
                "    logInfo(\"test says\", \"Boo!\") " + //
                "end ";

        assertTriggerWith(model, TriggerTypes.TRIGGER, EventEmittedTrigger.class,
                trigger -> trigger.getTrigger().getValue());
    }

    @Test
    public void testEventEmittedTrigger_withQuotes() throws Exception {
        String model = "rule\"State Machine Rule 1\" " + //
                "when " + //
                "    Channel test triggered \"world\" " + //
                "then " + //
                "    logInfo(\"test says\", \"Boo!\") " + //
                "end ";

        assertTriggerWith(model, TriggerTypes.TRIGGER, EventEmittedTrigger.class,
                trigger -> trigger.getTrigger().getValue());
    }

    private <T> void assertTriggerWith(String model, TriggerTypes triggerType, Class<T> triggerClass,
            Function<T, String> valueFunction) {
        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));

        RuleTriggerManager triggerManager = ((RuleEngineImpl) getService(RuleEngine.class)).getTriggerManager();
        waitForAssert(() -> {
            Iterable<Rule> rules = triggerManager.getRules(triggerType);
            Iterator<Rule> iterator = rules.iterator();
            assertTrue(iterator.hasNext());
        });
        Rule rule = triggerManager.getRules(triggerType).iterator().next();
        EventTrigger trigger = rule.getEventtrigger().get(0);
        assertTrue(triggerClass.isInstance(trigger));
        assertEquals("world", valueFunction.apply(triggerClass.cast(trigger)));
    }

}
