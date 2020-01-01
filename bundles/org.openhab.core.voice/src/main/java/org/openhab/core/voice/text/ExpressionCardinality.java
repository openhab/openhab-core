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
package org.openhab.core.voice.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Expression that successfully parses, if a given expression occurs or repeats with a specified cardinality. This class
 * is immutable.
 *
 * @author Tilman Kamp - Initial contribution
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
        List<ASTNode> nodes = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        while ((cr = subExpression.parse(language, list)).isSuccess()) {
            nodes.add(cr);
            values.add(cr.getValue());
            list = cr.getRemainingTokens();
            if (atMostOne) {
                break;
            }
        }
        if (!(atLeastOne && nodes.isEmpty())) {
            node.setChildren(nodes.toArray(new ASTNode[0]));
            node.setRemainingTokens(list);
            node.setSuccess(true);
            node.setValue(atMostOne ? (values.isEmpty() ? null : values.get(0)) : values.toArray());
            generateValue(node);
        }
        return node;
    }

    @Override
    List<Expression> getChildExpressions() {
        return Collections.unmodifiableList(Arrays.asList(subExpression));
    }

    @Override
    boolean collectFirsts(ResourceBundle language, Set<String> firsts) {
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
