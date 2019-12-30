/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.storage.mapdb.internal;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.core.ConfigConstants;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.storage.DeletableStorage;
import org.openhab.core.storage.Storage;

/**
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Alex Tugarev - Added test for getStorage without classloader
 * @author Markus Rathgeb - Migrate Groovy tests to OSGi
 * @author Markus Rathgeb - Migrate OSGi test to non-OSGi test
 */
@NonNullByDefault
public class MapDbStorageServiceTest {

    private static final String KEY_1 = "Key1";
    private static final String KEY_2 = "Key2";

    private static class PersistedItem {
        public final String itemType;
        public final List<String> groupNames;
        public final @Nullable String baseItemType;

        public PersistedItem(String itemType, List<String> groupNames) {
            this(itemType, groupNames, null);
        }

        public PersistedItem(String itemType, List<String> groupNames, @Nullable String baseItemType) {
            this.itemType = itemType;
            this.groupNames = groupNames;
            this.baseItemType = baseItemType;
        }

        @Override
        public String toString() {
            return String.format("PersistedItem [itemType=%s, groupNames=%s, baseItemType=%s]", itemType, groupNames,
                    baseItemType);
        }
    }

    private static class MockConfiguration {
        private final Map<String, Object> configuration = new HashMap<>();

        public void put(String key, Object value) {
            configuration.put(key, value);
        }

        public @Nullable Object get(String key) {
            return configuration.get(key);
        }
    }

    public static class EntryTypeSeparatorTest {
        public int num;
        public @Nullable String str;
        public boolean bool;

        @Override
        public String toString() {
            return "Entry [num=" + num + ", str=" + str + ", bool=" + bool + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (bool ? 1231 : 1237);
            result = prime * result + num;
            final String str = this.str;
            result = prime * result + ((str == null) ? 0 : str.hashCode());
            return result;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            EntryTypeSeparatorTest other = (EntryTypeSeparatorTest) obj;
            if (bool != other.bool) {
                return false;
            }
            if (num != other.num) {
                return false;
            }
            if (str == null) {
                if (other.str != null) {
                    return false;
                }
            } else if (!Objects.equals(str, other.str)) {
                return false;
            }
            return true;
        }

    }

    private @NonNullByDefault({}) Path tmpDir;
    private @NonNullByDefault({}) MapDbStorageService storageService;
    private @NonNullByDefault({}) MapDbStorage<Object> storage;

    @Before
    public void setup() throws IOException {
        tmpDir = Files.createTempDirectory(null);
        final Path userdata = tmpDir.resolve("userdata");
        userdata.toFile().mkdir();
        System.setProperty(ConfigConstants.USERDATA_DIR_PROG_ARGUMENT, userdata.toString());

        storageService = new MapDbStorageService();
        storageService.activate();
        this.storage = (MapDbStorage<Object>) storageService.getStorage("TestStorage", getClass().getClassLoader());
    }

    @After
    public void teardown() throws IOException {
        if (storage != null) {
            storage.delete();
            storage = null;
        }
        if (storageService != null) {
            storageService.deactivate();
            storageService = null;
        }

        // clean up database files ...
        removeDirRecursive(tmpDir);
    }

    private static void removeDirRecursive(final Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path).map(Path::toFile).sorted((o1, o2) -> -o1.compareTo(o2)).forEach(File::delete);
        }
    }

    /**
     * Assert elements are serialized and deserialized by the storage.
     */
    @Test
    public void serializationDeserialization() {
        Assert.assertThat(storage.getKeys().size(), Matchers.equalTo(0));
        storage.put(KEY_1, new PersistedItem(CoreItemFactory.STRING, Arrays.asList("LIGHT", "GROUND_FLOOR")));
        storage.put(KEY_2, new PersistedItem(CoreItemFactory.NUMBER, Arrays.asList("TEMPERATURE", "OUTSIDE")));
        Assert.assertThat(storage.getKeys().size(), Matchers.equalTo(2));
        final Object persistedObject = storage.get(KEY_1);
        Assert.assertThat(persistedObject, Matchers.instanceOf(PersistedItem.class));
        storage.remove(KEY_1);
        storage.remove(KEY_2);
        Assert.assertThat(storage.getKeys().size(), Matchers.equalTo(0));
    }

    /**
     * Assert old element gets overwritten when new value is stored under an existing key.
     */
    @Test
    public void override() {
        Object persistedObject = null;
        PersistedItem persistedItem = null;

        Assert.assertEquals(0, storage.getKeys().size());

        persistedObject = storage.put(KEY_1,
                new PersistedItem(CoreItemFactory.STRING, Arrays.asList("LIGHT", "GROUND_FLOOR")));
        Assert.assertEquals(1, storage.getKeys().size());
        Assert.assertNull(persistedObject);

        persistedObject = storage.get(KEY_1);
        Assert.assertTrue(persistedObject instanceof PersistedItem);
        persistedItem = (PersistedItem) persistedObject;
        Assert.assertEquals(CoreItemFactory.STRING, persistedItem.itemType);

        persistedObject = storage.put(KEY_1, new PersistedItem(CoreItemFactory.NUMBER, Arrays.asList("TEMPERATURE")));
        Assert.assertTrue(persistedObject instanceof PersistedItem);
        persistedItem = (PersistedItem) persistedObject;
        Assert.assertEquals(1, storage.getKeys().size());
        Assert.assertEquals(CoreItemFactory.STRING, persistedItem.itemType);

        persistedObject = storage.get(KEY_1);
        Assert.assertTrue(persistedObject instanceof PersistedItem);
        persistedItem = (PersistedItem) persistedObject;
        Assert.assertEquals(CoreItemFactory.NUMBER, persistedItem.itemType);

        storage.remove(KEY_1);
        Assert.assertEquals(0, storage.getKeys().size());
    }

    /**
     * Assert storage works without classloader.
     */
    @Test
    public void withoutClassloader() {
        final Storage<String> storageWithoutClassloader = storageService.getStorage("storageWithoutClassloader");
        final String value = "Value";
        storageWithoutClassloader.put(KEY_1, value);
        Assert.assertEquals(value, storageWithoutClassloader.get(KEY_1));
    }

    /**
     * Assert store configuration works.
     */
    @Test
    public void storeConfiguration() {
        final Storage<MockConfiguration> storageWithoutClassloader = storageService.getStorage("storage");
        final MockConfiguration configuration = new MockConfiguration();
        configuration.put(KEY_1, new BigDecimal(3));
        storageWithoutClassloader.put(KEY_2, configuration);
        final Object persistedObject = storageWithoutClassloader.get(KEY_2);
        Assert.assertTrue(persistedObject instanceof MockConfiguration);
        final MockConfiguration persistedConfiguration = (MockConfiguration) persistedObject;
        final Object cfgValue = persistedConfiguration.get(KEY_1);
        Assert.assertTrue(cfgValue instanceof BigDecimal);
    }

    /**
     * Checks that the usage of the type separator does not break the storage.
     */
    @Test
    public void typeSeparator() {
        final DeletableStorage<EntryTypeSeparatorTest> storage = storageService.getStorage("type_separator");
        try {
            final EntryTypeSeparatorTest entryOriginal = new EntryTypeSeparatorTest();
            entryOriginal.num = 2810;
            entryOriginal.str = MapDbStorage.TYPE_SEPARATOR;
            entryOriginal.bool = true;
            storage.put(KEY_1, entryOriginal);
            final EntryTypeSeparatorTest entryStorage = storage.get(KEY_1);
            Assert.assertThat(entryStorage, Matchers.equalTo(entryOriginal));
        } finally {
            storage.delete();
        }
    }

}
