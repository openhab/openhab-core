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
package org.openhab.core.automation.module.script.rulesupport.loader;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.module.script.ScriptDependencyTracker;
import org.openhab.core.service.WatchService;

/**
 * The {@link AbstractScriptDependencyTrackerTest} contains tests for the {@link AbstractScriptDependencyTracker}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AbstractScriptDependencyTrackerTest {

    private static final String WATCH_DIR = "test";

    private @NonNullByDefault({}) AbstractScriptDependencyTracker scriptDependencyTracker;
    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;

    @BeforeEach
    public void setup() {
        when(watchServiceMock.getWatchPath()).thenReturn(Path.of(""));
        scriptDependencyTracker = new AbstractScriptDependencyTracker(watchServiceMock, WATCH_DIR) {
        };
    }

    @AfterEach
    public void tearDown() {
        scriptDependencyTracker.deactivate();
    }

    @Test
    public void testScriptLibraryWatcherIsCreatedAndActivated() {
        verify(watchServiceMock).registerListener(eq(scriptDependencyTracker), eq(Path.of(WATCH_DIR)));
    }

    @Test
    public void testScriptLibraryWatchersIsDeactivatedOnShutdown() {
        scriptDependencyTracker.deactivate();

        verify(watchServiceMock).unregisterListener(eq(scriptDependencyTracker));
    }

    @Test
    public void testDependencyChangeIsForwardedToMultipleListeners() {
        ScriptDependencyTracker.Listener listener1 = mock(ScriptDependencyTracker.Listener.class);
        ScriptDependencyTracker.Listener listener2 = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener1);
        scriptDependencyTracker.addChangeTracker(listener2);

        scriptDependencyTracker.startTracking("scriptId", "depPath");
        scriptDependencyTracker.dependencyChanged("depPath");

        verify(listener1).onDependencyChange(eq("scriptId"));
        verify(listener2).onDependencyChange(eq("scriptId"));
        verifyNoMoreInteractions(listener1);
        verifyNoMoreInteractions(listener2);
    }

    @Test
    public void testDependencyChangeIsForwardedForMultipleScriptIds() {
        ScriptDependencyTracker.Listener listener = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener);

        scriptDependencyTracker.startTracking("scriptId1", "depPath");
        scriptDependencyTracker.startTracking("scriptId2", "depPath");

        scriptDependencyTracker.dependencyChanged("depPath");

        verify(listener).onDependencyChange(eq("scriptId1"));
        verify(listener).onDependencyChange(eq("scriptId2"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testDependencyChangeIsForwardedForMultipleDependencies() {
        ScriptDependencyTracker.Listener listener = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener);

        scriptDependencyTracker.startTracking("scriptId", "depPath1");
        scriptDependencyTracker.startTracking("scriptId", "depPath2");

        scriptDependencyTracker.dependencyChanged("depPath1");
        scriptDependencyTracker.dependencyChanged("depPath2");

        verify(listener, times(2)).onDependencyChange(eq("scriptId"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testDependencyChangeIsForwardedForCorrectDependencies() {
        ScriptDependencyTracker.Listener listener = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener);

        scriptDependencyTracker.startTracking("scriptId1", "depPath1");
        scriptDependencyTracker.startTracking("scriptId2", "depPath2");

        scriptDependencyTracker.dependencyChanged("depPath1");

        verify(listener).onDependencyChange(eq("scriptId1"));
        verifyNoMoreInteractions(listener);
    }
}
