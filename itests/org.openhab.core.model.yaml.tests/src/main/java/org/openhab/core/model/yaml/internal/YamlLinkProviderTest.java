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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.test.CountingRegistryListener;
import org.openhab.core.service.WatchService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.ItemChannelLink;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;

/**
 * The {@link YamlLinkProviderTest} contains tests for the
 * {@link org.openhab.core.model.yaml.internal.link.YamlLinkProvider}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class YamlLinkProviderTest extends JavaOSGiTest {

    private static final Path MODEL_NAME = Path.of("model-links.yaml");
    private static final Path INITIAL_MODEL = Path.of("links-initial.yaml");
    private static final Path MODIFIED_MODEL = Path.of("links-modified.yaml");

    private @NonNullByDefault({}) Path confPath;
    private @NonNullByDefault({}) WatchService watchService;
    private @NonNullByDefault({}) YamlModelRepository modelRepository;
    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;
    private @NonNullByDefault({}) CountingRegistryListener<ItemChannelLink> listener;

    @BeforeEach
    public void beforeEach() throws IOException {
        registerVolatileStorageService();

        watchService = getService(WatchService.class);
        assertThat(watchService, is(notNullValue()));
        confPath = watchService.getWatchPath().resolve("yaml");
        Files.createDirectories(confPath);
        modelRepository = getService(YamlModelRepository.class);
        assertThat(modelRepository, is(notNullValue()));
        itemChannelLinkRegistry = getService(ItemChannelLinkRegistry.class);
        assertThat(itemChannelLinkRegistry, is(notNullValue()));

        listener = new CountingRegistryListener<>();
        itemChannelLinkRegistry.addRegistryChangeListener(listener);
    }

    @Test
    void testLinksProcessing() throws IOException {
        Files.copy(bundleContext.getBundle().getResource(INITIAL_MODEL.toString()).openStream(),
                confPath.resolve(MODEL_NAME));

        waitForAssert(() -> {
            assertThat(listener.getAddedCounter(), is(2));
            assertThat(listener.getRemovedCounter(), is(0));
            assertThat(listener.getUpdatedCounter(), is(0));
        });

        // first entry
        ItemChannelLink link1 = itemChannelLinkRegistry
                .get("TestItem1 -> testBinding:testThingType:testThing:testChannel1");
        assertThat(link1, is(notNullValue()));
        assertThat(link1.getLinkedUID(), is(new ChannelUID("testBinding:testThingType:testThing:testChannel1")));
        assertThat(link1.getItemName(), is("TestItem1"));
        assertThat(link1.getConfiguration().getProperties(), is(anEmptyMap()));

        // second entry
        ItemChannelLink link2 = itemChannelLinkRegistry
                .get("TestItem2 -> testBinding:testThingType:testThing:testChannel2");
        assertThat(link2, is(notNullValue()));
        assertThat(link2.getLinkedUID(), is(new ChannelUID("testBinding:testThingType:testThing:testChannel2")));
        assertThat(link2.getItemName(), is("TestItem2"));
        Configuration config = link2.getConfiguration();
        assertThat(config.get("profile"), is("system:offset"));
        assertThat(config.get("offset"), is("5 s"));

        Files.copy(bundleContext.getBundle().getResource(MODIFIED_MODEL.toString()).openStream(),
                confPath.resolve(MODEL_NAME), StandardCopyOption.REPLACE_EXISTING);

        waitForAssert(() -> {
            assertThat(listener.getAddedCounter(), is(3));
            assertThat(listener.getRemovedCounter(), is(1));
            assertThat(listener.getUpdatedCounter(), is(1));
        });

        // first entry
        link1 = itemChannelLinkRegistry.get("TestItem1 -> testBinding:testThingType:testThing:testChannel1");
        assertThat(link1, is(notNullValue()));
        assertThat(link1.getLinkedUID(), is(new ChannelUID("testBinding:testThingType:testThing:testChannel1")));
        assertThat(link1.getItemName(), is("TestItem1"));
        config = link1.getConfiguration();
        assertThat(config.get("profile"), is("system:offset"));
        assertThat(config.get("offset"), is("1 min"));

        // removed second entry
        link2 = itemChannelLinkRegistry.get("TestItem2 -> testBinding:testThingType:testThing:testChannel2");
        assertThat(link2, is(nullValue()));

        // added third entry
        ItemChannelLink link3 = itemChannelLinkRegistry
                .get("TestItem3 -> testBinding:testThingType:testThing:testChannel3");
        assertThat(link3, is(notNullValue()));
        assertThat(link3.getLinkedUID(), is(new ChannelUID("testBinding:testThingType:testThing:testChannel3")));
        assertThat(link3.getItemName(), is("TestItem3"));

        // remove model
        Files.delete(confPath.resolve(MODEL_NAME));
        waitForAssert(() -> assertThat(itemChannelLinkRegistry.getAll(), is(empty())));
    }
}
