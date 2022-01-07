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
package org.openhab.core.config.dispatch.internal;

import static org.mockito.Mockito.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Stefan Triller - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
public class ConfigDispatcherFileWatcherTest {

    private TestConfigDispatcherFileWatcher configDispatcherFileWatcher;

    private @Mock ConfigDispatcher configDispatcher;

    @BeforeEach
    public void setUp() throws Exception {
        configDispatcherFileWatcher = new TestConfigDispatcherFileWatcher(configDispatcher);
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

        verifyNoInteractions(configDispatcher);
    }

    @Test
    public void nonConfigurationFileModified() {
        String path = "myPath";
        configDispatcherFileWatcher.processWatchEvent(null, StandardWatchEventKinds.ENTRY_MODIFY,
                new File(path).toPath());

        verifyNoInteractions(configDispatcher);
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

        verifyNoInteractions(configDispatcher);
    }

    public class TestConfigDispatcherFileWatcher extends ConfigDispatcherFileWatcher {
        public TestConfigDispatcherFileWatcher(ConfigDispatcher configDispatcher) {
            super(configDispatcher);
        }

        @Override
        protected void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
            super.processWatchEvent(event, kind, path);
        }
    }
}
