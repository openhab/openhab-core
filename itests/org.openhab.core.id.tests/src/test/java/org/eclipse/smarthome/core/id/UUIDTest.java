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
package org.eclipse.smarthome.core.id;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.smarthome.config.core.ConfigConstants;
import org.junit.Test;

/**
 * @author Kai Kreuzer - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class UUIDTest {

    @Test
    public void sameUUID() {
        String uuid1 = InstanceUUID.get();
        String uuid2 = InstanceUUID.get();
        assertEquals(uuid1, uuid2);
    }

    @Test
    public void readFromPersistedFile() throws IOException {
        // we first need to remove the cached value
        InstanceUUID.uuid = null;
        Path path = Paths.get(ConfigConstants.getUserDataFolder(), InstanceUUID.UUID_FILE_NAME);
        Files.createDirectories(path.getParent());
        Files.write(path, "123".getBytes());
        String uuid = InstanceUUID.get();
        assertEquals("123", uuid);
    }

    @Test
    public void ignoreEmptyFile() throws IOException {
        // we first need to remove the cached value
        InstanceUUID.uuid = null;
        Path path = Paths.get(ConfigConstants.getUserDataFolder(), InstanceUUID.UUID_FILE_NAME);
        Files.createDirectories(path.getParent());
        Files.write(path, "".getBytes());
        String uuid = InstanceUUID.get();
        assertNotEquals("", uuid);
    }
}
