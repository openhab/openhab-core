/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.voice.DialogContext;

/**
 * Represents an expression plus action code that will be executed after successful parsing. This class is immutable and
 * deriving classes should conform to this principle.
 *
 * @author Tilman Kamp - Initial contribution
 */
@NonNullByDefault
public abstract class Rule {

    private Expression expression;

    /**
     * Constructs a new instance.
     *
     * @param expression the expression that has to parse successfully, before {@link interpretAST} is called
     */
    public Rule(Expression expression) {
        this.expression = expression;
    }

    /**
     * Will get called after the expression was successfully parsed.
     *
     * @param language a resource bundle that can be used for looking up common localized response phrases
     * @param node the resulting AST node of the parse run. To be used as input.
     * @return
     */
    public abstract InterpretationResult interpretAST(ResourceBundle language, ASTNode node,
            @Nullable DialogContext dialogContext);

    InterpretationResult execute(ResourceBundle language, TokenList list, @Nullable DialogContext dialogContext) {
        ASTNode node = expression.parse(language, list);
        if (node.isSuccess() && node.getRemainingTokens().eof()) {
            return interpretAST(language, node, dialogContext);
        }
        return InterpretationResult.SYNTAX_ERROR;
    }

    /**
     * @return the expression
     */
    public Expression getExpression() {
        return expression;
    }
}
