/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.storage.mapdb.internal;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.storage.DeletableStorage;
import org.eclipse.smarthome.core.storage.Storage;
import org.eclipse.smarthome.core.storage.StorageService;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Alex Tugarev - Added test for getStorage without classloader
 * @author Markus Rathgeb - Migrate Groovy tests to OSGi
 */
@NonNullByDefault
public class StorageServiceOSGiTest extends JavaOSGiTest {

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
        private final Map<String, Object> configuration = new HashMap<String, Object>();

        public void put(String key, Object value) {
            configuration.put(key, value);
        }

        public Object get(String key) {
            return configuration.get(key);
        }
    }

    private @NonNullByDefault({}) MapDbStorageService storageService;
    private @NonNullByDefault({}) MapDbStorage<Object> storage;

    @Before
    public void setUp() {
        storageService = getService(StorageService.class, MapDbStorageService.class);
        final DeletableStorage<Object> storage = storageService.getStorage("TestStorage", getClass().getClassLoader());
        Assert.assertTrue(storage instanceof MapDbStorage);
        this.storage = (MapDbStorage<Object>) storage;
    }

    @After
    public void tearDown() throws IOException {
        unregisterService(storageService);

        // clean up database files ...
        removeDirRecursive("userdata");
        removeDirRecursive("runtime");
    }

    private static void removeDirRecursive(final String dir) throws IOException {
        final Path path = Paths.get(dir);
        if (Files.exists(path)) {
            Files.walk(path).map(Path::toFile).sorted((o1, o2) -> -o1.compareTo(o2)).forEach(File::delete);
        }
    }

    /**
     * Assert elements are serialized and deserialized by the storage.
     */
    @Test
    public void serializationDeserialization() {
        Assert.assertEquals(0, storage.getKeys().size());
        storage.put(KEY_1, new PersistedItem("String", Arrays.asList("LIGHT", "GROUND_FLOOR")));
        storage.put(KEY_2, new PersistedItem("Number", Arrays.asList("TEMPERATURE", "OUTSIDE")));
        Assert.assertEquals(2, storage.getKeys().size());
        final Object persistedObject = storage.get(KEY_1);
        Assert.assertTrue(persistedObject instanceof PersistedItem);
        storage.remove(KEY_1);
        storage.remove(KEY_2);
        Assert.assertEquals(0, storage.getKeys().size());
    }

    /**
     * Assert old element gets overwritten when new value is stored under an existing key.
     */
    @Test
    public void override() {
        Object persistedObject = null;
        PersistedItem persistedItem = null;

        Assert.assertEquals(0, storage.getKeys().size());

        persistedObject = storage.put(KEY_1, new PersistedItem("String", Arrays.asList("LIGHT", "GROUND_FLOOR")));
        Assert.assertEquals(1, storage.getKeys().size());
        Assert.assertNull(persistedObject);

        persistedObject = storage.get(KEY_1);
        Assert.assertTrue(persistedObject instanceof PersistedItem);
        persistedItem = (PersistedItem) persistedObject;
        Assert.assertEquals("String", persistedItem.itemType);

        persistedObject = storage.put(KEY_1, new PersistedItem("Number", Arrays.asList("TEMPERATURE")));
        Assert.assertTrue(persistedObject instanceof PersistedItem);
        persistedItem = (PersistedItem) persistedObject;
        Assert.assertEquals(1, storage.getKeys().size());
        Assert.assertEquals("String", persistedItem.itemType);

        persistedObject = storage.get(KEY_1);
        Assert.assertTrue(persistedObject instanceof PersistedItem);
        persistedItem = (PersistedItem) persistedObject;
        Assert.assertEquals("Number", persistedItem.itemType);

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

}
