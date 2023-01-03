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
package org.openhab.core.storage.json.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.OpenHAB;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class JsonStorageServiceOSGiTest extends JavaOSGiTest {

    private static final String KEY1 = "Key1";
    private static final String KEY2 = "Key2";

    private @NonNullByDefault({}) StorageService storageService;
    private @NonNullByDefault({}) Storage<PersistedItem> storage;

    @BeforeEach
    public void setUp() {
        storageService = getService(StorageService.class);
        storage = storageService.getStorage(UUID.randomUUID().toString(), this.getClass().getClassLoader());
    }

    @AfterEach
    public void tearDown() throws IOException {
        unregisterService(storageService);
    }

    @AfterAll
    public static void afterClass() throws IOException {
        // clean up database files ...
        final Path userData = Paths.get(OpenHAB.getUserDataFolder());
        if (Files.exists(userData)) {
            try (Stream<Path> walk = Files.walk(userData)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
        final Path config = Paths.get(OpenHAB.getConfigFolder());
        if (Files.exists(config)) {
            try (Stream<Path> walk = Files.walk(config)) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @Test
    public void testOnlyAlphanumericCharsInFileName() {
        JsonStorageService st = (JsonStorageService) storageService;

        String escaped = st.urlEscapeUnwantedChars("Strange:File-Name~with#Chars");
        assertEquals("Strange%3AFile-Name%7Ewith%23Chars", escaped);

        // test cut after 127 chars
        escaped = st.urlEscapeUnwantedChars(
                "AveryLongFileNameThatNeverEndsAveryLongFileNameThatNeverEndsAveryLongFileNameThatNeverEndsAveryLongFileNameThatNeverEndsAveryLongFileNameThatNeverEnds.json");
        assertEquals(
                "AveryLongFileNameThatNeverEndsAveryLongFileNameThatNeverEndsAveryLongFileNameThatNeverEndsAveryLongFileNameThatNeverEndsAveryLo",
                escaped);

        // test with valid file name
        escaped = st.urlEscapeUnwantedChars("Allowed.File.Name123");
        assertEquals("Allowed.File.Name123", escaped);
    }

    @Test
    public void testSerialization() {
        assertThat(storage.getKeys().size(), is(0));

        storage.put(KEY1, new PersistedItem(CoreItemFactory.STRING, List.of("LIGHT", "GROUND_FLOOR")));
        storage.put(KEY2, new PersistedItem(CoreItemFactory.NUMBER, List.of("TEMPERATURE", "OUTSIDE")));
        assertThat(storage.getKeys().size(), is(2));

        storage.remove(KEY1);
        storage.remove(KEY2);
        assertThat(storage.getKeys().size(), is(0));
    }

    @Test
    public void testOverride() {
        PersistedItem pItem = null;

        assertThat(storage.getKeys().size(), is(0));

        pItem = storage.put(KEY1, new PersistedItem(CoreItemFactory.STRING, List.of("LIGHT", "GROUND_FLOOR")));
        assertThat(storage.getKeys().size(), is(1));
        assertThat(pItem, is(nullValue()));

        pItem = storage.get(KEY1);
        assertThat(pItem, is(notNullValue()));
        assertThat(pItem.itemType, is(CoreItemFactory.STRING));

        pItem = storage.put(KEY1, new PersistedItem(CoreItemFactory.NUMBER, List.of("TEMPERATURE")));
        assertThat(pItem, is(notNullValue()));
        assertThat(storage.getKeys().size(), is(1));
        assertThat(pItem.itemType, is(CoreItemFactory.STRING));
        pItem = storage.get(KEY1);
        assertThat(pItem, is(notNullValue()));
        assertThat(pItem.itemType, is(CoreItemFactory.NUMBER));

        storage.remove(KEY1);
        assertThat(storage.getKeys().size(), is(0));
    }

    @Test
    public void testClassloader() {
        Storage<String> storageWithoutClassloader = storageService.getStorage("storageWithoutClassloader");
        storageWithoutClassloader.put(KEY1, "Value");

        assertThat(storageWithoutClassloader.get(KEY1), is(equalTo("Value")));
    }

    @Test
    public void testConfiguration() {
        Storage<DummyObject> storageWithoutClassloader = storageService.getStorage("storage");

        DummyObject dummy = new DummyObject();
        dummy.configuration.put("bigDecimal", new BigDecimal(3));

        storageWithoutClassloader.put("configuration", dummy);

        @SuppressWarnings("null")
        Object bigDecimal = storageWithoutClassloader.get("configuration").configuration.get("bigDecimal");
        assertThat(bigDecimal instanceof BigDecimal, is(true));
    }

    public static class DummyObject {
        private final Configuration configuration = new Configuration();
    }

    public static class PersistedItem {

        public @Nullable String itemType;
        public @Nullable List<String> groupNames;
        public @Nullable String baseItemType;

        /**
         * Package protected default constructor to allow reflective instantiation.
         *
         * !!! DO NOT REMOVE - Gson needs it !!!
         */
        PersistedItem() {
        }

        public PersistedItem(String itemType, List<String> groupNames) {
            this(itemType, groupNames, null);
        }

        public PersistedItem(String itemType, List<String> groupNames, @Nullable String baseItemType) {
            this.itemType = itemType;
            this.groupNames = groupNames;
            this.baseItemType = baseItemType;
        }
    }
}
