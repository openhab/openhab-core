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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.test.java.JavaTest;
import org.junit.Before;
import org.junit.Test;

/**
 * This test makes sure that the JSonStorage loads all stored numbers as BigDecimal
 *
 * @author Stefan Triller - Initial Contribution
 */
public class JSonStorageTest extends JavaTest {

    private JsonStorage<DummyObject> objectStorage;
    private File tmpFile;

    @Before
    public void setUp() throws IOException {
        tmpFile = File.createTempFile("storage-debug", ".json");
        tmpFile.deleteOnExit();
        objectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0);
    }

    private void persistAndReadAgain() {
        objectStorage.flush();
        waitForAssert(() -> {
            objectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0);
            DummyObject dummy = objectStorage.get("DummyObject");
            assertNotNull(dummy);
            assertNotNull(dummy.configuration);
        });
    }

    @Test
    public void allInsertedNumbersAreLoadedAsBigDecimalFromCache() {
        objectStorage.put("DummyObject", new DummyObject());
        DummyObject dummy = objectStorage.get("DummyObject");

        assertTrue(dummy.configuration.get("testShort") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testInt") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testLong") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testDouble") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testFloat") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testBigDecimal") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testBoolean") instanceof Boolean);
        assertTrue(dummy.configuration.get("testString") instanceof String);
    }

    @Test
    public void allInsertedNumbersAreLoadedAsBigDecimalFromDisk() {
        objectStorage.put("DummyObject", new DummyObject());
        persistAndReadAgain();
        DummyObject dummy = objectStorage.get("DummyObject");

        assertTrue(dummy.configuration.get("testShort") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testInt") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testLong") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testDouble") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testFloat") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testBigDecimal") instanceof BigDecimal);
        assertTrue(dummy.configuration.get("testBoolean") instanceof Boolean);
        assertTrue(dummy.configuration.get("testString") instanceof String);
    }

    @Test
    public void testIntegerScaleFromCache() {
        objectStorage.put("DummyObject", new DummyObject());
        DummyObject dummy = objectStorage.get("DummyObject");

        assertEquals(((BigDecimal) dummy.configuration.get("testShort")).scale(), 0);
        assertEquals(((BigDecimal) dummy.configuration.get("testInt")).scale(), 0);
        assertEquals(((BigDecimal) dummy.configuration.get("testLong")).scale(), 0);
        assertEquals(((BigDecimal) dummy.configuration.get("testBigDecimal")).scale(), 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIntegerScaleFromDisk() {
        objectStorage.put("DummyObject", new DummyObject());
        persistAndReadAgain();
        DummyObject dummy = objectStorage.get("DummyObject");

        assertEquals(((BigDecimal) dummy.configuration.get("testShort")).scale(), 0);
        assertEquals(((BigDecimal) dummy.configuration.get("testInt")).scale(), 0);
        assertEquals(((BigDecimal) dummy.configuration.get("testLong")).scale(), 0);
        assertEquals(((BigDecimal) dummy.configuration.get("testBigDecimal")).scale(), 0);
        assertEquals(((List<BigDecimal>) dummy.configuration.get("multiInt")).get(0).scale(), 0);
        assertEquals(((List<BigDecimal>) dummy.configuration.get("multiInt")).get(1).scale(), 0);
        assertEquals(((List<BigDecimal>) dummy.configuration.get("multiInt")).get(2).scale(), 0);
        assertEquals(((BigDecimal) dummy.channels.get(0).configuration.get("testChildLong")).scale(), 0);
    }

    @Test
    public void testStableOutput() throws IOException {
        objectStorage.put("DummyObject", new DummyObject());
        persistAndReadAgain();
        String storageString1 = FileUtils.readFileToString(tmpFile);

        objectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0);
        objectStorage.flush();
        String storageString2 = FileUtils.readFileToString(tmpFile);

        assertEquals(storageString1, storageString2);
    }

    private static class DummyObject {

        private final Configuration configuration = new Configuration();
        public List<InnerObject> channels = new ArrayList<>();

        public DummyObject() {
            configuration.put("testShort", Short.valueOf("12"));
            configuration.put("testInt", Integer.valueOf("12"));
            configuration.put("testLong", Long.valueOf("12"));
            configuration.put("testDouble", Double.valueOf("12.12"));
            configuration.put("testFloat", Float.valueOf("12.12"));
            configuration.put("testBigDecimal", new BigDecimal(12));
            configuration.put("testBoolean", true);
            configuration.put("testString", "hello world");
            configuration.put("multiInt", Arrays.asList(1, 2, 3));

            InnerObject inner = new InnerObject();
            inner.configuration.put("testChildLong", Long.valueOf("12"));
            channels.add(inner);
        }
    }

    private static class InnerObject {
        private final Configuration configuration = new Configuration();
    }

}
