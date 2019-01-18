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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Expression that successfully parses, if a given expression occurs or repeats with a specified cardinality. This class
 * is immutable.
 *
 * @author Tilman Kamp - Initial contribution and API
 *
 */
public final class ExpressionCardinality extends Expression {

    private Expression subExpression;
    private boolean atLeastOne = false;
    private boolean atMostOne = true;

    /**
     * Constructs a new instance.
     *
     * @param subExpression expression that could occur or repeat
     * @param atLeastOne true, if expression should occur at least one time
     * @param atMostOne true, if expression should occur at most one time
     */
    public ExpressionCardinality(Expression subExpression, boolean atLeastOne, boolean atMostOne) {
        this.subExpression = subExpression;
        this.atLeastOne = atLeastOne;
        this.atMostOne = atMostOne;
    }

    @Override
    ASTNode parse(ResourceBundle language, TokenList tokenList) {
        TokenList list = tokenList;
        ASTNode node = new ASTNode(), cr;
        ArrayList<ASTNode> nodes = new ArrayList<ASTNode>();
        ArrayList<Object> values = new ArrayList<Object>();
        while ((cr = subExpression.parse(language, list)).isSuccess()) {
            nodes.add(cr);
            values.add(cr.getValue());
            list = cr.getRemainingTokens();
            if (atMostOne) {
                break;
            }
        }
        if (!(atLeastOne && nodes.size() == 0)) {
            node.setChildren(nodes.toArray(new ASTNode[0]));
            node.setRemainingTokens(list);
            node.setSuccess(true);
            node.setValue(atMostOne ? (values.size() > 0 ? values.get(0) : null) : values.toArray());
            generateValue(node);
        }
        return node;
    }

    @Override
    List<Expression> getChildExpressions() {
        return Collections.unmodifiableList(Arrays.asList(subExpression));
    }

    @Override
    boolean collectFirsts(ResourceBundle language, HashSet<String> firsts) {
        return subExpression.collectFirsts(language, firsts) || atLeastOne;
    }

    @Override
    public String toString() {
        return "cardinal(" + atLeastOne + ", " + atMostOne + "' " + subExpression.toString() + ")";
    }

    /**
     * @return the subExpression
     */
    public Expression getSubExpression() {
        return subExpression;
    }

    /**
     * @return the atLeastOne
     */
    public boolean isAtLeastOne() {
        return atLeastOne;
    }

    /**
     * @return the atMostOne
     */
    public boolean isAtMostOne() {
        return atMostOne;
    }
}
