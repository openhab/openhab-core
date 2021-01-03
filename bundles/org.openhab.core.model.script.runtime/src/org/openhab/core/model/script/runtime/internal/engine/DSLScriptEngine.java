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

import java.io.Reader;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.impl.DefaultEvaluationContext;
import org.openhab.core.items.events.ItemEvent;
import org.openhab.core.model.script.engine.Script;
import org.openhab.core.model.script.engine.ScriptExecutionException;
import org.openhab.core.model.script.engine.ScriptParsingException;
import org.openhab.core.model.script.jvmmodel.ScriptJvmModelInferrer;
import org.openhab.core.model.script.runtime.DSLScriptContextProvider;
import org.openhab.core.thing.events.ChannelTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic implementation of the {@link javax.script.ScriptEngine} interface for using DSL scripts
 * within a jsr223 scripting context in Java.
 * Most methods are left empty, because they aren't used in our rule engine.
 * The most important methods are the ones that return metadata about the script engine factory.
 *
 * Note: This class is not marked as NonNullByDefault as almost all parameters of all methods are
 * nullable as the interface is declared without null annotations.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class DSLScriptEngine implements javax.script.ScriptEngine {

    private static final String OUTPUT_EVENT = "event";

    public static final String MIMETYPE_OPENHAB_DSL_RULE = "application/vnd.openhab.dsl.rule";

    private static final Map<String, String> IMPLICIT_VARS = Map.of("command",
            ScriptJvmModelInferrer.VAR_RECEIVED_COMMAND, "state", ScriptJvmModelInferrer.VAR_NEW_STATE, "newState",
            ScriptJvmModelInferrer.VAR_NEW_STATE, "oldState", ScriptJvmModelInferrer.VAR_PREVIOUS_STATE,
            "triggeringItem", ScriptJvmModelInferrer.VAR_TRIGGERING_ITEM);

    private final Logger logger = LoggerFactory.getLogger(DSLScriptEngine.class);

    private final org.openhab.core.model.script.engine.ScriptEngine scriptEngine;
    private final @Nullable DSLScriptContextProvider contextProvider;
    private final ScriptContext context = new SimpleScriptContext();

    private @Nullable Script parsedScript;

    public DSLScriptEngine(org.openhab.core.model.script.engine.ScriptEngine scriptEngine,
            @Nullable DSLScriptContextProvider contextProvider) {
        this.scriptEngine = scriptEngine;
        this.contextProvider = contextProvider;
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        return null;
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return null;
    }

    @Override
    public Object eval(String script) throws ScriptException {
        String modelName = null;
        try {
            IEvaluationContext specificContext = null;
            org.openhab.core.model.script.engine.Script s = null;
            if (script.stripLeading().startsWith(DSLScriptContextProvider.CONTEXT_IDENTIFIER)) {
                String contextString = script.stripLeading().substring(
                        DSLScriptContextProvider.CONTEXT_IDENTIFIER.length(), script.stripLeading().indexOf('\n'));
                if (contextString.contains("-")) {
                    int indexLastDash = contextString.lastIndexOf('-');
                    modelName = contextString.substring(0, indexLastDash);
                    String ruleIndex = contextString.substring(indexLastDash + 1);
                    if (contextProvider != null) {
                        DSLScriptContextProvider cp = contextProvider;
                        logger.debug("Script uses context '{}'.", contextString);
                        specificContext = cp.getContext(modelName);
                        XExpression xExpression = cp.getParsedScript(modelName, ruleIndex);
                        if (xExpression != null) {
                            s = scriptEngine.newScriptFromXExpression(xExpression);
                        } else {
                            logger.warn("No pre-parsed script found for {}-{}.", modelName, ruleIndex);
                            return null;
                        }
                    } else {
                        logger.error("Script references context '{}', but no context provider is registered!",
                                contextString);
                        return null;
                    }
                } else {
                    logger.error("Script has an invalid context reference '{}'!", contextString);
                    return null;
                }
            } else {
                s = parsedScript;
                if (s == null) {
                    s = scriptEngine.newScriptFromString(script);
                    parsedScript = s;
                }
            }
            IEvaluationContext evalContext = createEvaluationContext(s, specificContext);
            return s.execute(evalContext);
        } catch (ScriptExecutionException | ScriptParsingException e) {
            // in case of error, drop the cached script to make sure, it is re-resolved.
            parsedScript = null;
            throw new ScriptException(e.getMessage(), modelName, -1);
        }
    }

    private DefaultEvaluationContext createEvaluationContext(Script script, IEvaluationContext specificContext) {
        IEvaluationContext parentContext = specificContext;
        if (specificContext == null && script instanceof ScriptImpl) {
            XExpression xExpression = ((ScriptImpl) script).getXExpression();
            if (xExpression != null) {
                Resource resource = xExpression.eResource();
                if (resource instanceof XtextResource) {
                    IResourceServiceProvider provider = ((XtextResource) resource).getResourceServiceProvider();
                    parentContext = provider.get(IEvaluationContext.class);
                }
            }
        }
        DefaultEvaluationContext evalContext = new DefaultEvaluationContext(parentContext);
        for (Map.Entry<String, String> entry : IMPLICIT_VARS.entrySet()) {
            Object value = context.getAttribute(entry.getKey());
            if (value != null) {
                QualifiedName qn = QualifiedName.create(entry.getValue());
                if (evalContext.getValue(qn) == null) {
                    evalContext.newValue(qn, value);
                } else {
                    evalContext.assignValue(qn, value);
                }
            }
        }
        // now add specific implicit vars, where we have to map the right content
        Object value = context.getAttribute(OUTPUT_EVENT);
        if (value instanceof ChannelTriggeredEvent) {
            ChannelTriggeredEvent event = (ChannelTriggeredEvent) value;
            evalContext.newValue(QualifiedName.create(ScriptJvmModelInferrer.VAR_RECEIVED_EVENT), event.getEvent());
        }
        if (value instanceof ItemEvent) {
            ItemEvent event = (ItemEvent) value;
            evalContext.newValue(QualifiedName.create(ScriptJvmModelInferrer.VAR_TRIGGERING_ITEM_NAME),
                    event.getItemName());
        }

        return evalContext;
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return null;
    }

    @Override
    public Object eval(String script, Bindings n) throws ScriptException {
        return null;
    }

    @Override
    public Object eval(Reader reader, Bindings n) throws ScriptException {
        return null;
    }

    @Override
    public void put(String key, Object value) {

    }

    @Override
    public Object get(String key) {
        return null;
    }

    @Override
    public Bindings getBindings(int scope) {
        return null;
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {
    }

    @Override
    public Bindings createBindings() {
        return null;
    }

    @Override
    public ScriptContext getContext() {
        return context;
    }

    @Override
    public void setContext(ScriptContext context) {

    }

    @Override
    public ScriptEngineFactory getFactory() {
        return new ScriptEngineFactory() {

            @Override
            public ScriptEngine getScriptEngine() {
                return null;
            }

            @Override
            public String getProgram(String... statements) {
                return null;
            }

            @Override
            public Object getParameter(String key) {
                return null;
            }

            @Override
            public String getOutputStatement(String toDisplay) {
                return null;
            }

            @Override
            public List<String> getNames() {
                return null;
            }

            @Override
            public List<String> getMimeTypes() {
                return List.of(MIMETYPE_OPENHAB_DSL_RULE);
            }

            @Override
            public String getMethodCallSyntax(String obj, String m, String... args) {
                return null;
            }

            @Override
            public String getLanguageVersion() {
                return "v1";
            }

            @Override
            public String getLanguageName() {
                return "Rule DSL";
            }

            @Override
            public List<String> getExtensions() {
                return null;
            }

            @Override
            public String getEngineVersion() {
                return null;
            }

            @Override
            public String getEngineName() {
                return null;
            }
        };
    }

}
