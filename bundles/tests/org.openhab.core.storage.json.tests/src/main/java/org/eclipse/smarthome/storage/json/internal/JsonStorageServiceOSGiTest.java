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
package org.eclipse.smarthome.storage.json.internal;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.storage.Storage;
import org.eclipse.smarthome.core.storage.StorageService;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Simon Kaufmann - Initial implementation
 */
public class JsonStorageServiceOSGiTest extends JavaOSGiTest {

    private StorageService storageService;
    private Storage<PersistedItem> storage;

    @Before
    public void setUp() {
        storageService = getService(StorageService.class);
        storage = storageService.getStorage("TestStorage", this.getClass().getClassLoader());
    }

    @After
    public void tearDown() throws IOException {
        unregisterService(storageService);

        // clean up database files ...
        FileUtils.deleteDirectory(new File("./runtime"));
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

        storage.put("Key1", new PersistedItem("String", Arrays.asList("LIGHT", "GROUND_FLOOR")));
        storage.put("Key2", new PersistedItem("Number", Arrays.asList("TEMPERATURE", "OUTSIDE")));
        assertThat(storage.getKeys().size(), is(2));

        storage.remove("Key1");
        storage.remove("Key2");
        assertThat(storage.getKeys().size(), is(0));
    }

    @Test
    public void testOverride() {
        PersistedItem pItem = null;

        assertThat(storage.getKeys().size(), is(0));

        pItem = storage.put("Key1", new PersistedItem("String", Arrays.asList("LIGHT", "GROUND_FLOOR")));
        assertThat(storage.getKeys().size(), is(1));
        assertThat(pItem, is(nullValue()));

        pItem = storage.get("Key1");
        Assert.assertNotNull(pItem);
        assertThat(pItem.itemType, is("String"));

        pItem = storage.put("Key1", new PersistedItem("Number", Arrays.asList("TEMPERATURE")));
        Assert.assertNotNull(pItem);
        assertThat(storage.getKeys().size(), is(1));
        assertThat(pItem.itemType, is("String"));
        assertThat(storage.get("Key1").itemType, is("Number"));

        storage.remove("Key1");
        assertThat(storage.getKeys().size(), is(0));
    }

    @Test
    public void testClassloader() {
        Storage<String> storageWithoutClassloader = storageService.getStorage("storageWithoutClassloader");
        storageWithoutClassloader.put("Key1", "Value");

        assertThat(storageWithoutClassloader.get("Key1"), is(equalTo("Value")));
    }

    @Test
    public void testConfiguration() {
        Storage<DummyObject> storageWithoutClassloader = storageService.getStorage("storage");

        DummyObject dummy = new DummyObject();
        dummy.configuration.put("bigDecimal", new BigDecimal(3));

        storageWithoutClassloader.put("configuration", dummy);

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
