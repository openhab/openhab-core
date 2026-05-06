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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.internal.module.handler.ChannelEventTriggerHandler;
import org.openhab.core.automation.internal.module.handler.DateTimeTriggerHandler;
import org.openhab.core.automation.internal.module.handler.DayOfWeekConditionHandler;
import org.openhab.core.automation.internal.module.handler.EphemerisConditionHandler;
import org.openhab.core.automation.internal.module.handler.GenericCronTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GenericEventTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GroupCommandTriggerHandler;
import org.openhab.core.automation.internal.module.handler.GroupStateTriggerHandler;
import org.openhab.core.automation.internal.module.handler.IntervalConditionHandler;
import org.openhab.core.automation.internal.module.handler.ItemCommandActionHandler;
import org.openhab.core.automation.internal.module.handler.ItemCommandTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateConditionHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ItemStateUpdateActionHandler;
import org.openhab.core.automation.internal.module.handler.RuleEnablementActionHandler;
import org.openhab.core.automation.internal.module.handler.RunRuleActionHandler;
import org.openhab.core.automation.internal.module.handler.SystemTriggerHandler;
import org.openhab.core.automation.internal.module.handler.ThingStatusConditionHandler;
import org.openhab.core.automation.internal.module.handler.ThingStatusTriggerHandler;
import org.openhab.core.automation.internal.module.handler.TimeOfDayConditionHandler;
import org.openhab.core.automation.internal.module.handler.TimeOfDayTriggerHandler;
import org.openhab.core.automation.module.script.internal.handler.ScriptActionHandler;
import org.openhab.core.automation.module.script.internal.handler.ScriptConditionHandler;

/**
 * This class provides translations to and from aliases for {@link Module} typeUIDs.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class ModuleTypeAliases {

    private static final Map<Class<? extends Module>, Map<String, String>> ALIAS_IDX;
    private static final Map<Class<? extends Module>, Map<String, String>> TYPE_IDX;

    static {
        /*
         * Modify the table below to modify aliases. The first column indicates the type of alias:
         * Action (A), Condition (C) or Trigger (T).
         */
        String[][] table = { //
                { "A", "EnableRule", RuleEnablementActionHandler.UID }, //
                { "A", "SendCommand", ItemCommandActionHandler.ITEM_COMMAND_ACTION }, //
                { "A", "PostUpdate", ItemStateUpdateActionHandler.ITEM_STATE_UPDATE_ACTION }, //
                { "A", "Play", "media.PlayAction" }, //
                { "A", "RunRule", RunRuleActionHandler.UID }, //
                { "A", "Say", "media.SayAction" }, //
                { "A", "Script", ScriptActionHandler.TYPE_ID }, //
                { "C", "DayOfWeek", DayOfWeekConditionHandler.MODULE_TYPE_ID }, //
                { "C", "Dayset", EphemerisConditionHandler.DAYSET_MODULE_TYPE_ID }, //
                { "C", "Holiday", EphemerisConditionHandler.HOLIDAY_MODULE_TYPE_ID }, //
                { "C", "Interval", IntervalConditionHandler.MODULE_TYPE_ID }, //
                { "C", "ItemState", ItemStateConditionHandler.ITEM_STATE_CONDITION }, //
                { "C", "NotHoliday", EphemerisConditionHandler.NOT_HOLIDAY_MODULE_TYPE_ID }, //
                { "C", "Script", ScriptConditionHandler.TYPE_ID }, //
                { "C", "ThingStatus", ThingStatusConditionHandler.THING_STATUS_CONDITION }, //
                { "C", "TimeOfDay", TimeOfDayConditionHandler.MODULE_TYPE_ID }, //
                { "C", "Weekday", EphemerisConditionHandler.WEEKDAY_MODULE_TYPE_ID }, //
                { "C", "Weekend", EphemerisConditionHandler.WEEKEND_MODULE_TYPE_ID }, //
                { "T", "ChannelEvent", ChannelEventTriggerHandler.MODULE_TYPE_ID }, //
                { "T", "Cron", GenericCronTriggerHandler.MODULE_TYPE_ID }, //
                { "T", "DateTime", DateTimeTriggerHandler.MODULE_TYPE_ID }, //
                { "T", "GenericEvent", GenericEventTriggerHandler.MODULE_TYPE_ID }, //
                { "T", "MemberReceivedCommand", GroupCommandTriggerHandler.MODULE_TYPE_ID }, //
                { "T", "MemberChanged", GroupStateTriggerHandler.CHANGE_MODULE_TYPE_ID }, //
                { "T", "MemberUpdated", GroupStateTriggerHandler.UPDATE_MODULE_TYPE_ID }, //
                { "T", "ItemReceivedCommand", ItemCommandTriggerHandler.MODULE_TYPE_ID }, //
                { "T", "ItemChanged", ItemStateTriggerHandler.CHANGE_MODULE_TYPE_ID }, //
                { "T", "ItemUpdated", ItemStateTriggerHandler.UPDATE_MODULE_TYPE_ID }, //
                { "T", "StartLevel", SystemTriggerHandler.STARTLEVEL_MODULE_TYPE_ID }, //
                { "T", "ThingChanged", ThingStatusTriggerHandler.CHANGE_MODULE_TYPE_ID }, //
                { "T", "ThingUpdated", ThingStatusTriggerHandler.UPDATE_MODULE_TYPE_ID }, //
                { "T", "TimeOfDay", TimeOfDayTriggerHandler.MODULE_TYPE_ID } //
        };

        Map<Class<? extends Module>, Map<String, String>> aliasIdx = new HashMap<>();
        Map<Class<? extends Module>, Map<String, String>> typeIdx = new HashMap<>();
        Class<? extends Module> clazz;
        Map<String, String> inner;
        for (String[] entry : table) {
            switch (entry[0]) {
                case "A":
                    clazz = Action.class;
                    break;
                case "C":
                    clazz = Condition.class;
                    break;
                case "T":
                    clazz = Trigger.class;
                    break;
                default:
                    continue;
            }
            if (!aliasIdx.containsKey(clazz)) {
                aliasIdx.put(clazz, new HashMap<>());
            }
            if (!typeIdx.containsKey(clazz)) {
                typeIdx.put(clazz, new HashMap<>());
            }
            if ((inner = aliasIdx.get(clazz)) != null) {
                inner.put(entry[1], entry[2]);
            }
            if ((inner = typeIdx.get(clazz)) != null) {
                inner.put(entry[2], entry[1]);
            }
        }
        ALIAS_IDX = Collections.unmodifiableMap(aliasIdx);
        TYPE_IDX = Collections.unmodifiableMap(typeIdx);
    }

    /**
     * Not to be instantiated
     */
    private ModuleTypeAliases() {
    }

    /**
     * Translates an alias to a proper {@link Module} {@code typeUID}, or returns the argument if the argument doesn't
     * match an alias.
     *
     * @param clazz the {@link Module} subclass for which to translate aliases.
     * @param alias the potential alias to translate.
     * @return The corresponding {@link Module} {@code typeUID} or the unmodified argument the it's not an alias.
     */
    public static String aliasToType(Class<? extends Module> clazz, String alias) {
        Map<String, String> map = ALIAS_IDX.get(clazz);
        return map != null ? map.getOrDefault(alias, alias) : alias;
    }

    /**
     * Translates a {@link Module} {@code typeUID} to an alias if such a mapping exists, otherwise returns the argument.
     *
     * @param clazz the {@link Module} subinterface or implementation for which to translate aliases.
     * @param type {@code typeUID} to potentially translate to an alias.
     * @return The corresponding alias or the unmodified argument if no matching mapping was found.
     */
    public static String typeToAlias(Class<? extends Module> clazz, String type) {
        Class<? extends Module> iface;
        if (Trigger.class.isAssignableFrom(clazz)) {
            iface = Trigger.class;
        } else if (Condition.class.isAssignableFrom(clazz)) {
            iface = Condition.class;
        } else if (Action.class.isAssignableFrom(clazz)) {
            iface = Action.class;
        } else {
            return type;
        }
        Map<String, String> map = TYPE_IDX.get(iface);
        return map != null ? map.getOrDefault(type, type) : type;
    }
}
