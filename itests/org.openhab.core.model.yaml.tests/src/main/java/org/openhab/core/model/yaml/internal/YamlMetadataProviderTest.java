/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.test.CountingRegistryListener;
import org.openhab.core.service.WatchService;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * The {@link YamlMetadataProviderTest} contains tests for the
 * {@link org.openhab.core.model.yaml.internal.metadata.YamlMetadataProvider}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class YamlMetadataProviderTest extends JavaOSGiTest {

    private static final Path MODEL_NAME = Path.of("model-metadata.yaml");
    private static final Path INITIAL_MODEL = Path.of("metadata-initial.yaml");
    private static final Path MODIFIED_MODEL = Path.of("metadata-modified.yaml");

    private @NonNullByDefault({}) Path confPath;
    private @NonNullByDefault({}) WatchService watchService;
    private @NonNullByDefault({}) YamlModelRepository modelRepository;
    private @NonNullByDefault({}) MetadataRegistry metadataRegistry;
    private @NonNullByDefault({}) CountingRegistryListener<Metadata> listener;

    @BeforeEach
    public void beforeEach() throws IOException {
        watchService = getService(WatchService.class);
        assertThat(watchService, is(notNullValue()));
        confPath = watchService.getWatchPath().resolve("yaml");
        Files.createDirectories(confPath);
        modelRepository = getService(YamlModelRepository.class);
        assertThat(modelRepository, is(notNullValue()));
        metadataRegistry = getService(MetadataRegistry.class);
        assertThat(metadataRegistry, is(notNullValue()));

        listener = new CountingRegistryListener<>();
        metadataRegistry.addRegistryChangeListener(listener);
    }

    @Test
    void testMetadataProcessing() throws IOException {
        Files.copy(bundleContext.getBundle().getResource(INITIAL_MODEL.toString()).openStream(),
                confPath.resolve(MODEL_NAME));

        waitForAssert(() -> {
            assertThat(listener.getAddedCounter(), is(2));
            assertThat(listener.getRemovedCounter(), is(0));
            assertThat(listener.getUpdatedCounter(), is(0));
        });

        // first entry
        Metadata metadata1 = metadataRegistry.get(new MetadataKey("ns1", "testItem"));
        assertThat(metadata1, is(notNullValue()));
        assertThat(metadata1.getValue(), is("Foo"));
        Map<String, Object> config = metadata1.getConfiguration();
        assertThat(config, is(notNullValue()));
        assertThat(config.get("key"), is("value"));

        // second entry
        Metadata metadata2 = metadataRegistry.get(new MetadataKey("ns2", "testItem"));
        assertThat(metadata2, is(notNullValue()));
        assertThat(metadata2.getValue(), is("Bar"));
        assertThat(metadata2.getConfiguration(), is(anEmptyMap()));

        Files.copy(bundleContext.getBundle().getResource(MODIFIED_MODEL.toString()).openStream(),
                confPath.resolve(MODEL_NAME), StandardCopyOption.REPLACE_EXISTING);

        waitForAssert(() -> {
            assertThat(listener.getAddedCounter(), is(3));
            assertThat(listener.getRemovedCounter(), is(1));
            assertThat(listener.getUpdatedCounter(), is(1));
        });

        // modified first entry
        metadata1 = metadataRegistry.get(new MetadataKey("ns1", "testItem"));
        assertThat(metadata1, is(notNullValue()));
        assertThat(metadata1.getValue(), is("Foo"));
        config = metadata1.getConfiguration();
        assertThat(config, is(notNullValue()));
        assertThat(config.get("key"), is("newValue"));

        // deleted second entry
        metadata2 = metadataRegistry.get(new MetadataKey("ns2", "testItem"));
        assertThat(metadata2, is(nullValue()));

        // added third entry
        Metadata metadata3 = metadataRegistry.get(new MetadataKey("ns3", "testItem"));
        assertThat(metadata3, is(notNullValue()));
        assertThat(metadata3.getValue(), is("Baz"));
        assertThat(metadata3.getConfiguration(), is(anEmptyMap()));

        // remove model
        Files.delete(confPath.resolve(MODEL_NAME));
        waitForAssert(() -> assertThat(metadataRegistry.getAll(), is(empty())));
    }
}
