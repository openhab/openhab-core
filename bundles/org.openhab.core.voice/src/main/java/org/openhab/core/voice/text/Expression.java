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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Base class for all expressions.
 *
 * @author Tilman Kamp - Initial contribution
 */
public abstract class Expression {

    Expression() {
    }

    abstract ASTNode parse(ResourceBundle language, TokenList list);

    void generateValue(ASTNode node) {
    }

    List<Expression> getChildExpressions() {
        return Collections.emptyList();
    }

    abstract boolean collectFirsts(ResourceBundle language, Set<String> firsts);

    Set<String> getFirsts(ResourceBundle language) {
        Set<String> firsts = new HashSet<>();
        collectFirsts(language, firsts);
        return firsts;
    }
}
