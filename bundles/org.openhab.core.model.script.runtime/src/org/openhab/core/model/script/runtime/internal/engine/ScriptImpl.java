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
package org.openhab.core.model.script.runtime.internal.engine;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;
import org.eclipse.xtext.xbase.interpreter.IExpressionInterpreter;
import org.openhab.core.model.script.engine.Script;
import org.openhab.core.model.script.engine.ScriptExecutionException;

import com.google.inject.Inject;

/**
 * This is the default implementation of a {@link Script}.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class ScriptImpl implements Script {

    private XExpression xExpression;

    @Inject
    public ScriptImpl() {
    }

    /* package-local */
    void setXExpression(XExpression xExpression) {

        this.xExpression = xExpression;
    }

    /* package-local */
    XExpression getXExpression() {
        return xExpression;
    }

    @Override
    public Object execute() throws ScriptExecutionException {
        if (xExpression != null) {
            Resource resource = xExpression.eResource();
            IEvaluationContext evaluationContext = null;
            if (resource instanceof XtextResource) {
                IResourceServiceProvider provider = ((XtextResource) resource).getResourceServiceProvider();
                evaluationContext = provider.get(IEvaluationContext.class);
            }
            return execute(evaluationContext);
        } else {
            throw new ScriptExecutionException("Script does not contain any expression");
        }
    }

    @Override
    public Object execute(final IEvaluationContext evaluationContext) throws ScriptExecutionException {
        if (xExpression != null) {
            Resource resource = xExpression.eResource();
            IExpressionInterpreter interpreter = null;
            if (resource instanceof XtextResource) {
                IResourceServiceProvider provider = ((XtextResource) resource).getResourceServiceProvider();
                interpreter = provider.get(IExpressionInterpreter.class);
            }
            if (interpreter == null) {
                throw new ScriptExecutionException("Script interpreter couldn't be obtain");
            }
            try {
                IEvaluationResult result = interpreter.evaluate(xExpression, evaluationContext,
                        CancelIndicator.NullImpl);
                if (result == null) {
                    // this can only happen on an InterpreterCancelledException,
                    // i.e. NEVER ;-)
                    return null;
                }
                if (result.getException() != null) {
                    throw new ScriptExecutionException(result.getException().getMessage(), result.getException());
                }
                return result.getResult();
            } catch (Throwable e) {
                if (e instanceof ScriptExecutionException) {
                    throw (ScriptExecutionException) e;
                } else {
                    throw new ScriptExecutionException(
                            "An error occurred during the script execution: " + e.getMessage(), e);
                }
            }
        } else {
            throw new ScriptExecutionException("Script does not contain any expression");
        }
    }
}
