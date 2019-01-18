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
package org.eclipse.smarthome.config.dispatch.test;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;

import org.eclipse.smarthome.config.dispatch.internal.ConfigDispatcher;
import org.eclipse.smarthome.config.dispatch.internal.ConfigDispatcherFileWatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ConfigDispatcherFileWatcherTest {

    private TestConfigDispatcherFileWatcher configDispatcherFileWatcher;

    @Mock
    ConfigDispatcher configDispatcher;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        configDispatcherFileWatcher = new TestConfigDispatcherFileWatcher();
        configDispatcherFileWatcher.setConfigDispatcher(configDispatcher);
    }

    @Test
    public void configurationFileCreated() {
        String path = "myPath.cfg";
        configDispatcherFileWatcher.processWatchEvent(null, StandardWatchEventKinds.ENTRY_CREATE,
                new File(path).toPath());

        verify(configDispatcher).processConfigFile(new File(path));
    }

    @Test
    public void configurationFileModified() {
        String path = "myPath.cfg";
        configDispatcherFileWatcher.processWatchEvent(null, StandardWatchEventKinds.ENTRY_MODIFY,
                new File(path).toPath());

        verify(configDispatcher).processConfigFile(new File(path));
    }

    @Test
    public void nonConfigurationFileCreated() {
        String path = "myPath";
        configDispatcherFileWatcher.processWatchEvent(null, StandardWatchEventKinds.ENTRY_CREATE,
                new File(path).toPath());

        verifyZeroInteractions(configDispatcher);
    }

    @Test
    public void nonConfigurationFileModified() {
        String path = "myPath";
        configDispatcherFileWatcher.processWatchEvent(null, StandardWatchEventKinds.ENTRY_MODIFY,
                new File(path).toPath());

        verifyZeroInteractions(configDispatcher);
    }

    @Test
    public void configurationFileRemoved() {
        String path = "myPath.cfg";
        configDispatcherFileWatcher.processWatchEvent(null, StandardWatchEventKinds.ENTRY_DELETE,
                new File(path).toPath());

        verify(configDispatcher).fileRemoved(new File(path).getAbsolutePath());
    }

    @Test
    public void nonConfigurationFileRemoved() {
        String path = "myPath";
        configDispatcherFileWatcher.processWatchEvent(null, StandardWatchEventKinds.ENTRY_DELETE,
                new File(path).toPath());

        verifyZeroInteractions(configDispatcher);
    }

    public class TestConfigDispatcherFileWatcher extends ConfigDispatcherFileWatcher {

        @Override
        protected void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
            super.processWatchEvent(event, kind, path);
        }
    }

}
