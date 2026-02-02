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
package org.openhab.core.model.yaml.internal.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Trigger;

/**
 * The {@link ModuleTypeAliasesTest} contains tests for the {@link ModuleTypeAliases} class.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class ModuleTypeAliasesTest {

    @Test
    public void testAliasToType() {
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "a"), is("a"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, ""), is(""));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "EnableRule"), is("EnableRule"));
        assertThat(ModuleTypeAliases.aliasToType(Action.class, "EnableRule"), is("core.RuleEnablementAction"));
        assertThat(ModuleTypeAliases.aliasToType(Action.class, "SendCommand"), is("core.ItemCommandAction"));
        assertThat(ModuleTypeAliases.aliasToType(Action.class, "PostUpdate"), is("core.ItemStateUpdateAction"));
        assertThat(ModuleTypeAliases.aliasToType(Action.class, "Play"), is("media.PlayAction"));
        assertThat(ModuleTypeAliases.aliasToType(Action.class, "Say"), is("media.SayAction"));
        assertThat(ModuleTypeAliases.aliasToType(Action.class, "Script"), is("script.ScriptAction"));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, ""), is(""));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, "DayOfWeek"), is("timer.DayOfWeekCondition"));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, "Dayset"), is("ephemeris.DaysetCondition"));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, "Holiday"), is("ephemeris.HolidayCondition"));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, "Interval"), is("timer.IntervalCondition"));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, "ItemState"), is("core.ItemStateCondition"));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, "NotHoliday"), is("ephemeris.NotHolidayCondition"));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, "Script"), is("script.ScriptCondition"));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, "TimeOfDay"), is("core.TimeOfDayCondition"));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, "Weekday"), is("ephemeris.WeekdayCondition"));
        assertThat(ModuleTypeAliases.aliasToType(Condition.class, "Weekend"), is("ephemeris.WeekendCondition"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, ""), is(""));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "ChannelEvent"), is("core.ChannelEventTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "Cron"), is("timer.GenericCronTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "DateTime"), is("timer.DateTimeTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "MemberReceivedCommand"),
                is("core.GroupCommandTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "MemberChanged"), is("core.GroupStateChangeTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "ItemReceivedCommand"), is("core.ItemCommandTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "ItemChanged"), is("core.ItemStateChangeTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "ItemUpdated"), is("core.ItemStateUpdateTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "StartLevel"), is("core.SystemStartlevelTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "ThingChanged"), is("core.ThingStatusChangeTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "ThingUpdated"), is("core.ThingStatusUpdateTrigger"));
        assertThat(ModuleTypeAliases.aliasToType(Trigger.class, "TimeOfDay"), is("timer.TimeOfDayTrigger"));
    }

    @Test
    public void testTypeToAlias() {
        assertThat(ModuleTypeAliases.typeToAlias(Action.class, "a"), is("a"));
        assertThat(ModuleTypeAliases.typeToAlias(Action.class, ""), is(""));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "EnableRule"), is("EnableRule"));
        assertThat(ModuleTypeAliases.typeToAlias(Action.class, "core.RuleEnablementAction"), is("EnableRule"));
        assertThat(ModuleTypeAliases.typeToAlias(Action.class, "core.ItemCommandAction"), is("SendCommand"));
        assertThat(ModuleTypeAliases.typeToAlias(Action.class, "core.ItemStateUpdateAction"), is("PostUpdate"));
        assertThat(ModuleTypeAliases.typeToAlias(Action.class, "media.PlayAction"), is("Play"));
        assertThat(ModuleTypeAliases.typeToAlias(Action.class, "media.SayAction"), is("Say"));
        assertThat(ModuleTypeAliases.typeToAlias(Action.class, "script.ScriptAction"), is("Script"));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, ""), is(""));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, "timer.DayOfWeekCondition"), is("DayOfWeek"));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, "ephemeris.DaysetCondition"), is("Dayset"));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, "ephemeris.HolidayCondition"), is("Holiday"));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, "timer.IntervalCondition"), is("Interval"));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, "core.ItemStateCondition"), is("ItemState"));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, "ephemeris.NotHolidayCondition"), is("NotHoliday"));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, "script.ScriptCondition"), is("Script"));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, "core.TimeOfDayCondition"), is("TimeOfDay"));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, "ephemeris.WeekdayCondition"), is("Weekday"));
        assertThat(ModuleTypeAliases.typeToAlias(Condition.class, "ephemeris.WeekendCondition"), is("Weekend"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, ""), is(""));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "core.ChannelEventTrigger"), is("ChannelEvent"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "timer.GenericCronTrigger"), is("Cron"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "timer.DateTimeTrigger"), is("DateTime"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "core.GroupCommandTrigger"),
                is("MemberReceivedCommand"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "core.GroupStateChangeTrigger"), is("MemberChanged"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "core.ItemCommandTrigger"), is("ItemReceivedCommand"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "core.ItemStateChangeTrigger"), is("ItemChanged"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "core.ItemStateUpdateTrigger"), is("ItemUpdated"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "core.SystemStartlevelTrigger"), is("StartLevel"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "core.ThingStatusChangeTrigger"), is("ThingChanged"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "core.ThingStatusUpdateTrigger"), is("ThingUpdated"));
        assertThat(ModuleTypeAliases.typeToAlias(Trigger.class, "timer.TimeOfDayTrigger"), is("TimeOfDay"));
    }
}
