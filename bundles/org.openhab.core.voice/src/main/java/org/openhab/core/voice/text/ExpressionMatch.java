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

import java.util.ResourceBundle;
import java.util.Set;

/**
 * Expression that successfully parses, if a given string constant is found. This class is immutable.
 *
 * @author Tilman Kamp - Initial contribution
 */
public final class ExpressionMatch extends Expression {

    private String pattern;

    /**
     * Constructs a new instance.
     *
     * @param pattern the token that has to match for successful parsing
     */
    public ExpressionMatch(String pattern) {
        super();
        this.pattern = pattern;
    }

    @Override
    ASTNode parse(ResourceBundle language, TokenList list) {
        ASTNode node = new ASTNode();
        node.setSuccess(list.checkHead(pattern));
        if (node.isSuccess()) {
            node.setRemainingTokens(list.skipHead());
            node.setValue(pattern);
            node.setChildren(new ASTNode[0]);
            generateValue(node);
        }
        return node;
    }

    @Override
    boolean collectFirsts(ResourceBundle language, Set<String> firsts) {
        firsts.add(pattern);
        return true;
    }

    @Override
    public String toString() {
        return "match(\"" + pattern + "\")";
    }

    /**
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }
}
