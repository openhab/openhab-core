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
package org.openhab.core.model.rule.validation;

import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.validation.Check;
import org.openhab.core.automation.util.RuleUtil;
import org.openhab.core.model.rule.rules.Rule;
import org.openhab.core.model.rule.rules.RulesPackage;
import org.openhab.core.model.rule.rules.TimeOfDayCondition;
import org.openhab.core.model.rule.rules.TimerTrigger;

/**
 * This class contains custom validation rules.
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class RulesValidator extends AbstractRulesValidator {

    private static Set<String> ALLOWED_TIME_NAMES = Set.of("midnight", "noon");

    @Check
    public void checkRuleUID(@Nullable Rule rule) {
        String uid;
        if (rule == null || (uid = rule.getUid()) == null) {
            return;
        }
        if (!RuleUtil.isValidRuleUID(uid)) {
            error(buildMsgWithLineNb("Rule UID '" + uid
                    + "' is invalid. A rule UID can't contain '/', '\\' or have leading or trailing whitespace.", rule,
                    RulesPackage.Literals.RULE__UID),
                    RulesPackage.Literals.RULE.getEStructuralFeature(RulesPackage.RULE__UID), "uid");
        }
    }

    @Check
    public void checkTimerTrigger(@Nullable TimerTrigger timeTrigger) {
        String time;
        if (timeTrigger == null || (time = timeTrigger.getTime()) == null) {
            return;
        }
        if (ALLOWED_TIME_NAMES.contains(time)) {
            return;
        }
        if (!isValidTime(time)) {
            error(buildMsgWithLineNb("time '" + time + "' in trigger is invalid", timeTrigger,
                    RulesPackage.Literals.TIMER_TRIGGER__TIME),
                    RulesPackage.Literals.TIMER_TRIGGER.getEStructuralFeature(RulesPackage.TIMER_TRIGGER__TIME),
                    "time");
        }
    }

    @Check
    public void checkTimeOfDayCondition(@Nullable TimeOfDayCondition timeOfDayCondition) {
        String start, end;
        if (timeOfDayCondition == null || (start = timeOfDayCondition.getStart()) == null
                || (end = timeOfDayCondition.getEnd()) == null) {
            return;
        }
        if (!isValidTime(start)) {
            error(buildMsgWithLineNb("start time '" + start + "' in condition is invalid", timeOfDayCondition,
                    RulesPackage.Literals.TIME_OF_DAY_CONDITION__START),
                    RulesPackage.Literals.TIME_OF_DAY_CONDITION
                            .getEStructuralFeature(RulesPackage.TIME_OF_DAY_CONDITION__START),
                    "time");
        }
        if (!isValidTime(end)) {
            error(buildMsgWithLineNb("end time '" + end + "' in condition is invalid", timeOfDayCondition,
                    RulesPackage.Literals.TIME_OF_DAY_CONDITION__END),
                    RulesPackage.Literals.TIME_OF_DAY_CONDITION
                            .getEStructuralFeature(RulesPackage.TIME_OF_DAY_CONDITION__END),
                    "time");
        }
    }

    private static boolean isValidTime(String time) {
        String[] splittedTime = time.split(":", 3);
        if (splittedTime.length != 2) {
            return false;
        }
        try {
            int hour = Integer.parseInt(splittedTime[0]);
            int minute = Integer.parseInt(splittedTime[1]);
            return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String buildMsgWithLineNb(String msg, EObject object, @Nullable EAttribute attribute) {
        ICompositeNode node = NodeModelUtils.getNode(object);
        INode nodeAttr = null;
        if (attribute != null) {
            List<INode> nodes = NodeModelUtils.findNodesForFeature(object, attribute);
            if (nodes != null && nodes.size() >= 1) {
                nodeAttr = nodes.getFirst();
            }
        }
        if (nodeAttr != null) {
            return buildMsgWithLineNb(msg, nodeAttr);
        } else if (node != null) {
            return buildMsgWithLineNb(msg, node);
        }
        return msg;
    }

    private static String buildMsgWithLineNb(String msg, INode node) {
        int startLine = node.getStartLine();
        int endLine = node.getEndLine();
        return (startLine == endLine) ? "Line " + startLine + ": " + msg
                : "Line " + startLine + "-" + endLine + ": " + msg;
    }
}
