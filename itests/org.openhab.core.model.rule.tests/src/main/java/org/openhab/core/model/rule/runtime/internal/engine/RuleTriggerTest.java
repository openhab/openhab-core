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
package org.openhab.core.model.rule.runtime.internal.engine;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.rule.rules.ChangedEventTrigger;
import org.openhab.core.model.rule.rules.CommandEventTrigger;
import org.openhab.core.model.rule.rules.EventEmittedTrigger;
import org.openhab.core.model.rule.rules.EventTrigger;
import org.openhab.core.model.rule.rules.Rule;
import org.openhab.core.model.rule.runtime.RuleEngine;
import org.openhab.core.model.rule.runtime.internal.engine.RuleTriggerManager.TriggerTypes;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * @author Simon Kaufmann - Initial contribution
 */
public class RuleTriggerTest extends JavaOSGiTest {

    private static final String TESTMODEL_NAME = "testModel.rules";
    private ModelRepository modelRepository;

    @Before
    public void setup() {
        modelRepository = getService(ModelRepository.class);
        assertNotNull(modelRepository);
    }

    @Test
    public void testChangedEventTriggerWithoutQuotes() throws Exception {
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
    public void testChangedEventTriggerWithQuotes() throws Exception {
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
    public void testCommandEventTriggerWithoutQuotes() throws Exception {
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
    public void testCommandEventTriggerWithQuotes() throws Exception {
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
    public void testEventEmittedTriggerWithoutQuotes() throws Exception {
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
    public void testEventEmittedTriggerWithQuotes() throws Exception {
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

        RuleEngineImpl ruleEngine = (RuleEngineImpl) getService(RuleEngine.class);
        assertNotNull(ruleEngine);
        RuleTriggerManager triggerManager = ruleEngine.getTriggerManager();
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
