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
package org.openhab.core.transform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.openhab.core.transform.Transformation.FUNCTION;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.service.WatchService;

/**
 * The {@link FileTransformationProviderTest} includes tests for the
 * {@link FileTransformationProvider}
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class FileTransformationProviderTest {
    private static final String FOO_TYPE = "foo";
    private static final String INITIAL_CONTENT = "initial";
    private static final String INITIAL_FILENAME = INITIAL_CONTENT + "." + FOO_TYPE;
    private static final Transformation INITIAL_CONFIGURATION = new Transformation(INITIAL_FILENAME, INITIAL_FILENAME,
            FOO_TYPE, Map.of(FUNCTION, INITIAL_CONTENT));
    private static final String ADDED_CONTENT = "added";
    private static final String ADDED_FILENAME = ADDED_CONTENT + "." + FOO_TYPE;

    private @Mock @NonNullByDefault({}) WatchService watchService;
    private @Mock @NonNullByDefault({}) ProviderChangeListener<@NonNull Transformation> listenerMock;

    private @NonNullByDefault({}) FileTransformationProvider provider;
    private @NonNullByDefault({}) @TempDir Path targetPath;

    @BeforeEach
    public void setup() throws IOException {
        // set initial content
        Files.write(targetPath.resolve(INITIAL_FILENAME), INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8));

        provider = new FileTransformationProvider(watchService, targetPath);
        provider.addProviderChangeListener(listenerMock);
    }

    @Test
    public void testInitialConfigurationIsPresent() {
        // assert that initial configuration is present
        assertThat(provider.getAll(), contains(INITIAL_CONFIGURATION));
    }

    @Test
    public void testAddingConfigurationIsPropagated() throws IOException {
        Path path = targetPath.resolve(ADDED_FILENAME);

        Files.write(path, ADDED_CONTENT.getBytes(StandardCharsets.UTF_8));
        Transformation addedConfiguration = new Transformation(ADDED_FILENAME, ADDED_FILENAME, FOO_TYPE,
                Map.of(FUNCTION, ADDED_CONTENT));

        provider.processWatchEvent(WatchService.Kind.CREATE, targetPath.relativize(path));

        // assert registry is notified and internal cache updated
        Mockito.verify(listenerMock).added(provider, addedConfiguration);
        assertThat(provider.getAll(), hasItem(addedConfiguration));
    }

    @Test
    public void testUpdatingConfigurationIsPropagated() throws IOException {
        Path path = targetPath.resolve(INITIAL_FILENAME);
        Files.write(path, "updated".getBytes(StandardCharsets.UTF_8));
        Transformation updatedConfiguration = new Transformation(INITIAL_FILENAME, INITIAL_FILENAME, FOO_TYPE,
                Map.of(FUNCTION, "updated"));

        provider.processWatchEvent(WatchService.Kind.MODIFY, targetPath.relativize(path));

        Mockito.verify(listenerMock).updated(provider, INITIAL_CONFIGURATION, updatedConfiguration);
        assertThat(provider.getAll(), contains(updatedConfiguration));
        assertThat(provider.getAll(), not(contains(INITIAL_CONFIGURATION)));
    }

    @Test
    public void testDeletingConfigurationIsPropagated() {
        Path path = targetPath.resolve(INITIAL_FILENAME);

        provider.processWatchEvent(WatchService.Kind.DELETE, targetPath.relativize(path));

        Mockito.verify(listenerMock).removed(provider, INITIAL_CONFIGURATION);
        assertThat(provider.getAll(), not(contains(INITIAL_CONFIGURATION)));
    }

    @Test
    public void testLanguageIsProperlyParsed() throws IOException {
        String fileName = "test_de." + FOO_TYPE;
        Path path = targetPath.resolve(fileName);

        Files.write(path, INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8));

        Transformation expected = new Transformation(fileName, fileName, FOO_TYPE, Map.of(FUNCTION, INITIAL_CONTENT));

        provider.processWatchEvent(WatchService.Kind.CREATE, targetPath.relativize(path));
        assertThat(provider.getAll(), hasItem(expected));
    }

    @Test
    public void testMissingExtensionIsIgnored() throws IOException {
        Path path = targetPath.resolve("extensionMissing");
        Files.write(path, INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8));
        provider.processWatchEvent(WatchService.Kind.CREATE, targetPath.relativize(path));
        provider.processWatchEvent(WatchService.Kind.MODIFY, targetPath.relativize(path));

        Mockito.verify(listenerMock, never()).added(any(), any());
        Mockito.verify(listenerMock, never()).updated(any(), any(), any());
    }

    @Test
    public void testIgnoredExtensionIsIgnored() throws IOException {
        Path path = targetPath.resolve("extensionIgnore.txt");
        Files.write(path, INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8));
        provider.processWatchEvent(WatchService.Kind.CREATE, targetPath.relativize(path));
        provider.processWatchEvent(WatchService.Kind.MODIFY, targetPath.relativize(path));

        Mockito.verify(listenerMock, never()).added(any(), any());
        Mockito.verify(listenerMock, never()).updated(any(), any(), any());
    }
}
