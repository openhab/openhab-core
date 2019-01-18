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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Base class for all expressions.
 *
 * @author Tilman Kamp - Initial contribution and API
 *
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

    abstract boolean collectFirsts(ResourceBundle language, HashSet<String> firsts);

    HashSet<String> getFirsts(ResourceBundle language) {
        HashSet<String> firsts = new HashSet<String>();
        collectFirsts(language, firsts);
        return firsts;
    }
}
