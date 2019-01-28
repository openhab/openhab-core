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
package org.eclipse.smarthome.core.voice.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Expression that successfully parses, if one of the given alternative expressions matches. This class is immutable.
 *
 * @author Tilman Kamp - Initial contribution and API
 *
 */
final class ExpressionAlternatives extends Expression {

    private List<Expression> subExpressions;

    /**
     * Constructs a new instance.
     *
     * @param subExpressions the sub expressions that are tried/parsed as alternatives in the given order
     */
    public ExpressionAlternatives(Expression... subExpressions) {
        super();
        this.subExpressions = Collections
                .unmodifiableList(Arrays.asList(Arrays.copyOf(subExpressions, subExpressions.length)));
    }

    @Override
    ASTNode parse(ResourceBundle language, TokenList list) {
        ASTNode node = new ASTNode(), cr;
        for (int i = 0; i < subExpressions.size(); i++) {
            cr = subExpressions.get(i).parse(language, list);
            if (cr.isSuccess()) {
                node.setChildren(new ASTNode[] { cr });
                node.setRemainingTokens(cr.getRemainingTokens());
                node.setSuccess(true);
                node.setValue(cr.getValue());
                generateValue(node);
                return node;
            }
        }
        return node;
    }

    @Override
    List<Expression> getChildExpressions() {
        return subExpressions;
    }

    @Override
    boolean collectFirsts(ResourceBundle language, HashSet<String> firsts) {
        boolean blocking = true;
        for (Expression e : subExpressions) {
            blocking = blocking && e.collectFirsts(language, firsts);
        }
        return blocking;
    }

    @Override
    public String toString() {
        String s = null;
        for (Expression e : subExpressions) {
            s = s == null ? e.toString() : (s + ", " + e.toString());
        }
        return "alt(" + s + ")";
    }
}
