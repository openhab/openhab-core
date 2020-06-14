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

import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.openhab.core.model.script.engine.ScriptEngine;
import org.openhab.core.model.script.runtime.DSLScriptContextProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

/**
 * An implementation of {@link ScriptEngineFactory} for DSL scripts.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Component(service = ScriptEngineFactory.class)
public class DSLScriptEngineFactory implements ScriptEngineFactory {

    private static final String SCRIPT_TYPE = "dsl";

    private @NonNullByDefault({}) DSLScriptEngine dslScriptEngine;

    private final ScriptEngine scriptEngine;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected @Nullable DSLScriptContextProvider contextProvider;

    @Activate
    public DSLScriptEngineFactory(@Reference ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    @Activate
    protected void activate() {
        dslScriptEngine = new DSLScriptEngine(scriptEngine, contextProvider);
    }

    @Override
    public List<String> getScriptTypes() {
        return List.of(SCRIPT_TYPE, DSLScriptEngine.MIMETYPE_OPENHAB_DSL_RULE);
    }

    @Override
    public void scopeValues(javax.script.ScriptEngine scriptEngine, Map<String, Object> scopeValues) {
    }

    @Override
    public javax.script.@Nullable ScriptEngine createScriptEngine(String scriptType) {
        return dslScriptEngine;
    }

}
