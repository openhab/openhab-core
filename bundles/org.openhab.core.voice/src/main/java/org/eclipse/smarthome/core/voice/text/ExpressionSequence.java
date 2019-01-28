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
 * Expression that successfully parses, if a sequence of given expressions is matching. This class is immutable.
 *
 * @author Tilman Kamp - Initial contribution and API
 *
 */
public final class ExpressionSequence extends Expression {

    private List<Expression> subExpressions;

    /**
     * Constructs a new instance.
     *
     * @param subExpressions the sub expressions that are parsed in the given order
     */
    public ExpressionSequence(Expression... subExpressions) {
        super();
        this.subExpressions = Collections
                .unmodifiableList(Arrays.asList(Arrays.copyOf(subExpressions, subExpressions.length)));
    }

    @Override
    ASTNode parse(ResourceBundle language, TokenList tokenList) {
        TokenList list = tokenList;
        int l = subExpressions.size();
        ASTNode node = new ASTNode(), cr;
        ASTNode[] children = new ASTNode[l];
        Object[] values = new Object[l];
        for (int i = 0; i < l; i++) {
            cr = children[i] = subExpressions.get(i).parse(language, list);
            if (!cr.isSuccess()) {
                return node;
            }
            values[i] = cr.getValue();
            list = cr.getRemainingTokens();
        }
        node.setChildren(children);
        node.setRemainingTokens(list);
        node.setSuccess(true);
        node.setValue(values);
        generateValue(node);
        return node;
    }

    @Override
    List<Expression> getChildExpressions() {
        return subExpressions;
    }

    @Override
    boolean collectFirsts(ResourceBundle language, HashSet<String> firsts) {
        boolean blocking = false;
        for (Expression e : subExpressions) {
            blocking = e.collectFirsts(language, firsts);
            if (blocking) {
                break;
            }
        }
        
        return blocking;
    }

    @Override
    public String toString() {
        String s = null;
        for (Expression e : subExpressions) {
            s = s == null ? e.toString() : (s + ", " + e.toString());
        }
        return "seq(" + s + ")";
    }
}
