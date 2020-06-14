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
package org.openhab.core.model.script.runtime;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;

/**
 * Interface of a provider that can provide Xbase-relevant object structures for
 * a purely string based script. This is required to support DSL rules, which
 * can have a context (variables) per file that is shared among multiple rules.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface DSLScriptContextProvider {

    /**
     * Identifier for scripts that are created from a DSL rule file
     */
    static final String CONTEXT_IDENTIFIER = "// context: ";

    /**
     * Returns the evaluation context, i.e. the current state of the variables of the rule file.
     *
     * @param contextName the filename of the rule file in question
     * @return the evaluation context
     */
    @Nullable
    IEvaluationContext getContext(String contextName);

    /**
     * Returns the {@link XExpression}, which is the readily parsed script. As it might refer
     * to variables from the rule file scope, this script cannot be parsed independently.
     *
     * @param contextName the filename of the rule file in question
     * @param ruleIndex the index of the rule within the file
     * @return the parsed script
     */
    @Nullable
    XExpression getParsedScript(String contextName, String ruleIndex);
}
