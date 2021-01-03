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
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.storage.json.StorageMigration;
import org.openhab.core.test.java.JavaTest;

/**
 * This test makes sure that the migration algorithm works.
 *
 * @author Simon Lamon - Initial contribution
 */
@NonNullByDefault
public class JsonStorageMigrationTest extends JavaTest {

    private @NonNullByDefault({}) File tmpFile;

    @BeforeEach
    public void setUp() throws IOException {
        tmpFile = File.createTempFile("storage-debug", ".json");
        tmpFile.deleteOnExit();
    }

    @Test
    public void attemptMigration() {
        // JsonStorage: OldObject classes => Migration => NewObject classes

        JsonStorage<OldObject> oldObjectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0);
        oldObjectStorage.put("DummyObject", new OldObject(5, "7"));
        oldObjectStorage.flush();

        oldObjectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0);
        OldObject oldDummy = oldObjectStorage.get("DummyObject");
        assertNotNull(oldDummy);

        List<StorageMigration> storageMigrations = List.of(new OldObjectToNewObject());
        JsonStorage<NewObject> newObjectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0,
                storageMigrations);
        NewObject newDummy = newObjectStorage.get("DummyObject");
        assertNotNull(newDummy);
        if (oldDummy != null && newDummy != null) {
            assertEquals(oldDummy.getTestNumber(), newDummy.getTestNumber());
            assertEquals(oldDummy.getTestString(), newDummy.getTestString());
        }
    }

    @Test
    public void attemptMigrationWithChangedClasspath() {
        // JsonStorage: OldObject classes => Migration => NewObject classes
        // The OldObject classpath no longer exists and changed to OldObjectWithChangedClassPath

        JsonStorage<OldObject> oldObjectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0);
        oldObjectStorage.put("DummyObject", new OldObject(5, "7"));
        oldObjectStorage.flush();

        oldObjectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0);
        OldObject oldDummy = oldObjectStorage.get("DummyObject");
        assertNotNull(oldDummy);

        List<StorageMigration> storageMigrations = List.of(new OldObjectToNewObjectChangedClasspath());
        JsonStorage<NewObject> newObjectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0,
                storageMigrations);
        NewObject newDummy = newObjectStorage.get("DummyObject");
        assertNotNull(newDummy);
        if (oldDummy != null && newDummy != null) {
            assertEquals(oldDummy.getTestNumber(), newDummy.getTestNumber());
            assertEquals(oldDummy.getTestString(), newDummy.getTestString());
        }
    }

    @Test
    public void attemptMultipleMigrations() {
        // JsonStorage: OldObject classes => Migration => InBetweenObject => Migration => NewObject classes

        JsonStorage<OldObject> oldObjectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0);
        oldObjectStorage.put("DummyObject", new OldObject(5, "7"));
        oldObjectStorage.flush();

        oldObjectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0);
        OldObject oldDummy = oldObjectStorage.get("DummyObject");
        assertNotNull(oldDummy);

        List<StorageMigration> storageMigrations = List.of(new OldObjectToIntermediateObject(),
                new InBetweenObjectToNewObject());
        JsonStorage<NewObject> newObjectStorage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0,
                storageMigrations);
        NewObject newDummy = newObjectStorage.get("DummyObject");
        assertNotNull(newDummy);
        if (oldDummy != null && newDummy != null) {
            assertEquals(oldDummy.getTestNumber(), newDummy.getTestNumber());
            assertEquals(oldDummy.getTestString(), newDummy.getTestString());
        }

    }

    private class OldObjectToNewObjectChangedClasspath extends StorageMigration {
        public OldObjectToNewObjectChangedClasspath() {
            super(OldObject.class.getTypeName(), OldObjectWithChangedClassPath.class, NewObject.class.getTypeName(),
                    NewObject.class);
        }

        @Override
        public Object migrate(Object in) {
            OldObjectWithChangedClassPath oldObject = (OldObjectWithChangedClassPath) in;
            return new NewObject(oldObject.getTestNumber(), oldObject.getTestString());
        }
    }

    private class OldObjectToNewObject extends StorageMigration {
        public OldObjectToNewObject() {
            super(OldObject.class, NewObject.class);
        }

        @Override
        public Object migrate(Object in) {
            OldObject oldObject = (OldObject) in;
            return new NewObject(oldObject.getTestNumber(), oldObject.getTestString());
        }
    }

    private class OldObjectToIntermediateObject extends StorageMigration {
        public OldObjectToIntermediateObject() {
            super(OldObject.class, InBetweenObject.class);
        }

        @Override
        public Object migrate(Object in) {
            OldObject oldObject = (OldObject) in;
            return new InBetweenObject(oldObject.getTestNumber(), oldObject.getTestString());
        }
    }

    private class InBetweenObjectToNewObject extends StorageMigration {
        public InBetweenObjectToNewObject() {
            super(InBetweenObject.class, NewObject.class);
        }

        @Override
        public Object migrate(Object in) {
            InBetweenObject oldObject = (InBetweenObject) in;
            return new NewObject(oldObject.getTestNumber(), oldObject.getTestString());
        }
    }

    private static class NewObject {
        private int testNumber3;
        private String testString3;

        public NewObject(int testNumber, String testString) {
            this.testNumber3 = testNumber;
            this.testString3 = testString;
        }

        public int getTestNumber() {
            return testNumber3;
        }

        public String getTestString() {
            return testString3;
        }
    }

    private static class InBetweenObject {
        private int testNumber2;
        private String testString2;

        public InBetweenObject(int testNumber, String testString) {
            this.testNumber2 = testNumber;
            this.testString2 = testString;
        }

        public int getTestNumber() {
            return testNumber2;
        }

        public String getTestString() {
            return testString2;
        }
    }

    private static class OldObject {
        private int testNumber;
        private String testString;

        public OldObject(int testNumber, String testString) {
            this.testNumber = testNumber;
            this.testString = testString;
        }

        public int getTestNumber() {
            return testNumber;
        }

        public String getTestString() {
            return testString;
        }
    }

    private static class OldObjectWithChangedClassPath {
        private int testNumber;
        private String testString;

        @SuppressWarnings("unused")
        public OldObjectWithChangedClassPath(int testNumber, String testString) {
            this.testNumber = testNumber;
            this.testString = testString;
        }

        public int getTestNumber() {
            return testNumber;
        }

        public String getTestString() {
            return testString;
        }
    }
}
