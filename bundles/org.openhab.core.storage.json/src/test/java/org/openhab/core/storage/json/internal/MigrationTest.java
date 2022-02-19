/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.storage.json.internal.migration.RenamingTypeMigrator;
import org.openhab.core.storage.json.internal.migration.TypeMigrationException;
import org.openhab.core.storage.json.internal.migration.TypeMigrator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {@link MigrationTest} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class MigrationTest {
    private static final String OBJECT_KEY = "foo";
    private static final String OBJECT_VALUE = "bar";

    private @NonNullByDefault({}) File tmpFile;

    @BeforeEach
    public void setup() throws IOException {
        tmpFile = File.createTempFile("storage-debug", ".json");
        tmpFile.deleteOnExit();

        // store old class
        OldNameClass oldNameInstance = new OldNameClass(OBJECT_VALUE);
        JsonStorage<OldNameClass> storage = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0,
                List.of());
        storage.put(OBJECT_KEY, oldNameInstance);
        storage.flush();
    }

    @Test
    public void testRenameClassMigration() {
        // read new class
        JsonStorage<NewNameClass> storage1 = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0,
                List.of(new RenamingTypeMigrator(OldNameClass.class.getName(), NewNameClass.class.getName())));
        NewNameClass newNameInstance = storage1.get(OBJECT_KEY);
        assertNotNull(newNameInstance);

        Objects.requireNonNull(newNameInstance);
        assertEquals(OBJECT_VALUE, newNameInstance.value);
    }

    @Test
    public void testRenameFieldMigration() {
        // read new class
        JsonStorage<NewFieldClass> storage1 = new JsonStorage<>(tmpFile, this.getClass().getClassLoader(), 0, 0, 0,
                List.of(new OldToNewFieldMigrator()));
        NewFieldClass newNameInstance = storage1.get(OBJECT_KEY);
        assertNotNull(newNameInstance);

        Objects.requireNonNull(newNameInstance);
        assertEquals(OBJECT_VALUE, newNameInstance.val);
    }

    @SuppressWarnings("unused")
    private static class OldNameClass {
        public String value;

        public OldNameClass(String value) {
            this.value = value;
        }
    }

    @SuppressWarnings("unused")
    private static class NewNameClass {
        public String value;

        public NewNameClass(String value) {
            this.value = value;
        }
    }

    @SuppressWarnings("unused")
    private static class NewFieldClass {
        public String val;

        public NewFieldClass(String value) {
            this.val = value;
        }
    }

    private static class OldToNewFieldMigrator implements TypeMigrator {

        @Override
        public String getOldType() {
            return OldNameClass.class.getName();
        }

        @Override
        public String getNewType() {
            return NewFieldClass.class.getName();
        }

        @Override
        public JsonElement migrate(JsonElement oldValue) throws TypeMigrationException {
            JsonObject newElement = oldValue.getAsJsonObject();
            JsonElement element = newElement.remove("value");
            newElement.add("val", element);
            return newElement;
        }
    }
}
