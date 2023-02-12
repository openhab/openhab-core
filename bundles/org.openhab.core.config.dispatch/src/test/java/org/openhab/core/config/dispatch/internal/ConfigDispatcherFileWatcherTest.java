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

import java.nio.file.Path;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @BeforeEach
    public void setUp() {
        configDispatcherFileWatcher = new ConfigDispatcherFileWatcher(configDispatcherMock, watchService);
        verify(configDispatcherMock).processConfigFile(any());
    }

    @Test
    public void configurationFileCreated() {
        Path path = Path.of("myPath.cfg");
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.CREATE, path);

        verify(configDispatcherMock).processConfigFile(path.toFile());
    }

    @Test
    public void configurationFileModified() {
        Path path = Path.of("myPath.cfg");
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.MODIFY, path);

        verify(configDispatcherMock).processConfigFile(path.toFile());
    }

    @Test
    public void nonConfigurationFileCreated() {
        Path path = Path.of("myPath");
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.CREATE, path);

        verifyNoMoreInteractions(configDispatcherMock);
    }

    @Test
    public void nonConfigurationFileModified() {
        Path path = Path.of("myPath");
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.MODIFY, path);

        verifyNoMoreInteractions(configDispatcherMock);
    }

    @Test
    public void configurationFileRemoved() {
        Path path = Path.of("myPath.cfg");
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.DELETE, path);

        verify(configDispatcherMock).fileRemoved(path.toAbsolutePath().toString());
    }

    @Test
    public void nonConfigurationFileRemoved() {
        Path path = Path.of("myPath");
        configDispatcherFileWatcher.processWatchEvent(WatchService.Kind.DELETE, path);

        verifyNoMoreInteractions(configDispatcherMock);
    }
}
