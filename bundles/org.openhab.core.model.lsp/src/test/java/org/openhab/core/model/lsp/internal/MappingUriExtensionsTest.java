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
package org.openhab.core.model.lsp.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class MappingUriExtensionsTest {

    public @TempDir @NonNullByDefault({}) File folder;
    private @NonNullByDefault({}) File confFolder;

    public static Collection<Object[]> data() {
        return List.of(new Object[][] { //
                { "conf", //
                        "file:///q:/conf", //
                        "file:///q:/conf", //
                        "" }, //
                { "conf", //
                        "file:///q:", //
                        "file:///q:", //
                        "" }, //
                { "conf", //
                        "file://fqdn/openhab-conf", //
                        "file://fqdn/openhab-conf", //
                        "" }, //
                { "conf", //
                        "file://fqdn/openhab-conf/conf", //
                        "file://fqdn/openhab-conf/conf", //
                        "" }, //
                { null, //
                        "file:///asdf/conf/items/test.items", //
                        "file:///asdf/conf", //
                        "items/test.items" }, //
                { null, //
                        "file:///asdf/items/test.items", //
                        "file:///asdf", //
                        "items/test.items" }, //
                { null, //
                        "file://fqdn/openhab-conf/conf/items/test.items", //
                        "file://fqdn/openhab-conf/conf", //
                        "items/test.items" }, //
                { null, //
                        "file://fqdn/openhab-conf/items/test.items", //
                        "file://fqdn/openhab-conf", //
                        "items/test.items" },//
        });
    }

    @BeforeEach
    public void setup() throws IOException {
        confFolder = new File(folder, "conf");

        File itemsFolder = new File(confFolder, "items");
        itemsFolder.mkdirs();

        File itemsFile = new File(itemsFolder, "test.items");
        itemsFile.deleteOnExit();
        itemsFile.createNewFile();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testGuessClientPath(String conf, String request, String expectedClientPath, String expectedUriPath) {
        MappingUriExtensions mapper = createMapper(conf);
        String clientPath = mapper.guessClientPath(request);
        assertEquals(expectedClientPath, clientPath);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testToUri(String conf, String request, String expectedClientPath, String expectedUriPath) {
        MappingUriExtensions mapper = createMapper(conf);
        URI clientPath = mapper.toUri(request);
        assertEquals(confFolder.toPath().toUri().toString() + expectedUriPath, clientPath.toString());
    }

    private MappingUriExtensions createMapper(String conf) {
        return new MappingUriExtensions(conf) {
            @Override
            protected String calcServerLocation(@Nullable String configFolder) {
                // ensure test execution is independent from the current working directory
                return removeTrailingSlash(confFolder.toPath().toUri().toString());
            }
        };
    }
}
