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
import static org.hamcrest.Matchers.is;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.core.config.core.OrderingMapSerializer;
import org.openhab.core.config.core.OrderingSetSerializer;
import org.openhab.core.storage.json.internal.migration.PersistedTransformationTypeMigrator;
import org.openhab.core.storage.json.internal.migration.TypeMigrationException;
import org.openhab.core.storage.json.internal.migration.TypeMigrator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * The {@link PersistedTransformationMigratorTest} contains tests for the ThingImpl and BridgeImpl migrators
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistedTransformationMigratorTest {

    private final Gson internalMapper = new GsonBuilder() //
            .registerTypeHierarchyAdapter(Map.class, new OrderingMapSerializer())//
            .registerTypeHierarchyAdapter(Set.class, new OrderingSetSerializer())//
            .registerTypeHierarchyAdapter(Map.class, new StorageEntryMapDeserializer()) //
            .setPrettyPrinting() //
            .create();

    private @NonNullByDefault({}) Map<String, StorageEntry> inputMap;
    private @NonNullByDefault({}) Map<String, StorageEntry> resultMap;

    @BeforeEach
    public void setup() throws FileNotFoundException {
        inputMap = readDatabase(Path.of("src/test/resources/transformMigration-input.json"));
        resultMap = readDatabase(Path.of("src/test/resources/transformMigration-result.json"));

        assertThat(inputMap.size(), is(2));
        assertThat(resultMap.size(), is(2));
    }

    private static Stream<Arguments> typeMigrationsSource() {
        return Stream.of(Arguments.of("config:map:2480cdc5d0:de", new PersistedTransformationTypeMigrator()),
                Arguments.of("config:script:testTransCfg", new PersistedTransformationTypeMigrator()));
    }

    @ParameterizedTest
    @MethodSource("typeMigrationsSource")
    public void typeMigration(String thingUid, TypeMigrator migrator) throws TypeMigrationException {
        StorageEntry inputEntry = inputMap.get(thingUid);
        StorageEntry resultEntry = resultMap.get(thingUid);

        assertThat(inputEntry.getEntityClassName(), is(migrator.getOldType()));

        JsonElement entityValue = (JsonElement) inputEntry.getValue();
        JsonElement newEntityValue = migrator.migrate(entityValue);

        assertThat(newEntityValue, is(resultEntry.getValue()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, StorageEntry> readDatabase(Path path) throws FileNotFoundException {
        final Map<String, StorageEntry> map = new ConcurrentHashMap<>();

        FileReader reader = new FileReader(path.toFile());
        Map<String, StorageEntry> loadedMap = internalMapper.fromJson(reader, map.getClass());

        if (loadedMap != null && !loadedMap.isEmpty()) {
            map.putAll(loadedMap);
        }

        return map;
    }
}
