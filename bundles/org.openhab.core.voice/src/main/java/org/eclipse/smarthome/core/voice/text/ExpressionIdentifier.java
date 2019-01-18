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

import java.util.HashSet;
import java.util.ResourceBundle;

/**
 * Expression that successfully parses, if a thing identifier token is found. This class is immutable.
 *
 * @author Tilman Kamp - Initial contribution and API
 *
 */
public final class ExpressionIdentifier extends Expression {

    private AbstractRuleBasedInterpreter interpreter;
    private Expression stopper;

    /**
     * Constructs a new instance.
     *
     * @param interpreter the interpreter it belongs to. Used for dynamically fetching item name tokens
     */
    public ExpressionIdentifier(AbstractRuleBasedInterpreter interpreter) {
        this(interpreter, null);
    }

    /**
     * Constructs a new instance.
     *
     * @param interpreter the interpreter it belongs to. Used for dynamically fetching item name tokens
     * @param stopper Expression that should not match, if the current token should be accepted as identifier
     */
    public ExpressionIdentifier(AbstractRuleBasedInterpreter interpreter, Expression stopper) {
        super();
        this.interpreter = interpreter;
        this.stopper = stopper;
    }

    @Override
    ASTNode parse(ResourceBundle language, TokenList list) {
        ASTNode node = new ASTNode();
        node.setSuccess(list.size() > 0 && (stopper == null || !stopper.parse(language, list).isSuccess()));
        if (node.isSuccess()) {
            node.setRemainingTokens(list.skipHead());
            node.setValue(list.head());
            node.setChildren(new ASTNode[0]);
            generateValue(node);
        }
        return node;
    }

    @Override
    boolean collectFirsts(ResourceBundle language, HashSet<String> firsts) {
        HashSet<String> f = new HashSet<String>(interpreter.getAllItemTokens(language.getLocale()));
        if (stopper != null) {
            f.removeAll(stopper.getFirsts(language));
        }
        firsts.addAll(f);
        return true;
    }

    @Override
    public String toString() {
        return "identifier(stop=" + stopper + ")";
    }

    /**
     * @return the interpreter
     */
    public AbstractRuleBasedInterpreter getInterpreter() {
        return interpreter;
    }

    /**
     * @return the stopper expression
     */
    public Expression getStopper() {
        return stopper;
    }
}
