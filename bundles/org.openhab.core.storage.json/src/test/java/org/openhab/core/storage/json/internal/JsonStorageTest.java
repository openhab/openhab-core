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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.test.java.JavaTest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * This test makes sure that the JsonStorage loads all stored numbers as BigDecimal
 *
 * @author Stefan Triller - Initial contribution
 * @author Samie Salonen - test for ensuring ordering of keys in json
 */
@NonNullByDefault
public class JsonStorageTest extends JavaTest {

    private @NonNullByDefault({}) JsonStorage<DummyObject> objectStorage;
    private @NonNullByDefault({}) File tmpFile;

    @BeforeEach
    public void setUp() throws IOException {
        tmpFile = File.createTempFile("storage-debug", ".json");
        tmpFile.deleteOnExit();
        objectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0, List.of());
    }

    private void persistAndReadAgain() {
        objectStorage.flush();
        waitForAssert(() -> {
            objectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0, List.of());
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

        objectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0, List.of());
        objectStorage.flush();
        String storageString2 = Files.readString(tmpFile.toPath());

        assertEquals(storageString1, storageString2);
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Test
    public void testOrdering() throws IOException {
        objectStorage.put("DummyObject", new DummyObject());
        {
            objectStorage.put("a", new DummyObject());
            objectStorage.put("b", new DummyObject());
            persistAndReadAgain();
        }
        String storageStringAB = Files.readString(tmpFile.toPath());

        {
            objectStorage.remove("a");
            objectStorage.remove("b");
            objectStorage.put("b", new DummyObject());
            objectStorage.put("a", new DummyObject());
            persistAndReadAgain();
        }
        String storageStringBA = Files.readString(tmpFile.toPath());
        assertEquals(storageStringAB, storageStringBA);

        {
            objectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0, List.of());
            objectStorage.flush();
        }
        String storageStringReserialized = Files.readString(tmpFile.toPath());
        assertEquals(storageStringAB, storageStringReserialized);
        Gson gson = new GsonBuilder().create();

        // Parse json. Gson preserves json object key ordering when we parse only JsonObject
        JsonObject orderedMap = gson.fromJson(storageStringAB, JsonObject.class);
        // Assert ordering of top level keys (uppercase first in alphabetical order, then lowercase items in
        // alphabetical order)
        assertArrayEquals(new String[] { "DummyObject", "a", "b" }, orderedMap.keySet().toArray());
        // Ordering is ensured also for sub-keys of Configuration object
        assertArrayEquals(
                new String[] { "multiInt", "testBigDecimal", "testBoolean", "testDouble", "testFloat", "testInt",
                        "testLong", "testShort", "testString" },
                orderedMap.getAsJsonObject("DummyObject").getAsJsonObject("value").getAsJsonObject("configuration")
                        .getAsJsonObject("properties").keySet().toArray());
        // Set having non-comparable items remains unordered
        assertArrayEquals(
                new String[] { "http://www.example.com/key2", "http://www.example.com/key1",
                        "http://www.example.com/key3" },
                gson.fromJson(orderedMap.getAsJsonObject("DummyObject").getAsJsonObject("value")
                        .getAsJsonArray("innerSetWithNonComparableElements"), LinkedList.class).toArray());
        // ...while Set having all Comparable keys is ordered:
        assertArrayEquals(new Object[] { -5, 0, 3, 50 },
                ((LinkedList<Integer>) gson.fromJson(
                        orderedMap.getAsJsonObject("DummyObject").getAsJsonObject("value")
                                .getAsJsonArray("innerSetWithComparableElements"),
                        TypeToken.getParameterized(LinkedList.class, Integer.class).getType())).toArray());

        // Map having non-comparable items remains unordered
        assertArrayEquals(
                new String[] { "http://www.example.com/key2", "http://www.example.com/key1",
                        "http://www.example.com/key3" },
                gson.fromJson(orderedMap.getAsJsonObject("DummyObject").getAsJsonObject("value")
                        .getAsJsonObject("innerMapWithNonComparableKeys"), LinkedHashMap.class).keySet().toArray());
        // ...while Map with Comparable keys is ordered
        assertArrayEquals(new Integer[] { -5, 0, 3, 50 },
                ((LinkedHashMap<Integer, Object>) gson.fromJson(
                        orderedMap.getAsJsonObject("DummyObject").getAsJsonObject("value")
                                .getAsJsonObject("innerMapWithComparableKeys"),
                        TypeToken.getParameterized(LinkedHashMap.class, Integer.class, Object.class).getType()))
                        .keySet().toArray());
    }

    private static class DummyObject {

        // For the test here we use Linked variants of Map and Set which preserve the insertion order
        // In tests we verify that collections having Comparable items (keys) are ordered on serialization
        private final Map<URL, Object> innerMapWithNonComparableKeys = new LinkedHashMap<>();
        private final Map<Integer, Object> innerMapWithComparableKeys = new LinkedHashMap<>();
        private final Set<URL> innerSetWithNonComparableElements = new LinkedHashSet<>();
        private final Set<Integer> innerSetWithComparableElements = new LinkedHashSet<>();
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
            innerMapWithComparableKeys.put(3, 1);
            innerMapWithComparableKeys.put(0, 2);
            innerMapWithComparableKeys.put(50, 3);
            innerMapWithComparableKeys.put(-5, 4);

            innerSetWithComparableElements.add(3);
            innerSetWithComparableElements.add(0);
            innerSetWithComparableElements.add(50);
            innerSetWithComparableElements.add(-5);

            innerMapWithNonComparableKeys.put(newURL("http://www.example.com/key2"), 1);
            innerMapWithNonComparableKeys.put(newURL("http://www.example.com/key1"), 2);
            innerMapWithNonComparableKeys.put(newURL("http://www.example.com/key3"), 3);

            innerSetWithNonComparableElements.add(newURL("http://www.example.com/key2"));
            innerSetWithNonComparableElements.add(newURL("http://www.example.com/key1"));
            innerSetWithNonComparableElements.add(newURL("http://www.example.com/key3"));
        }
    }

    private static URL newURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static class InnerObject {
        private final Configuration configuration = new Configuration();
    }
}
