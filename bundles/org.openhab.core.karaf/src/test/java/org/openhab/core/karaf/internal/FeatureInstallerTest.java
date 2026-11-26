/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.karaf.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the detection of installed .kar files in {@link FeatureInstaller}.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class FeatureInstallerTest {

    private static final String KAR_NAME = "openhab-addons-5.3.0";

    private @TempDir @NonNullByDefault({}) Path tempDir;
    private @NonNullByDefault({}) Path addonsDirectory;
    private @NonNullByDefault({}) Path karStorage;

    @BeforeEach
    public void setUp() throws IOException {
        addonsDirectory = Files.createDirectory(tempDir.resolve("addons"));
        karStorage = Files.createDirectory(tempDir.resolve("kar"));
    }

    @Test
    public void noKarFiles() throws IOException {
        assertTrue(FeatureInstaller.allKarsInstalled(addonsDirectory, karStorage, List.of()));
    }

    @Test
    public void karNotInstalled() throws IOException {
        Files.createFile(addonsDirectory.resolve(KAR_NAME + ".kar"));

        assertFalse(FeatureInstaller.allKarsInstalled(addonsDirectory, karStorage, List.of()));
    }

    @Test
    public void karListedWhileBeingExtracted() throws IOException {
        Files.createFile(addonsDirectory.resolve(KAR_NAME + ".kar"));
        Files.createDirectories(karStorage.resolve(KAR_NAME).resolve("repository"));

        assertFalse(FeatureInstaller.allKarsInstalled(addonsDirectory, karStorage, List.of(KAR_NAME)));
    }

    @Test
    public void karCompletelyExtracted() throws IOException {
        Files.createFile(addonsDirectory.resolve(KAR_NAME + ".kar"));
        Files.createDirectories(karStorage.resolve(KAR_NAME));
        Files.createFile(karStorage.resolve(KAR_NAME).resolve("features.cfg"));

        assertTrue(FeatureInstaller.allKarsInstalled(addonsDirectory, karStorage, List.of(KAR_NAME)));
    }
}
