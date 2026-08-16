/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.core.ModelParser;
import org.openhab.core.model.rule.RulesStandaloneSetup;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.model.script.engine.ScriptEngine;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.LoggerFactory;

/**
 * Addon of the default OSGi bundle activator
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class RuleRuntimeActivator implements ModelParser {

    @Activate
    public RuleRuntimeActivator(final @Reference ScriptEngine scriptEngine,
            final @Reference ScriptServiceUtil scriptServiceUtil) {
        RulesStandaloneSetup.doSetup(scriptServiceUtil, scriptEngine);
        LoggerFactory.getLogger(RuleRuntimeActivator.class).debug("Registered 'rule' configuration parser");
    }

    public void deactivate() throws Exception {
        RulesStandaloneSetup.unregister();
    }

    @Override
    public String getExtension() {
        return "rules";
    }
}
