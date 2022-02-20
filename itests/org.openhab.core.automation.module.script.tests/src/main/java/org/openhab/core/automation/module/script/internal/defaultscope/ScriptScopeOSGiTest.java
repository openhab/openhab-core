/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.internal.defaultscope;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * This tests the script modules if the Nashorn JavaScript engine is available.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class ScriptScopeOSGiTest extends JavaOSGiTest {

    private static final boolean NASHORN_AVAILABLE = isNashornAvailable();

    private @NonNullByDefault({}) ScriptEngine engine;

    private final String path = "OH-INF/automation/jsr223/";
    private final String workingFile = "scopeWorking.js";
    private final String failureFile = "scopeFailure.js";

    /**
     * Returns if the Nashorn JavaScript engine is available based on the Java specification version property.
     * Nashorn has been removed from JDK 15 and onwards.
     *
     * @return {@code true} if Nashorn is available, {@code false} otherwise
     */
    private static boolean isNashornAvailable() {
        try {
            String javaVersion = System.getProperty("java.specification.version");
            return javaVersion == null ? false : Long.parseLong(javaVersion) < 15;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @BeforeEach
    public void init() {
        assumeTrue(NASHORN_AVAILABLE);

        ScriptEngineManager scriptManager = getService(ScriptEngineManager.class);
        ScriptEngineContainer container = scriptManager.createScriptEngine("js", "myJSEngine");
        engine = container.getScriptEngine();
    }

    @Test
    public void testScopeDefinesItemTypes() throws ScriptException, IOException {
        URL url = bundleContext.getBundle().getResource(path + workingFile);
        engine.eval(new InputStreamReader(url.openStream()));
    }

    @Test
    public void testScopeDoesNotDefineFoobar() throws ScriptException, IOException {
        URL url = bundleContext.getBundle().getResource(path + failureFile);
        assertThrows(ScriptException.class, () -> engine.eval(new InputStreamReader(url.openStream())));
    }
}
