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

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.validation.Check;
import org.openhab.core.automation.util.RuleUtil;
import org.openhab.core.model.rule.rules.Rule;
import org.openhab.core.model.rule.rules.RulesPackage;

/**
 * This class contains custom validation rules.
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class RulesValidator extends AbstractRulesValidator {

    @Check
    public void checkRuleUID(@Nullable Rule rule) {
        String uid;
        if (rule == null || (uid = rule.getUid()) == null) {
            return;
        }
        if (!RuleUtil.isValidRuleUID(uid)) {
            error(buildMsgWithLineNum(rule, "Rule UID '" + uid + "' is invalid."), rule,
                    RulesPackage.Literals.RULE__UID, "uid");
        }
    }

    private String buildMsgWithLineNum(EObject object, String msg) {
        ICompositeNode node = NodeModelUtils.getNode(object);
        if (node == null) {
            return msg;
        }
        int startLine = node.getStartLine();
        int endLine = node.getEndLine();
        return (startLine == endLine) ? "Line " + startLine + ": " + msg
                : "Line " + startLine + "-" + endLine + ": " + msg;
    }
}
