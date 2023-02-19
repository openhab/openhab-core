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
package org.openhab.core.config.dispatch.internal;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.service.WatchService;

/**
 * @author Stefan Triller - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class ConfigDispatcherFileWatcherTest {

    private @NonNullByDefault({}) ConfigDispatcherFileWatcher configDispatcherFileWatcher;

    private @Mock @NonNullByDefault({}) ConfigDispatcher configDispatcherMock;
    private @Mock @NonNullByDefault({}) WatchService watchService;

    private @TempDir @NonNullByDefault({}) Path tempDir;

    private @NonNullByDefault({}) Path cfgPath;
    private @NonNullByDefault({}) Path nonCfgPath;

    @BeforeEach
    public void setUp() throws IOException {
        configDispatcherFileWatcher = new ConfigDispatcherFileWatcher(configDispatcherMock, watchService);
        verify(configDispatcherMock).processConfigFile(any());

        when(watchService.getWatchPath()).thenReturn(tempDir.toAbsolutePath());

        cfgPath = tempDir.resolve("myPath.cfg");
        nonCfgPath = tempDir.resolve("myPath");

        Files.createFile(cfgPath);
        Files.createFile(nonCfgPath);
    }

    @Test
    public void configurationFileCreated() throws IOException {
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.CREATE, cfgPath);
        verify(configDispatcherMock).processConfigFile(cfgPath.toAbsolutePath().toFile());
    }

    @Test
    public void configurationFileModified() throws IOException {
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.MODIFY, cfgPath);
        verify(configDispatcherMock).processConfigFile(cfgPath.toAbsolutePath().toFile());
    }

    @Test
    public void nonConfigurationFileCreated() {
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.CREATE, nonCfgPath);
        verifyNoMoreInteractions(configDispatcherMock);
    }

    @Test
    public void nonConfigurationFileModified() {
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.MODIFY, nonCfgPath);
        verifyNoMoreInteractions(configDispatcherMock);
    }

    @Test
    public void configurationFileRemoved() {
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.DELETE, cfgPath);
        verify(configDispatcherMock).fileRemoved(cfgPath.toAbsolutePath().toString());
    }

    @Test
    public void nonConfigurationFileRemoved() {
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.DELETE, nonCfgPath);
        verifyNoMoreInteractions(configDispatcherMock);
    }
}
