/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.defaultscope.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;

/**
 * This tests the script modules
 *
 * @author Kai Kreuzer - initial contribution
 *
 */
public class ScopeTest extends JavaOSGiTest {

    private ScriptEngine engine;

    private final String path = "ESH-INF" + File.separator + "automation" + File.separator + "jsr223";
    private final String workingFile = "scopeWorking.js";
    private final String failureFile = "scopeFailure.js";

    @Before
    public void init() {
        ScriptEngineManager scriptManager = getService(ScriptEngineManager.class);
        ScriptEngineContainer container = scriptManager.createScriptEngine("js", "myJSEngine");
        engine = container.getScriptEngine();
    }

    @Test
    public void testScopeDefinesItemTypes() throws ScriptException, IOException {
        URL url = bundleContext.getBundle().getResource(path + File.separator + workingFile);
        engine.eval(new InputStreamReader(url.openStream()));
    }

    @Test(expected = ScriptException.class)
    public void testScopeDoesNotDefineFoobar() throws ScriptException, IOException {
        URL url = bundleContext.getBundle().getResource(path + File.separator + failureFile);
        engine.eval(new InputStreamReader(url.openStream()));
    }
}
