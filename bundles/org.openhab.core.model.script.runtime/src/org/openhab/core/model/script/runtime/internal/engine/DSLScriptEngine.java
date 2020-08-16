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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.interpreter.IEvaluationContext;
import org.eclipse.xtext.xbase.interpreter.impl.DefaultEvaluationContext;
import org.openhab.core.model.script.engine.ScriptExecutionException;
import org.openhab.core.model.script.engine.ScriptParsingException;
import org.openhab.core.model.script.runtime.DSLScriptContextProvider;
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

    public static final String MIMETYPE_OPENHAB_DSL_RULE = "application/vnd.openhab.dsl.rule";

    private static final Map<String, String> implicitVars = Map.of("command", "receivedCommand", "event",
            "receivedEvent", "newState", "newState", "oldState", "previousState", "triggeringItem", "triggeringItem");

    private final Logger logger = LoggerFactory.getLogger(DSLScriptEngine.class);

    private final org.openhab.core.model.script.engine.ScriptEngine scriptEngine;
    private final @Nullable DSLScriptContextProvider contextProvider;
    private final ScriptContext context = new SimpleScriptContext();

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
                String[] segments = contextString.split("-");
                if (segments.length == 2) {
                    modelName = segments[0];
                    String ruleIndex = segments[1];
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
                s = scriptEngine.newScriptFromString(script);
            }
            IEvaluationContext evalContext = createEvaluationContext(specificContext);
            s.execute(evalContext);
        } catch (ScriptExecutionException | ScriptParsingException e) {
            throw new ScriptException(e.getMessage(), modelName, -1);
        }
        return null;
    }

    private DefaultEvaluationContext createEvaluationContext(IEvaluationContext specificContext) {
        DefaultEvaluationContext evalContext = new DefaultEvaluationContext(specificContext);
        for (Map.Entry<String, String> entry : implicitVars.entrySet()) {
            Object value = context.getAttribute(entry.getKey());
            if (value != null) {
                evalContext.newValue(QualifiedName.create(entry.getValue()), value);
            }
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
