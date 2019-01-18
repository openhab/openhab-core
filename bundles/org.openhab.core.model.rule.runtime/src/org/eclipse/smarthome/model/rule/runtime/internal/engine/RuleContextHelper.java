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
package org.eclipse.smarthome.model.rule.runtime.internal.engine;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.smarthome.model.rule.rules.Rule;
import org.eclipse.smarthome.model.rule.rules.RuleModel;
import org.eclipse.smarthome.model.rule.rules.VariableDeclaration;
import org.eclipse.smarthome.model.script.engine.ScriptEngine;
import org.eclipse.smarthome.model.script.engine.ScriptExecutionException;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Helper class to deal with rule evaluation contexts.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@SuppressWarnings("restriction")
public class RuleContextHelper {

    /**
     * Retrieves the evaluation context (= set of variables) for a rule. The context is shared with all rules in the
     * same model (= rule file).
     *
     * @param rule the rule to get the context for
     * @return the evaluation context
     */
    public static synchronized IEvaluationContext getContext(Rule rule, Injector injector) {
        Logger logger = LoggerFactory.getLogger(RuleContextHelper.class);
        RuleModel ruleModel = (RuleModel) rule.eContainer();

        // check if a context already exists on the resource
        for (Adapter adapter : ruleModel.eAdapters()) {
            if (adapter instanceof RuleContextAdapter) {
                return ((RuleContextAdapter) adapter).getContext();
            }
        }
        Provider<IEvaluationContext> contextProvider = injector.getProvider(IEvaluationContext.class);
        // no evaluation context found, so create a new one
        ScriptEngine scriptEngine = injector.getInstance(ScriptEngine.class);
        if (scriptEngine != null) {
            IEvaluationContext evaluationContext = contextProvider.get();
            for (VariableDeclaration var : ruleModel.getVariables()) {
                try {
                    Object initialValue = var.getRight() == null ? null
                            : scriptEngine.newScriptFromXExpression(var.getRight()).execute();
                    evaluationContext.newValue(QualifiedName.create(var.getName()), initialValue);
                } catch (ScriptExecutionException e) {
                    logger.warn("Variable '{}' on rule file '{}' cannot be initialized with value '{}': {}",
                            new Object[] { var.getName(), ruleModel.eResource().getURI().path(),
                                    var.getRight().toString(), e.getMessage() });
                }
            }
            ruleModel.eAdapters().add(new RuleContextAdapter(evaluationContext));
            return evaluationContext;
        } else {
            logger.debug("Rule variables of rule {} cannot be evaluated as no scriptengine is available!",
                    ruleModel.eResource().getURI().path());
            return contextProvider.get();
        }
    }

    /**
     * Inner class that wraps an evaluation context into an EMF adapters
     */
    private static class RuleContextAdapter extends EContentAdapter {

        private final IEvaluationContext context;

        public RuleContextAdapter(IEvaluationContext context) {
            this.context = context;
        }

        public IEvaluationContext getContext() {
            return context;
        }

    }

}
