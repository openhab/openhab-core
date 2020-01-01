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
package org.openhab.core.storage.json.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.core.ConfigConstants;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * @author Simon Kaufmann - Initial contribution
 */
public class JsonStorageServiceOSGiTest extends JavaOSGiTest {

    private static final String KEY1 = "Key1";
    private static final String KEY2 = "Key2";

    private StorageService storageService;
    private Storage<PersistedItem> storage;

    @Before
    public void setUp() {
        storageService = getService(StorageService.class);
        storage = storageService.getStorage(UUID.randomUUID().toString(), this.getClass().getClassLoader());
    }

    @After
    public void tearDown() throws IOException {
        unregisterService(storageService);
    }

    @AfterClass
    public static void afterClass() throws IOException {
        // clean up database files ...
        FileUtils.deleteDirectory(new File(ConfigConstants.getUserDataFolder()));
        FileUtils.deleteDirectory(new File(ConfigConstants.getConfigFolder()));
    }

    @Test
    public void testOnlyAlphanumericCharsInFileName() throws UnsupportedEncodingException {
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

        storage.put(KEY1, new PersistedItem(CoreItemFactory.STRING, Arrays.asList("LIGHT", "GROUND_FLOOR")));
        storage.put(KEY2, new PersistedItem(CoreItemFactory.NUMBER, Arrays.asList("TEMPERATURE", "OUTSIDE")));
        assertThat(storage.getKeys().size(), is(2));

        storage.remove(KEY1);
        storage.remove(KEY2);
        assertThat(storage.getKeys().size(), is(0));
    }

    @Test
    public void testOverride() {
        PersistedItem pItem = null;

        assertThat(storage.getKeys().size(), is(0));

        pItem = storage.put(KEY1, new PersistedItem(CoreItemFactory.STRING, Arrays.asList("LIGHT", "GROUND_FLOOR")));
        assertThat(storage.getKeys().size(), is(1));
        assertThat(pItem, is(nullValue()));

        pItem = storage.get(KEY1);
        assertThat(pItem, is(notNullValue()));
        assertThat(pItem.itemType, is(CoreItemFactory.STRING));

        pItem = storage.put(KEY1, new PersistedItem(CoreItemFactory.NUMBER, Arrays.asList("TEMPERATURE")));
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

        public String itemType;
        public List<String> groupNames;
        public String baseItemType;

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

        public PersistedItem(String itemType, List<String> groupNames, String baseItemType) {
            this.itemType = itemType;
            this.groupNames = groupNames;
            this.baseItemType = baseItemType;
        }

    }

}
