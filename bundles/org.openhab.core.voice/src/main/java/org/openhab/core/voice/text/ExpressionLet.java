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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Expression that decorates the resulting (proxied) AST node of a given expression by a name, value and tag.
 * This class is immutable.
 *
 * @author Tilman Kamp - Initial contribution
 */
public final class ExpressionLet extends Expression {

    private Expression subExpression;
    private String name;
    private Object value;
    private Object tag;

    /**
     * Constructs a new instance.
     *
     * @param name the name that should be set on the node. Null, if the node's name should not be changed.
     * @param subExpression the expression who's resulting node should be altered
     */
    public ExpressionLet(String name, Expression subExpression) {
        this(name, subExpression, null, null);
    }

    /**
     * Constructs a new instance.
     *
     * @param subExpression the expression who's resulting node should be altered
     * @param value the value that should be set on the node. Null, if the node's value should not be changed.
     */
    public ExpressionLet(Expression subExpression, Object value) {
        this(null, subExpression, value, null);
    }

    /**
     * Constructs a new instance.
     *
     * @param name the name that should be set on the node. Null, if the node's name should not be changed.
     * @param subExpression the expression who's resulting node should be altered
     * @param value the value that should be set on the node. Null, if the node's value should not be changed.
     * @param tag the tag that should be set on the node. Null, if the node's tag should not be changed.
     */
    public ExpressionLet(String name, Expression subExpression, Object value, Object tag) {
        super();
        if (name != null) {
            this.name = name;
        }
        this.subExpression = subExpression;
        if (value != null) {
            this.value = value;
        }
        if (tag != null) {
            this.tag = tag;
        }
    }

    @Override
    ASTNode parse(ResourceBundle language, TokenList list) {
        ASTNode node = subExpression.parse(language, list);
        if (node.isSuccess()) {
            node.setName(name);
            if (value != null) {
                node.setValue(value);
            }
            if (tag != null) {
                node.setTag(tag);
            }
        }
        return node;
    }

    @Override
    List<Expression> getChildExpressions() {
        return Collections.unmodifiableList(Arrays.asList(subExpression));
    }

    @Override
    boolean collectFirsts(ResourceBundle language, Set<String> firsts) {
        return subExpression.collectFirsts(language, firsts);
    }

    @Override
    public String toString() {
        return "let(\"" + name + "\", " + subExpression.toString() + ", \"" + value + "\", \"" + tag + "\")";
    }

    /**
     * @return the subExpression
     */
    public Expression getSubExpression() {
        return subExpression;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @return the tag
     */
    public Object getTag() {
        return tag;
    }
}
