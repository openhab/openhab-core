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
package org.openhab.core.model.rule.runtime;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.automation.internal.module.handler.ChannelEventTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GenericCronTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GroupCommandTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GroupStateTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ItemCommandTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateTriggerHandler;
import org.openhab.core.automation.internal.module.handler.SystemTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ThingStatusTriggerHandler;
import org.openhab.core.automation.module.script.internal.handler.AbstractScriptModuleHandler;
import org.openhab.core.automation.module.script.internal.handler.ScriptActionHandler;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.rule.jvmmodel.RulesRefresher;
import org.openhab.core.model.rule.runtime.internal.DSLRuleProvider;
import org.openhab.core.model.script.runtime.internal.engine.DSLScriptEngine;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class DSLRuleProviderTest extends JavaOSGiTest {

    private static final String TESTMODEL_NAME = "dslruletest.rules";

    private @NonNullByDefault({}) ModelRepository modelRepository;
    private @NonNullByDefault({}) DSLRuleProvider dslRuleProvider;
    private @NonNullByDefault({}) ReadyService readyService;

    @Before
    public void setup() {
        registerVolatileStorageService();

        EventPublisher eventPublisher = event -> {
        };

        registerService(eventPublisher);

        dslRuleProvider = getService(RuleProvider.class, DSLRuleProvider.class);
        assertNotNull(dslRuleProvider);

        modelRepository = getService(ModelRepository.class);
        assertThat(modelRepository, is(notNullValue()));
        modelRepository.removeModel(TESTMODEL_NAME);

        readyService = getService(ReadyService.class);
        assertThat(readyService, is(notNullValue()));
        for (String id : Set.of("items", "things", "rules", RulesRefresher.RULES_REFRESH)) {
            readyService.markReady(new ReadyMarker("dsl", id));
        }
    }

    @After
    public void tearDown() {
        modelRepository.removeModel(TESTMODEL_NAME);
    }

    @Test
    public void testSimpleRules() {
        Collection<Rule> rules = dslRuleProvider.getAll();
        assertThat(rules.size(), is(0));

        String model = "rule RuleNumberOne\n" + //
                "when\n" + //
                "   System started\n" + //
                "then\n" + //
                "   logInfo('Test', 'Test')\n" + //
                "end\n\n" + //
                "rule 'Rule Number Two'\n" + //
                "when\n" + //
                "   Item X changed\n" + //
                "then\n" + //
                "   logInfo('Test', 'Test')\n" + //
                "end\n\n";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Rule> actualRules = dslRuleProvider.getAll();

        assertThat(actualRules.size(), is(2));

        Iterator<Rule> it = actualRules.iterator();

        Rule firstRule = it.next();

        assertThat(firstRule.getUID(), is("dslruletest-1"));
        assertThat(firstRule.getName(), is("RuleNumberOne"));
        assertThat(firstRule.getTriggers().get(0).getTypeUID(), is(SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID));
        assertThat(firstRule.getActions().get(0).getTypeUID(), is(ScriptActionHandler.TYPE_ID));
        assertThat(firstRule.getActions().get(0).getConfiguration().get(AbstractScriptModuleHandler.SCRIPT_TYPE),
                is(DSLScriptEngine.MIMETYPE_OPENHAB_DSL_RULE));

        Rule secondRule = it.next();

        assertThat(secondRule.getUID(), is("dslruletest-2"));
        assertThat(secondRule.getName(), is("Rule Number Two"));
        assertThat(secondRule.getTriggers().get(0).getTypeUID(), is(ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID));
        assertThat(secondRule.getTriggers().get(0).getConfiguration().get(ItemStateTriggerHandler.CFG_ITEMNAME),
                is("X"));
        assertThat(secondRule.getActions().get(0).getTypeUID(), is(ScriptActionHandler.TYPE_ID));
        assertThat(secondRule.getActions().get(0).getConfiguration().get(AbstractScriptModuleHandler.SCRIPT_TYPE),
                is(DSLScriptEngine.MIMETYPE_OPENHAB_DSL_RULE));
    }

    @Test
    public void testAllTriggers() {
        Collection<Rule> rules = dslRuleProvider.getAll();
        assertThat(rules.size(), is(0));

        String model = "rule RuleWithAllTriggers\n" + //
                "when\n" + //
                "   System started or\n" + //
                "   Time is noon or\n" + //
                "   Time is midnight or\n" + //
                "   Time cron \"0 0/1 * * * ?\" or\n" + //
                "   Item X received command ON or\n" + //
                "   Item Y received update \"A\" or\n" + //
                "   Item Z changed from 1 to 2 or\n" + //
                "   Member of G1 received command UP or\n" + //
                "   Member of G2 received update \"10|°C\" or\n" + //
                "   Member of G3 changed from PLAY to PAUSE or\n" + //
                "   Thing \"T1\" received update OFFLINE or\n" + //
                "   Thing \"T2\" changed from OFFLINE to ONLINE or\n" + //
                "   Channel \"a:b:c:1\" triggered START\n" + //
                "then\n" + //
                "   logInfo('Test', 'Test')\n" + //
                "end\n\n";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Rule> actualRules = dslRuleProvider.getAll();

        assertThat(actualRules.size(), is(1));

        Iterator<Rule> it = actualRules.iterator();

        Rule rule = it.next();

        assertThat(rule.getUID(), is("dslruletest-1"));
        assertThat(rule.getName(), is("RuleWithAllTriggers"));
        assertThat(rule.getTriggers().size(), is(13));

        assertThat(rule.getTriggers().get(0).getTypeUID(), is(SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(0).getConfiguration().get(SystemTriggerHandler.CFG_STARTLEVEL),
                is(new BigDecimal(20)));
        assertThat(rule.getTriggers().get(1).getTypeUID(), is(GenericCronTriggerHandler.MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(1).getConfiguration().get(GenericCronTriggerHandler.CFG_CRON_EXPRESSION),
                is("0 0 12 * * ?"));

        assertThat(rule.getTriggers().get(2).getTypeUID(), is(GenericCronTriggerHandler.MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(2).getConfiguration().get(GenericCronTriggerHandler.CFG_CRON_EXPRESSION),
                is("0 0 0 * * ?"));

        assertThat(rule.getTriggers().get(3).getTypeUID(), is(GenericCronTriggerHandler.MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(3).getConfiguration().get(GenericCronTriggerHandler.CFG_CRON_EXPRESSION),
                is("0 0/1 * * * ?"));

        assertThat(rule.getTriggers().get(4).getTypeUID(), is(ItemCommandTriggerHandler.MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(4).getConfiguration().get(ItemCommandTriggerHandler.CFG_ITEMNAME), is("X"));
        assertThat(rule.getTriggers().get(4).getConfiguration().get(ItemCommandTriggerHandler.CFG_COMMAND), is("ON"));

        assertThat(rule.getTriggers().get(5).getTypeUID(), is(ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(5).getConfiguration().get(ItemStateTriggerHandler.CFG_ITEMNAME), is("Y"));
        assertThat(rule.getTriggers().get(5).getConfiguration().get(ItemStateTriggerHandler.CFG_STATE), is("A"));

        assertThat(rule.getTriggers().get(6).getTypeUID(), is(ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(6).getConfiguration().get(ItemStateTriggerHandler.CFG_ITEMNAME), is("Z"));
        assertThat(rule.getTriggers().get(6).getConfiguration().get(ItemStateTriggerHandler.CFG_PREVIOUS_STATE),
                is("1"));
        assertThat(rule.getTriggers().get(6).getConfiguration().get(ItemStateTriggerHandler.CFG_STATE), is("2"));

        assertThat(rule.getTriggers().get(7).getTypeUID(), is(GroupCommandTriggerHandler.MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(7).getConfiguration().get(GroupCommandTriggerHandler.CFG_GROUPNAME),
                is("G1"));
        assertThat(rule.getTriggers().get(7).getConfiguration().get(GroupCommandTriggerHandler.CFG_COMMAND), is("UP"));

        assertThat(rule.getTriggers().get(8).getTypeUID(), is(GroupStateTriggerHandler.UPDATE_MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(8).getConfiguration().get(GroupStateTriggerHandler.CFG_GROUPNAME), is("G2"));
        assertThat(rule.getTriggers().get(8).getConfiguration().get(GroupStateTriggerHandler.CFG_STATE), is("10|°C"));

        assertThat(rule.getTriggers().get(9).getTypeUID(), is(GroupStateTriggerHandler.CHANGE_MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(9).getConfiguration().get(GroupStateTriggerHandler.CFG_GROUPNAME), is("G3"));
        assertThat(rule.getTriggers().get(9).getConfiguration().get(GroupStateTriggerHandler.CFG_PREVIOUS_STATE),
                is("PLAY"));
        assertThat(rule.getTriggers().get(9).getConfiguration().get(GroupStateTriggerHandler.CFG_STATE), is("PAUSE"));

        assertThat(rule.getTriggers().get(10).getTypeUID(), is(ThingStatusTriggerHandler.UPDATE_MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(10).getConfiguration().get(ThingStatusTriggerHandler.CFG_THING_UID),
                is("T1"));
        assertThat(rule.getTriggers().get(10).getConfiguration().get(ThingStatusTriggerHandler.CFG_STATUS),
                is("OFFLINE"));

        assertThat(rule.getTriggers().get(11).getTypeUID(), is(ThingStatusTriggerHandler.CHANGE_MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(11).getConfiguration().get(ThingStatusTriggerHandler.CFG_THING_UID),
                is("T2"));
        assertThat(rule.getTriggers().get(11).getConfiguration().get(ThingStatusTriggerHandler.CFG_PREVIOUS_STATUS),
                is("OFFLINE"));
        assertThat(rule.getTriggers().get(11).getConfiguration().get(ThingStatusTriggerHandler.CFG_STATUS),
                is("ONLINE"));

        assertThat(rule.getTriggers().get(12).getTypeUID(), is(ChannelEventTriggerHandler.MODULE_TYPE_ID));
        assertThat(rule.getTriggers().get(12).getConfiguration().get(ChannelEventTriggerHandler.CFG_CHANNEL),
                is("a:b:c:1"));
        assertThat(rule.getTriggers().get(12).getConfiguration().get(ChannelEventTriggerHandler.CFG_CHANNEL_EVENT),
                is("START"));
    }

    @Test
    public void testVars() {
        Collection<Rule> rules = dslRuleProvider.getAll();
        assertThat(rules.size(), is(0));

        String model = "var x = 3 * 5\n\n" + //
                "rule FirstRule\n" + //
                "when\n" + //
                "   System started\n" + //
                "then\n" + //
                "   logInfo('Test', 'Test')\n" + //
                "end\n\n";

        modelRepository.addOrRefreshModel(TESTMODEL_NAME, new ByteArrayInputStream(model.getBytes()));
        Collection<Rule> actualRules = dslRuleProvider.getAll();

        assertThat(actualRules.size(), is(1));

        Iterator<Rule> it = actualRules.iterator();

        Rule rule = it.next();

        assertThat(rule.getUID(), is("dslruletest-1"));
        assertThat(rule.getName(), is("FirstRule"));
        assertThat(rule.getTriggers().size(), is(1));

        IEvaluationContext context = dslRuleProvider.getContext("dslruletest");
        assertThat(context, is(notNullValue()));
        Object x = context.getValue(QualifiedName.create("x"));
        assertThat(x, is(15));
    }
}
