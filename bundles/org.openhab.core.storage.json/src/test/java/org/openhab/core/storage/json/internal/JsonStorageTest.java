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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.test.java.JavaTest;

/**
 * This test makes sure that the JsonStorage loads all stored numbers as BigDecimal
 *
 * @author Stefan Triller - Initial contribution
 */
public class JsonStorageTest extends JavaTest {

    private JsonStorage<DummyObject> objectStorage;
    private File tmpFile;

    @BeforeEach
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

        assertNotNull(dummy);
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

        assertNotNull(dummy);
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

        assertNotNull(dummy);
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

        assertNotNull(dummy);
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
        String storageString1 = Files.readString(tmpFile.toPath());

        objectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0);
        objectStorage.flush();
        String storageString2 = Files.readString(tmpFile.toPath());

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
            configuration.put("multiInt", List.of(1, 2, 3));

            InnerObject inner = new InnerObject();
            inner.configuration.put("testChildLong", Long.valueOf("12"));
            channels.add(inner);
        }
    }

    private static class InnerObject {
        private final Configuration configuration = new Configuration();
    }
}
