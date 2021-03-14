/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.rulesupport.internal.loader;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.script.ScriptEngine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.openhab.core.automation.module.script.ScriptDependencyListener;
import org.openhab.core.automation.module.script.ScriptEngineContainer;
import org.openhab.core.automation.module.script.ScriptEngineManager;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.StartLevelService;
import org.opentest4j.AssertionFailedError;

/**
 * Test for Script File Watcher, covering differing start levels and dependency tracking
 *
 * @author Jonathan Gilbert - initial contribution
 */
class ScriptFileWatcherTest {

    private ScriptFileWatcher scriptFileWatcher;
    private ScriptEngineManager scriptEngineManager;
    private DependencyTracker dependencyTracker;
    private ReadyService readyService;

    @TempDir
    Path tempScriptDir;

    @BeforeEach
    public void setUp() {
        scriptEngineManager = mock(ScriptEngineManager.class);
        dependencyTracker = mock(DependencyTracker.class);
        readyService = mock(ReadyService.class);
        scriptFileWatcher = new ScriptFileWatcher(scriptEngineManager, dependencyTracker, readyService);
        scriptFileWatcher.activate();
    }

    @AfterEach
    public void tearDown() {
        scriptFileWatcher.deactivate();
    }

    protected Path getFile(String name) {
        Path tempFile = tempScriptDir.resolve(name);
        try {
            File parent = tempFile.getParent().toFile();

            if (!parent.exists() && !parent.mkdirs()) {
                fail("Failed to create parent directories");
            }

            if (!tempFile.toFile().createNewFile()) {
                fail("Failed to create temp script file");
            }
        } catch (IOException e) {
            throw new AssertionFailedError("Failed to create temp script file: " + e.getMessage());
        }
        return Path.of(tempFile.toUri());
    }

    void updateStartLevel(int level) {
        scriptFileWatcher
                .onReadyMarkerAdded(new ReadyMarker(StartLevelService.STARTLEVEL_MARKER_TYPE, Integer.toString(level)));
    }

    @Test
    public void testLoadOneDefaultFileAlreadyStarted() {
        when(scriptEngineManager.isSupported("js")).thenReturn(true);
        ScriptEngineContainer scriptEngineContainer = mock(ScriptEngineContainer.class);
        when(scriptEngineContainer.getScriptEngine()).thenReturn(mock(ScriptEngine.class));
        when(scriptEngineManager.createScriptEngine(anyString(), anyString())).thenReturn(scriptEngineContainer);

        updateStartLevel(100);

        Path p = getFile("script.js");

        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p);

        verify(scriptEngineManager).createScriptEngine("js", p.toFile().toURI().toString());
    }

    @Test
    public void testLoadOneDefaultFileWaitUntilStarted() {
        when(scriptEngineManager.isSupported("js")).thenReturn(true);
        ScriptEngineContainer scriptEngineContainer = mock(ScriptEngineContainer.class);
        when(scriptEngineContainer.getScriptEngine()).thenReturn(mock(ScriptEngine.class));
        when(scriptEngineManager.createScriptEngine(anyString(), anyString())).thenReturn(scriptEngineContainer);

        Path p = getFile("script.js");
        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p);

        // verify not yet called
        verify(scriptEngineManager, never()).createScriptEngine(anyString(), anyString());

        // verify is called when the start level increases
        updateStartLevel(100);
        verify(scriptEngineManager, timeout(1000).times(1)).createScriptEngine("js", p.toFile().toURI().toString());
    }

    @Test
    public void testLoadOneCustomFileWaitUntilStarted() {
        when(scriptEngineManager.isSupported("js")).thenReturn(true);
        ScriptEngineContainer scriptEngineContainer = mock(ScriptEngineContainer.class);
        when(scriptEngineContainer.getScriptEngine()).thenReturn(mock(ScriptEngine.class));
        when(scriptEngineManager.createScriptEngine(anyString(), anyString())).thenReturn(scriptEngineContainer);

        updateStartLevel(50);

        Path p = getFile("script.sl60.js");
        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p);

        // verify not yet called
        verify(scriptEngineManager, never()).createScriptEngine(anyString(), anyString());

        // verify is called when the start level increases
        updateStartLevel(100);
        verify(scriptEngineManager, timeout(1000).times(1)).createScriptEngine("js", p.toFile().toURI().toString());
    }

    @Test
    public void testLoadTwoCustomFilesDifferentStartLevels() {
        when(scriptEngineManager.isSupported("js")).thenReturn(true);
        ScriptEngineContainer scriptEngineContainer = mock(ScriptEngineContainer.class);
        when(scriptEngineContainer.getScriptEngine()).thenReturn(mock(ScriptEngine.class));
        when(scriptEngineManager.createScriptEngine(anyString(), anyString())).thenReturn(scriptEngineContainer);

        Path p1 = getFile("script.sl70.js");
        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p1);
        Path p2 = getFile("script.sl50.js");
        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p2);

        // verify not yet called
        verify(scriptEngineManager, never()).createScriptEngine(anyString(), anyString());

        updateStartLevel(40);

        // verify not yet called
        verify(scriptEngineManager, never()).createScriptEngine(anyString(), anyString());

        updateStartLevel(60);

        verify(scriptEngineManager, timeout(1000).times(1)).createScriptEngine("js", p2.toFile().toURI().toString());
        verify(scriptEngineManager, never()).createScriptEngine(anyString(), eq(p1.toFile().toURI().toString()));

        updateStartLevel(80);

        verify(scriptEngineManager, timeout(1000).times(1)).createScriptEngine("js", p1.toFile().toURI().toString());
    }

    @Test
    public void testLoadTwoCustomFilesAlternativePatternDifferentStartLevels() {
        when(scriptEngineManager.isSupported("js")).thenReturn(true);
        ScriptEngineContainer scriptEngineContainer = mock(ScriptEngineContainer.class);
        when(scriptEngineContainer.getScriptEngine()).thenReturn(mock(ScriptEngine.class));
        when(scriptEngineManager.createScriptEngine(anyString(), anyString())).thenReturn(scriptEngineContainer);

        Path p1 = getFile("sl70/script.js");
        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p1);
        Path p2 = getFile("sl50/script.js");
        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p2);

        // verify not yet called
        verify(scriptEngineManager, never()).createScriptEngine(anyString(), anyString());

        updateStartLevel(40);

        // verify not yet called
        verify(scriptEngineManager, never()).createScriptEngine(anyString(), anyString());

        updateStartLevel(60);

        verify(scriptEngineManager, timeout(1000).times(1)).createScriptEngine("js", p2.toFile().toURI().toString());
        verify(scriptEngineManager, never()).createScriptEngine(anyString(), eq(p1.toFile().toURI().toString()));

        updateStartLevel(80);

        verify(scriptEngineManager, timeout(1000).times(1)).createScriptEngine("js", p1.toFile().toURI().toString());
    }

    @Test
    public void testLoadOneDefaultFileDelayedSupport() {

        // set an executor which captures the scheduled task
        ScheduledExecutorService scheduledExecutorService = spy(
                new DelegatingScheduledExecutorService(Executors.newSingleThreadScheduledExecutor()));
        ArgumentCaptor<Runnable> scheduledTask = ArgumentCaptor.forClass(Runnable.class);
        scriptFileWatcher.setExecuterFactory(() -> scheduledExecutorService);

        when(scriptEngineManager.isSupported("js")).thenReturn(false);
        ScriptEngineContainer scriptEngineContainer = mock(ScriptEngineContainer.class);
        when(scriptEngineContainer.getScriptEngine()).thenReturn(mock(ScriptEngine.class));
        when(scriptEngineManager.createScriptEngine(anyString(), anyString())).thenReturn(scriptEngineContainer);
        updateStartLevel(100);

        verify(scheduledExecutorService).scheduleWithFixedDelay(scheduledTask.capture(), anyLong(), anyLong(), any());

        Path p = getFile("script.js");
        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p);

        // verify not yet called
        verify(scriptEngineManager, never()).createScriptEngine(anyString(), anyString());

        // add support is added for .js files
        when(scriptEngineManager.isSupported("js")).thenReturn(true);
        // update (in current thread)
        scheduledTask.getValue().run();

        // verify script has now been processed
        verify(scriptEngineManager, times(1)).createScriptEngine("js", p.toFile().toURI().toString());
    }

    @Test
    public void testOrderingWithinSingleStartLevel() {
        when(scriptEngineManager.isSupported("js")).thenReturn(true);
        ScriptEngineContainer scriptEngineContainer = mock(ScriptEngineContainer.class);
        when(scriptEngineContainer.getScriptEngine()).thenReturn(mock(ScriptEngine.class));

        when(scriptEngineManager.createScriptEngine(anyString(), anyString())).thenReturn(scriptEngineContainer);

        Path p64 = getFile("script.sl64.js");
        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p64);
        Path p66 = getFile("script.sl66.js");
        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p66);
        Path p65 = getFile("script.sl65.js");
        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p65);

        updateStartLevel(70);

        InOrder inOrder = inOrder(scriptEngineManager);

        inOrder.verify(scriptEngineManager, timeout(1000).times(1)).createScriptEngine("js",
                p64.toFile().toURI().toString());
        inOrder.verify(scriptEngineManager, timeout(1000).times(1)).createScriptEngine("js",
                p65.toFile().toURI().toString());
        inOrder.verify(scriptEngineManager, timeout(1000).times(1)).createScriptEngine("js",
                p66.toFile().toURI().toString());
    }

    @Test
    public void testReloadActiveWhenDependencyChanged() {
        when(scriptEngineManager.isSupported("js")).thenReturn(true);
        ScriptEngineContainer scriptEngineContainer = mock(ScriptEngineContainer.class);
        when(scriptEngineContainer.getScriptEngine()).thenReturn(mock(ScriptEngine.class));
        when(scriptEngineManager.createScriptEngine(anyString(), anyString())).thenReturn(scriptEngineContainer);

        updateStartLevel(100);

        Path p = getFile("script.js");

        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p);

        scriptFileWatcher.onDependencyChange(p.toFile().toURI().toString());

        verify(scriptEngineManager, times(2)).createScriptEngine("js", p.toFile().toURI().toString());
    }

    @Test
    public void testNotReloadInactiveWhenDependencyChanged() {
        when(scriptEngineManager.isSupported("js")).thenReturn(true);
        ScriptEngineContainer scriptEngineContainer = mock(ScriptEngineContainer.class);
        when(scriptEngineContainer.getScriptEngine()).thenReturn(mock(ScriptEngine.class));
        when(scriptEngineManager.createScriptEngine(anyString(), anyString())).thenReturn(scriptEngineContainer);

        Path p = getFile("script.js");

        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p);

        scriptFileWatcher.onDependencyChange(p.toFile().toURI().toString());

        verify(scriptEngineManager, never()).createScriptEngine("js", p.toFile().toURI().toString());
    }

    @Test
    public void testRegisterDependency() {
        when(scriptEngineManager.isSupported("js")).thenReturn(true);
        ScriptEngineContainer scriptEngineContainer = mock(ScriptEngineContainer.class);
        when(scriptEngineContainer.getScriptEngine()).thenReturn(mock(ScriptEngine.class));
        when(scriptEngineManager.createScriptEngine(anyString(), anyString())).thenReturn(scriptEngineContainer);

        updateStartLevel(100);

        Path p = getFile("script.js");

        when(scriptEngineContainer.getIdentifier()).thenReturn(p.toFile().toURI().toString());

        // capture and run the listener
        ArgumentCaptor<ScriptDependencyListener> listener = ArgumentCaptor.forClass(ScriptDependencyListener.class);

        scriptFileWatcher.processWatchEvent(null, ENTRY_CREATE, p);

        verify(scriptEngineManager).loadScript(eq(p.toFile().toURI().toString()), any(), listener.capture());

        listener.getValue().accept("test");

        // verify the dependency was tracked
        verify(dependencyTracker).addLibForScript(p.toFile().toURI().toString(), "test");
    }
}
