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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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
    private static final Path DEPENDENCY = Path.of("depFile");
    private static final Path DEPENDENCY2 = Path.of("depFile2");

    private @NonNullByDefault({}) AbstractScriptDependencyTracker scriptDependencyTracker;
    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @NonNullByDefault({}) @TempDir Path rootWatchPath;
    private @NonNullByDefault({}) Path depPath;
    private @NonNullByDefault({}) Path depPath2;

    @BeforeEach
    public void setup() throws IOException {
        when(watchServiceMock.getWatchPath()).thenReturn(rootWatchPath);
        scriptDependencyTracker = new AbstractScriptDependencyTracker(watchServiceMock, WATCH_DIR) {
        };

        depPath = rootWatchPath.resolve(WATCH_DIR).resolve(DEPENDENCY);
        depPath2 = rootWatchPath.resolve(WATCH_DIR).resolve(DEPENDENCY2);

        Files.createFile(depPath);

        Files.createFile(depPath2);
    }

    @AfterEach
    public void tearDown() {
        scriptDependencyTracker.deactivate();
    }

    @Test
    public void testScriptLibraryWatcherIsCreatedAndActivated() {
        verify(watchServiceMock).registerListener(eq(scriptDependencyTracker), eq(rootWatchPath.resolve(WATCH_DIR)));
    }

    @Test
    public void testScriptLibraryWatchersIsDeactivatedOnShutdown() {
        scriptDependencyTracker.deactivate();

        verify(watchServiceMock).unregisterListener(eq(scriptDependencyTracker));
    }

    @Test
    public void testDependencyChangeIsForwardedToMultipleListeners() throws IOException {
        ScriptDependencyTracker.Listener listener1 = mock(ScriptDependencyTracker.Listener.class);
        ScriptDependencyTracker.Listener listener2 = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener1);
        scriptDependencyTracker.addChangeTracker(listener2);

        scriptDependencyTracker.startTracking("scriptId", depPath.toString());
        scriptDependencyTracker.processWatchEvent(WatchService.Kind.CREATE, DEPENDENCY);

        verify(listener1).onDependencyChange(eq("scriptId"));
        verify(listener2).onDependencyChange(eq("scriptId"));
        verifyNoMoreInteractions(listener1);
        verifyNoMoreInteractions(listener2);
    }

    @Test
    public void testDependencyChangeIsForwardedForMultipleScriptIds() {
        ScriptDependencyTracker.Listener listener = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener);

        scriptDependencyTracker.startTracking("scriptId1", depPath.toString());
        scriptDependencyTracker.startTracking("scriptId2", depPath.toString());
        scriptDependencyTracker.processWatchEvent(WatchService.Kind.MODIFY, DEPENDENCY);

        verify(listener).onDependencyChange(eq("scriptId1"));
        verify(listener).onDependencyChange(eq("scriptId2"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testDependencyChangeIsForwardedForMultipleDependencies() {
        ScriptDependencyTracker.Listener listener = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener);

        scriptDependencyTracker.startTracking("scriptId", depPath.toString());
        scriptDependencyTracker.startTracking("scriptId", depPath2.toString());
        scriptDependencyTracker.processWatchEvent(WatchService.Kind.MODIFY, DEPENDENCY);
        scriptDependencyTracker.processWatchEvent(WatchService.Kind.DELETE, DEPENDENCY2);

        verify(listener, times(2)).onDependencyChange(eq("scriptId"));
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void testDependencyChangeIsForwardedForCorrectDependencies() {
        ScriptDependencyTracker.Listener listener = mock(ScriptDependencyTracker.Listener.class);

        scriptDependencyTracker.addChangeTracker(listener);

        scriptDependencyTracker.startTracking("scriptId1", depPath.toString());
        scriptDependencyTracker.startTracking("scriptId2", depPath2.toString());

        scriptDependencyTracker.processWatchEvent(WatchService.Kind.CREATE, DEPENDENCY);

        verify(listener).onDependencyChange(eq("scriptId1"));
        verifyNoMoreInteractions(listener);
    }
}
