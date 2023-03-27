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
package org.openhab.core.automation.module.script;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.script.Compilable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.transform.Transformation;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationRegistry;

/**
 * The {@link ScriptTransformationServiceTest} holds tests for the {@link ScriptTransformationService}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ScriptTransformationServiceTest {
    private static final String SCRIPT_LANGUAGE = "customDsl";
    private static final String SCRIPT_UID = "scriptUid." + SCRIPT_LANGUAGE;
    private static final String INVALID_SCRIPT_UID = "invalidScriptUid";

    private static final String INLINE_SCRIPT = "|inlineScript";

    private static final String SCRIPT = "script";
    private static final String SCRIPT_OUTPUT = "output";

    private static final Transformation TRANSFORMATION_CONFIGURATION = new Transformation(SCRIPT_UID, "label",
            SCRIPT_LANGUAGE, Map.of(Transformation.FUNCTION, SCRIPT));
    private static final Transformation INVALID_TRANSFORMATION_CONFIGURATION = new Transformation(INVALID_SCRIPT_UID,
            "label", "invalid", Map.of(Transformation.FUNCTION, SCRIPT));

    private @Mock @NonNullByDefault({}) TransformationRegistry transformationRegistry;
    private @Mock @NonNullByDefault({}) ScriptEngineManager scriptEngineManager;
    private @Mock @NonNullByDefault({}) ScriptEngineContainer scriptEngineContainer;
    private @Mock @NonNullByDefault({}) ScriptEngine scriptEngine;
    private @Mock @NonNullByDefault({}) ScriptContext scriptContext;

    private @NonNullByDefault({}) ScriptTransformationService service;

    @BeforeEach
    public void setUp() throws ScriptException {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ScriptTransformationService.SCRIPT_TYPE_PROPERTY_NAME, SCRIPT_LANGUAGE);
        service = new ScriptTransformationService(transformationRegistry, scriptEngineManager, properties);

        when(scriptEngineManager.createScriptEngine(eq(SCRIPT_LANGUAGE), any())).thenReturn(scriptEngineContainer);
        when(scriptEngineManager.isSupported(anyString()))
                .thenAnswer(arguments -> SCRIPT_LANGUAGE.equals(arguments.getArgument(0)));
        when(scriptEngineContainer.getScriptEngine()).thenReturn(scriptEngine);
        when(scriptEngine.eval(SCRIPT)).thenReturn("output");
        when(scriptEngine.getContext()).thenReturn(scriptContext);

        when(transformationRegistry.get(anyString())).thenAnswer(arguments -> {
            String scriptUid = arguments.getArgument(0);
            if (SCRIPT_UID.equals(scriptUid)) {
                return TRANSFORMATION_CONFIGURATION;
            } else if (INVALID_SCRIPT_UID.equals(scriptUid)) {
                return INVALID_TRANSFORMATION_CONFIGURATION;
            } else {
                return null;
            }
        });
    }

    @Test
    public void success() throws TransformationException {
        String returnValue = Objects.requireNonNull(service.transform(SCRIPT_UID, "input"));

        assertThat(returnValue, is(SCRIPT_OUTPUT));
    }

    @Test
    public void scriptExecutionParametersAreInjectedIntoEngineContext() throws TransformationException {
        service.transform(SCRIPT_UID + "?param1=value1&param2=value2", "input");

        verify(scriptContext).setAttribute(eq("input"), eq("input"), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute(eq("param1"), eq("value1"), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute(eq("param2"), eq("value2"), eq(ScriptContext.ENGINE_SCOPE));
        verifyNoMoreInteractions(scriptContext);
    }

    @Test
    public void scriptExecutionParametersAreDecoded() throws TransformationException {
        service.transform(SCRIPT_UID + "?param1=%26amp;&param2=%3dvalue", "input");

        verify(scriptContext).setAttribute(eq("input"), eq("input"), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute(eq("param1"), eq("&amp;"), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute(eq("param2"), eq("=value"), eq(ScriptContext.ENGINE_SCOPE));
        verifyNoMoreInteractions(scriptContext);
    }

    @Test
    public void scriptSetAttributesBeforeCompiling() throws TransformationException, ScriptException {
        abstract class CompilableScriptEngine implements ScriptEngine, Compilable {
        }
        scriptEngine = mock(CompilableScriptEngine.class);

        when(scriptEngineContainer.getScriptEngine()).thenReturn(scriptEngine);
        when(scriptEngine.getContext()).thenReturn(scriptContext);

        InOrder inOrder = inOrder(scriptContext, scriptEngine);

        service.transform(SCRIPT_UID + "?param1=value1", "input");

        inOrder.verify(scriptContext, times(2)).setAttribute(anyString(), anyString(), eq(ScriptContext.ENGINE_SCOPE));
        inOrder.verify((Compilable) scriptEngine).compile(SCRIPT);
        inOrder.verify(scriptEngine).eval(SCRIPT);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void invalidScriptExecutionParametersAreDiscarded() throws TransformationException {
        service.transform(SCRIPT_UID + "?param1=value1&invalid", "input");

        verify(scriptContext).setAttribute(eq("input"), eq("input"), eq(ScriptContext.ENGINE_SCOPE));
        verify(scriptContext).setAttribute(eq("param1"), eq("value1"), eq(ScriptContext.ENGINE_SCOPE));
        verifyNoMoreInteractions(scriptContext);
    }

    @Test
    public void scriptsAreCached() throws TransformationException {
        service.transform(SCRIPT_UID, "input");
        service.transform(SCRIPT_UID, "input");

        verify(transformationRegistry).get(SCRIPT_UID);
    }

    @Test
    public void scriptCacheInvalidatedAfterChange() throws TransformationException {
        service.transform(SCRIPT_UID, "input");
        service.updated(TRANSFORMATION_CONFIGURATION, TRANSFORMATION_CONFIGURATION);
        service.transform(SCRIPT_UID, "input");

        verify(transformationRegistry, times(2)).get(SCRIPT_UID);
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

    @Test
    public void inlineScriptProperlyProcessed() throws TransformationException, ScriptException {
        service.transform(INLINE_SCRIPT, "input");

        verify(scriptEngine).eval(INLINE_SCRIPT.substring(1));
    }
}
