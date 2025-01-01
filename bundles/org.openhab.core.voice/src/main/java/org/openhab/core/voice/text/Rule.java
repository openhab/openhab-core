/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

/**
 * Represents an expression plus action code that will be executed after successful parsing. This class is immutable and
 * deriving classes should conform to this principle.
 *
 * @author Tilman Kamp - Initial contribution
 */
@NonNullByDefault
public abstract class Rule {

    private final Expression expression;
    private final AbstractRuleBasedInterpreter.ItemFilter itemFilter;
    private final boolean isForced;
    private final boolean isSilent;

    /**
     * Constructs a new instance.
     *
     * @param expression the expression that has to parse successfully, before {@link #interpretAST} is called
     * @param itemFilter Filters allowed items for rule.
     * @param isSilent Rule will emit no response on success.
     */
    public Rule(Expression expression, AbstractRuleBasedInterpreter.ItemFilter itemFilter, boolean isForced,
            boolean isSilent) {
        this.expression = expression;
        this.itemFilter = itemFilter;
        this.isSilent = isSilent;
        this.isForced = isForced;
    }

    /**
     * Will get called after the expression was successfully parsed.
     *
     * @param language a resource bundle that can be used for looking up common localized response phrases
     * @param node the resulting AST node of the parse run. To be used as input.
     * @param context for rule interpretation
     * @return
     */
    public abstract InterpretationResult interpretAST(ResourceBundle language, ASTNode node,
            InterpretationContext context);

    InterpretationResult execute(ResourceBundle language, TokenList list, @Nullable String locationItem) {
        ASTNode node = expression.parse(language, list);
        if (node.isSuccess() && node.getRemainingTokens().eof()) {
            return interpretAST(language, node,
                    new InterpretationContext(this.itemFilter, isForced, isSilent, locationItem));
        }
        return InterpretationResult.SYNTAX_ERROR;
    }

    /**
     * @return the expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Context for rule execution.
     *
     * @author Miguel Álvarez - Initial contribution
     *
     * @param itemFilter Restricts rule compatibility to allowed items.
     * @param locationItem Location item to prioritize item matches or null.
     */
    public record InterpretationContext(AbstractRuleBasedInterpreter.ItemFilter itemFilter, boolean isForced,
            boolean isSilent, @Nullable String locationItem) {
    }
}
