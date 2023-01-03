/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openhab.core.automation.module.script.ScriptEngineFactory.CONTEXT_KEY_DEPENDENCY_LISTENER;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.module.script.ScriptDependencyTracker;
import org.openhab.core.automation.module.script.ScriptEngineFactory;

/**
 * The {@link ScriptEngineManagerImplTest} is a test class for the {@link ScriptEngineManagerImpl}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScriptEngineManagerImplTest {

    private static final String SUPPORTED_SCRIPT_TYPE = "supported";

    private @Mock @NonNullByDefault({}) ScriptExtensionManager scriptExtensionManagerMock;
    private @Mock @NonNullByDefault({}) ScriptEngineFactory scriptEngineFactoryMock;
    private @Mock @NonNullByDefault({}) ScriptEngine scriptEngineMock;
    private @Mock @NonNullByDefault({}) javax.script.ScriptEngineFactory internalScriptEngineFactoryMock;
    private @Mock @NonNullByDefault({}) ScriptDependencyTracker scriptDependencyTrackerMock;
    private @Mock @NonNullByDefault({}) ScriptContext scriptContextMock;
    private @Mock @NonNullByDefault({}) Consumer<String> dependencyListenerMock;

    private @NonNullByDefault({}) ScriptEngineManagerImpl scriptEngineManager;

    @BeforeEach
    public void setup() {
        when(scriptEngineMock.getFactory()).thenReturn(internalScriptEngineFactoryMock);
        when(scriptEngineMock.getContext()).thenReturn(scriptContextMock);

        when(scriptEngineFactoryMock.getScriptTypes()).thenReturn(List.of(SUPPORTED_SCRIPT_TYPE));
        when(scriptEngineFactoryMock.createScriptEngine(SUPPORTED_SCRIPT_TYPE)).thenReturn(scriptEngineMock);
        when(scriptEngineFactoryMock.getDependencyTracker()).thenReturn(scriptDependencyTrackerMock);
        when(scriptDependencyTrackerMock.getTracker(any())).thenReturn(dependencyListenerMock);

        scriptEngineManager = new ScriptEngineManagerImpl(scriptExtensionManagerMock);
        scriptEngineManager.addScriptEngineFactory(scriptEngineFactoryMock);
    }

    @Test
    public void testDependencyListenerIsProperlyHandled() {
        String engineIdentifier = "testIdentifier";
        String scriptContent = "testContent";

        InputStreamReader scriptContentReader = new InputStreamReader(
                new ByteArrayInputStream(scriptContent.getBytes(StandardCharsets.UTF_8)));

        scriptEngineManager.createScriptEngine(SUPPORTED_SCRIPT_TYPE, engineIdentifier);
        scriptEngineManager.loadScript(engineIdentifier, scriptContentReader);

        // verify the dependency tracker is requested
        verify(scriptDependencyTrackerMock).getTracker(eq(engineIdentifier));

        // verify dependency tracker is set in the context
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(scriptContextMock).setAttribute(eq(CONTEXT_KEY_DEPENDENCY_LISTENER), captor.capture(),
                eq(ScriptContext.ENGINE_SCOPE));

        Object captured = captor.getValue();
        assertThat(captured, is(dependencyListenerMock));

        // verify tracking is stopped when script engine is removed
        scriptEngineManager.removeEngine(engineIdentifier);
        verify(scriptDependencyTrackerMock).removeTracking(eq(engineIdentifier));
    }
}
