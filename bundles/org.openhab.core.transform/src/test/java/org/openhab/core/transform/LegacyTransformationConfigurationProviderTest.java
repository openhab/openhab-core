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
package org.openhab.core.transform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.registry.ProviderChangeListener;

/**
 * The {@link LegacyTransformationConfigurationProviderTest} includes tests for the
 * {@link LegacyTransformationConfigurationProvider}
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@NonNullByDefault
public class LegacyTransformationConfigurationProviderTest {
    private static final String FOO_TYPE = "foo";
    private static final String INITIAL_CONTENT = "initial";
    private static final String INITIAL_FILENAME = INITIAL_CONTENT + "." + FOO_TYPE;
    private static final TransformationConfiguration INITIAL_CONFIGURATION = new TransformationConfiguration(
            INITIAL_FILENAME, INITIAL_FILENAME, FOO_TYPE, null, INITIAL_CONTENT);
    private static final String ADDED_CONTENT = "added";
    private static final String ADDED_FILENAME = ADDED_CONTENT + "." + FOO_TYPE;

    @Mock
    private @NonNullByDefault({}) WatchEvent<String> watchEvent;

    @Mock
    private @NonNullByDefault({}) ProviderChangeListener<@NonNull TransformationConfiguration> listener;

    private @NonNullByDefault({}) LegacyTransformationConfigurationProvider provider;

    private @NonNullByDefault({}) Path targetPath;

    @BeforeEach
    public void setup() throws IOException {
        // create directory
        targetPath = Files.createTempDirectory("legacyTest");
        // set initial content
        Files.write(targetPath.resolve(INITIAL_FILENAME), INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8));

        provider = new LegacyTransformationConfigurationProvider(targetPath);
        provider.addProviderChangeListener(listener);
    }

    @Test
    public void testInitialConfigurationIsPresent() {
        // assert that initial configuration is present
        assertTrue(provider.getAll().contains(INITIAL_CONFIGURATION));
    }

    @Test
    public void testAddingConfigurationIsPropagated() throws IOException {
        Path path = targetPath.resolve(ADDED_FILENAME);

        Files.write(path, ADDED_CONTENT.getBytes(StandardCharsets.UTF_8));
        TransformationConfiguration addedConfiguration = new TransformationConfiguration(ADDED_FILENAME, ADDED_FILENAME,
                FOO_TYPE, null, ADDED_CONTENT);

        provider.processWatchEvent(watchEvent, StandardWatchEventKinds.ENTRY_CREATE, path);

        // assert registry is notified and internal cache updated
        Mockito.verify(listener).added(provider, addedConfiguration);
        assertTrue(provider.getAll().contains(addedConfiguration));
    }

    @Test
    public void testUpdatingConfigurationIsPropagated() throws IOException {
        Path path = targetPath.resolve(INITIAL_FILENAME);
        Files.write(path, "updated".getBytes(StandardCharsets.UTF_8));
        TransformationConfiguration updatedConfiguration = new TransformationConfiguration(INITIAL_FILENAME,
                INITIAL_FILENAME, FOO_TYPE, null, "updated");

        provider.processWatchEvent(watchEvent, StandardWatchEventKinds.ENTRY_MODIFY, path);

        Mockito.verify(listener).updated(provider, INITIAL_CONFIGURATION, updatedConfiguration);
        assertTrue(provider.getAll().contains(updatedConfiguration));
        assertFalse(provider.getAll().contains(INITIAL_CONFIGURATION));
    }

    @Test
    public void testDeletingConfigurationIsPropagated() {
        Path path = targetPath.resolve(INITIAL_FILENAME);

        provider.processWatchEvent(watchEvent, StandardWatchEventKinds.ENTRY_DELETE, path);

        Mockito.verify(listener).removed(provider, INITIAL_CONFIGURATION);
        assertFalse(provider.getAll().contains(INITIAL_CONFIGURATION));
    }

    @Test
    public void testLanguageIsProperlyParsed() throws IOException {
        String fileName = "test_de." + FOO_TYPE;
        Path path = targetPath.resolve(fileName);

        Files.write(path, INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8));

        TransformationConfiguration expected = new TransformationConfiguration(fileName, fileName, FOO_TYPE, "de",
                INITIAL_CONTENT);

        provider.processWatchEvent(watchEvent, StandardWatchEventKinds.ENTRY_CREATE, path);
        assertTrue(provider.getAll().contains(expected));
    }

    @Test
    public void testMissingExtensionIsIgnored() throws IOException {
        Path path = targetPath.resolve("extensionMissing");
        Files.write(path, INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8));
        provider.processWatchEvent(watchEvent, StandardWatchEventKinds.ENTRY_CREATE, path);
        provider.processWatchEvent(watchEvent, StandardWatchEventKinds.ENTRY_MODIFY, path);

        Mockito.verify(listener, never()).added(any(), any());
        Mockito.verify(listener, never()).updated(any(), any(), any());
    }

    @Test
    public void testIgnoredExtensionIsIgnored() throws IOException {
        Path path = targetPath.resolve("extensionIgnore.txt");
        Files.write(path, INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8));
        provider.processWatchEvent(watchEvent, StandardWatchEventKinds.ENTRY_CREATE, path);
        provider.processWatchEvent(watchEvent, StandardWatchEventKinds.ENTRY_MODIFY, path);

        Mockito.verify(listener, never()).added(any(), any());
        Mockito.verify(listener, never()).updated(any(), any(), any());
    }

    @AfterEach
    public void tearDown() throws IOException {
        try (Stream<Path> walk = Files.walk(targetPath)) {
            walk.map(Path::toFile).forEach(File::delete);
        }
        Files.deleteIfExists(targetPath);
    }
}
