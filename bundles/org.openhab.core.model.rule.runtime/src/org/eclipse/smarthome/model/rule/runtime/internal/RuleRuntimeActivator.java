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
package org.eclipse.smarthome.model.rule.runtime.internal;

import org.eclipse.smarthome.model.core.ModelParser;
import org.eclipse.smarthome.model.rule.RulesStandaloneSetup;
import org.eclipse.smarthome.model.script.ScriptServiceUtil;
import org.eclipse.smarthome.model.script.engine.ScriptEngine;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of the default OSGi bundle activator
 *
 * @author Kai Kreuzer - Initial contribution and API
 */
@Component(immediate = true, service = { ModelParser.class, RuleRuntimeActivator.class })
public class RuleRuntimeActivator implements ModelParser {

    private final Logger logger = LoggerFactory.getLogger(RuleRuntimeActivator.class);
    private ScriptServiceUtil scriptServiceUtil;
    private ScriptEngine scriptEngine;

    public void activate(BundleContext bc) throws Exception {
        RulesStandaloneSetup.doSetup(scriptServiceUtil, scriptEngine);
        logger.debug("Registered 'rule' configuration parser");
    }

    public void deactivate() throws Exception {
        RulesStandaloneSetup.unregister();
    }

    @Override
    public String getExtension() {
        return "rules";
    }

    @Reference
    protected void setScriptServiceUtil(ScriptServiceUtil scriptServiceUtil) {
        this.scriptServiceUtil = scriptServiceUtil;
    }

    protected void unsetScriptServiceUtil(ScriptServiceUtil scriptServiceUtil) {
        this.scriptServiceUtil = null;
    }

    @Reference
    public void setScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public void unsetScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = null;
    }

}
