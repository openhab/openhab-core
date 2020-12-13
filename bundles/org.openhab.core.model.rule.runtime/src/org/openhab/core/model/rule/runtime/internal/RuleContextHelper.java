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
package org.openhab.core.model.rule.runtime.internal;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.openhab.core.model.rule.RulesStandaloneSetup;
import org.openhab.core.model.rule.rules.RuleModel;
import org.openhab.core.model.rule.rules.VariableDeclaration;
import org.openhab.core.model.script.engine.ScriptEngine;
import org.openhab.core.model.script.engine.ScriptExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * Helper class to deal with rule evaluation contexts.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class RuleContextHelper {

    /**
     * Retrieves the evaluation context (= set of variables) for a ruleModel. The context is shared with all rules in
     * the
     * same model (= rule file).
     *
     * @param ruleModel the ruleModel to get the context for
     * @return the evaluation context
     */
    public static synchronized IEvaluationContext getContext(RuleModel ruleModel) {
        Logger logger = LoggerFactory.getLogger(RuleContextHelper.class);
        Injector injector = RulesStandaloneSetup.getInjector();

        // check if a context already exists on the resource
        for (Adapter adapter : ruleModel.eAdapters()) {
            if (adapter instanceof RuleContextAdapter) {
                return ((RuleContextAdapter) adapter).getContext();
            }
        }
        Provider<@NonNull IEvaluationContext> contextProvider = injector.getProvider(IEvaluationContext.class);
        // no evaluation context found, so create a new one
        ScriptEngine scriptEngine = injector.getInstance(ScriptEngine.class);
        IEvaluationContext evaluationContext = contextProvider.get();
        for (VariableDeclaration var : ruleModel.getVariables()) {
            try {
                Object initialValue = var.getRight() == null ? null
                        : scriptEngine.newScriptFromXExpression(var.getRight()).execute();
                evaluationContext.newValue(QualifiedName.create(var.getName()), initialValue);
            } catch (ScriptExecutionException e) {
                logger.warn("Variable '{}' on rule file '{}' cannot be initialized with value '{}': {}", var.getName(),
                        ruleModel.eResource().getURI().path(), var.getRight(), e.getMessage());
            }
        }
        ruleModel.eAdapters().add(new RuleContextAdapter(evaluationContext));
        return evaluationContext;
    }

    public static synchronized String getVariableDeclaration(RuleModel ruleModel) {
        StringBuilder vars = new StringBuilder();
        for (VariableDeclaration var : ruleModel.getVariables()) {
            vars.append(NodeModelUtils.findActualNodeFor(var).getText());
        }
        return vars.toString();
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
