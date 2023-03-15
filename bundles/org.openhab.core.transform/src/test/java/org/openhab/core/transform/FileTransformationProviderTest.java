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
import static org.mockito.Mockito.when;
import static org.openhab.core.service.WatchService.Kind.CREATE;
import static org.openhab.core.service.WatchService.Kind.DELETE;
import static org.openhab.core.service.WatchService.Kind.MODIFY;
import static org.openhab.core.transform.Transformation.FUNCTION;

import java.io.IOException;
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
    private static final Path INITIAL_FILENAME = Path.of(INITIAL_CONTENT + "." + FOO_TYPE);
    private static final Transformation INITIAL_CONFIGURATION = new Transformation(INITIAL_FILENAME.toString(),
            INITIAL_FILENAME.toString(), FOO_TYPE, Map.of(FUNCTION, INITIAL_CONTENT));
    private static final String ADDED_CONTENT = "added";
    private static final Path ADDED_FILENAME = Path.of(ADDED_CONTENT + "." + FOO_TYPE);

    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @Mock @NonNullByDefault({}) ProviderChangeListener<@NonNull Transformation> listenerMock;

    private @NonNullByDefault({}) FileTransformationProvider provider;
    private @NonNullByDefault({}) @TempDir Path configPath;
    private @NonNullByDefault({}) Path transformationPath;

    @BeforeEach
    public void setup() throws IOException {
        when(watchServiceMock.getWatchPath()).thenReturn(configPath);
        transformationPath = configPath.resolve(TransformationService.TRANSFORM_FOLDER_NAME);

        // create transformation directory and set initial content
        Files.createDirectories(transformationPath);
        Files.writeString(transformationPath.resolve(INITIAL_FILENAME), INITIAL_CONTENT);

        provider = new FileTransformationProvider(watchServiceMock);
        provider.addProviderChangeListener(listenerMock);
    }

    @Test
    public void testInitialConfigurationIsPresent() {
        // assert that initial configuration is present
        assertThat(provider.getAll(), contains(INITIAL_CONFIGURATION));
    }

    @Test
    public void testAddingConfigurationIsPropagated() throws IOException {
        Files.writeString(transformationPath.resolve(ADDED_FILENAME), ADDED_CONTENT);
        Transformation addedConfiguration = new Transformation(ADDED_FILENAME.toString(), ADDED_FILENAME.toString(),
                FOO_TYPE, Map.of(FUNCTION, ADDED_CONTENT));
        provider.processWatchEvent(CREATE, ADDED_FILENAME);

        // assert registry is notified and internal cache updated
        Mockito.verify(listenerMock).added(provider, addedConfiguration);
        assertThat(provider.getAll(), hasItem(addedConfiguration));
    }

    @Test
    public void testUpdatingConfigurationIsPropagated() throws IOException {
        Files.writeString(transformationPath.resolve(INITIAL_FILENAME), "updated");
        Transformation updatedConfiguration = new Transformation(INITIAL_FILENAME.toString(),
                INITIAL_FILENAME.toString(), FOO_TYPE, Map.of(FUNCTION, "updated"));
        provider.processWatchEvent(MODIFY, INITIAL_FILENAME);

        Mockito.verify(listenerMock).updated(provider, INITIAL_CONFIGURATION, updatedConfiguration);
        assertThat(provider.getAll(), contains(updatedConfiguration));
        assertThat(provider.getAll(), not(contains(INITIAL_CONFIGURATION)));
    }

    @Test
    public void testDeletingConfigurationIsPropagated() {
        provider.processWatchEvent(DELETE, INITIAL_FILENAME);

        Mockito.verify(listenerMock).removed(provider, INITIAL_CONFIGURATION);
        assertThat(provider.getAll(), not(contains(INITIAL_CONFIGURATION)));
    }

    @Test
    public void testLanguageIsProperlyParsed() throws IOException {
        String fileName = "test_de." + FOO_TYPE;
        Path path = transformationPath.resolve(fileName);

        Files.writeString(path, INITIAL_CONTENT);

        Transformation expected = new Transformation(fileName, fileName, FOO_TYPE, Map.of(FUNCTION, INITIAL_CONTENT));

        provider.processWatchEvent(CREATE, Path.of(fileName));
        assertThat(provider.getAll(), hasItem(expected));
    }

    @Test
    public void testMissingExtensionIsIgnored() throws IOException {
        Path extensionMissing = Path.of("extensionMissing");
        Path path = transformationPath.resolve(extensionMissing);
        Files.writeString(path, INITIAL_CONTENT);
        provider.processWatchEvent(CREATE, extensionMissing);
        provider.processWatchEvent(MODIFY, extensionMissing);

        Mockito.verify(listenerMock, never()).added(any(), any());
        Mockito.verify(listenerMock, never()).updated(any(), any(), any());
    }

    @Test
    public void testIgnoredExtensionIsIgnored() throws IOException {
        Path extensionIgnored = Path.of("extensionIgnore.txt");
        Path path = transformationPath.resolve(extensionIgnored);
        Files.writeString(path, INITIAL_CONTENT);
        provider.processWatchEvent(CREATE, extensionIgnored);
        provider.processWatchEvent(MODIFY, extensionIgnored);

        Mockito.verify(listenerMock, never()).added(any(), any());
        Mockito.verify(listenerMock, never()).updated(any(), any(), any());
    }
}
