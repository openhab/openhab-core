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
package org.openhab.core.automation.module.script;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Objects;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.transform.TransformationConfiguration;
import org.openhab.core.transform.TransformationConfigurationRegistry;
import org.openhab.core.transform.TransformationException;

/**
 * The {@link ScriptTransformationServiceTest} holds tests for the {@link ScriptTransformationService}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScriptTransformationServiceTest {
    private static final String SCRIPT_TYPE = "script";
    private static final String SCRIPT_UID = "scriptUid";
    private static final String SCRIPT = "script";
    private static final String SCRIPT_OUTPUT = "output";

    private static final TransformationConfiguration TRANSFORMATION_CONFIGURATION = new TransformationConfiguration(
            SCRIPT_UID, "label", "script", null, SCRIPT);

    private @Mock @NonNullByDefault({}) TransformationConfigurationRegistry transformationConfigurationRegistry;
    private @Mock @NonNullByDefault({}) ScriptEngineManager scriptEngineManager;
    private @Mock @NonNullByDefault({}) ScriptEngineContainer scriptEngineContainer;
    private @Mock @NonNullByDefault({}) ScriptEngine scriptEngine;
    private @Mock @NonNullByDefault({}) ScriptContext scriptContext;

    private @NonNullByDefault({}) ScriptTransformationService service;

    @BeforeEach
    public void setUp() throws ScriptException {
        service = new ScriptTransformationService(transformationConfigurationRegistry, scriptEngineManager);

        when(scriptEngineManager.createScriptEngine(eq(SCRIPT_TYPE), any())).thenReturn(scriptEngineContainer);
        when(scriptEngineManager.isSupported(anyString()))
                .thenAnswer(scriptType -> SCRIPT_TYPE.equals(scriptType.getArgument(0)));
        when(scriptEngineContainer.getScriptEngine()).thenReturn(scriptEngine);
        when(scriptEngine.eval(SCRIPT)).thenReturn("output");
        when(scriptEngine.getContext()).thenReturn(scriptContext);

        when(transformationConfigurationRegistry.get(anyString())).thenAnswer(
                scriptUid -> SCRIPT_UID.equals(scriptUid.getArgument(0)) ? TRANSFORMATION_CONFIGURATION : null);
    }

    @Test
    public void success() throws TransformationException {
        String returnValue = Objects.requireNonNull(service.transform(SCRIPT_UID, "input"));

        assertThat(returnValue, is(SCRIPT_OUTPUT));
    }

    @Test
    public void scriptExecutionParametersAreInjectedIntoEngineContext() throws TransformationException {
        service.transform(SCRIPT_UID + "?param1=value1&param2=value2", "input");

        verify(scriptContext).setAttribute(eq("inputString"), eq("input"), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute(eq("param1"), eq("value1"), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute(eq("param2"), eq("value2"), eq(ScriptContext.ENGINE_SCOPE));
        verifyNoMoreInteractions(scriptContext);
    }

    @Test
    public void invalidScriptExecutionParametersAreDiscarded() throws TransformationException {
        service.transform(SCRIPT_UID + "?param1=value1&invalid", "input");

        verify(scriptContext).setAttribute(eq("inputString"), eq("input"), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute(eq("param1"), eq("value1"), eq(ScriptContext.ENGINE_SCOPE));
        verifyNoMoreInteractions(scriptContext);
    }

    @Test
    public void scriptsAreCached() throws TransformationException {
        service.transform(SCRIPT_UID, "input");
        service.transform(SCRIPT_UID, "input");

        verify(transformationConfigurationRegistry).get(SCRIPT_UID);
    }

    @Test
    public void scriptCacheInvalidatedAfterChange() throws TransformationException {
        service.transform(SCRIPT_UID, "input");
        service.updated(TRANSFORMATION_CONFIGURATION, TRANSFORMATION_CONFIGURATION);
        service.transform(SCRIPT_UID, "input");

        verify(transformationConfigurationRegistry, times(2)).get(SCRIPT_UID);
    }

    @Test
    public void unknownScriptTypeThrowsException() {
        when(scriptEngineManager.isSupported(anyString())).thenReturn(false);
        TransformationException e = assertThrows(TransformationException.class,
                () -> service.transform(SCRIPT_UID, "input"));

        assertThat(e.getMessage(), is("Script type 'script' is not supported by any available script engine."));
    }

    @Test
    public void unknownScriptUidThrowsException() {
        TransformationException e = assertThrows(TransformationException.class,
                () -> service.transform("foo", "input"));

        assertThat(e.getMessage(), is("Could not get script for UID 'foo'."));
    }

    @Test
    public void scriptExceptionResultsInTransformationException() throws ScriptException {
        when(scriptEngine.eval(SCRIPT)).thenThrow(new ScriptException("exception"));

        TransformationException e = assertThrows(TransformationException.class,
                () -> service.transform(SCRIPT_UID, "input"));

        assertThat(e.getMessage(), is("Failed to execute script."));
        assertThat(e.getCause(), instanceOf(ScriptException.class));
        assertThat(e.getCause().getMessage(), is("exception"));
    }
}
