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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.OpenHAB;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;

/**
 * The {@link ThingMigrationOSGiTest} is a test for thing migrations
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ThingMigrationOSGiTest extends JavaOSGiTest {
    private static final Path DB_DIR = Path.of(OpenHAB.getUserDataFolder(), "jsondb");
    private static final String DB_NAME = "org.openhab.core.thing.Thing";
    private static final String DB_OLD_NAME = "org.openhab.core.thing.Thing-old";

    @Test
    public void migrationParsable() throws IOException {
        Files.createDirectories(DB_DIR);

        StorageService storageService = Objects.requireNonNull(getService(StorageService.class));

        // prepare storage
        Files.copy(bundleContext.getBundle().getResource(DB_NAME + ".json").openStream(),
                DB_DIR.resolve(DB_NAME + ".json"));

        // we need to go over a ManagedThingProvider because the ThingStorageEntity is an internal package
        ManagedThingProvider managedThingProvider = new ManagedThingProvider(storageService);
        Collection<Thing> things = managedThingProvider.getAll();
        assertThat(things.size(), is(2));

        unregisterService(storageService);
    }
}
