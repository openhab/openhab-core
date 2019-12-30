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
package org.openhab.core.model.lsp.internal;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.emf.common.util.URI;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 *
 * @author Simon Kaufmann - Initial contribution
 */
@RunWith(Parameterized.class)
public class MappingUriExtensionsTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    private File confFolder;
    private File itemsFolder;
    private File itemsFile;
    private final String conf;
    private final String request;
    private final String expectedClientPath;
    private final String expectedUriPath;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
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

    public MappingUriExtensionsTest(String conf, String request, String expectedClientPath, String expectedUriPath) {
        this.conf = conf;
        this.request = request;
        this.expectedClientPath = expectedClientPath;
        this.expectedUriPath = expectedUriPath;
    }

    @Before
    public void setup() throws Exception {
        confFolder = folder.newFolder("conf");
        itemsFolder = new File(confFolder, "items");
        itemsFolder.mkdirs();
        itemsFile = new File(itemsFolder, "test.items");
        itemsFile.deleteOnExit();
        itemsFile.createNewFile();
    }

    @Test
    public void testGuessClientPath() {
        MappingUriExtensions mapper = createMapper();
        String clientPath = mapper.guessClientPath(request);
        assertEquals(expectedClientPath, clientPath);
    }

    @Test
    public void testToUri() {
        MappingUriExtensions mapper = createMapper();
        URI clientPath = mapper.toUri(request);
        assertEquals(confFolder.toPath().toUri().toString() + expectedUriPath, clientPath.toString());
    }

    @Test
    public void testToPathEmfURI() {
        MappingUriExtensions mapper = createMapper();
        mapper.toUri(request);

        URI uri = URI.createURI(confFolder.toPath().toUri().toString() + expectedUriPath);
        String res = mapper.toPath(uri);
        assertEquals(request, res);
    }

    private MappingUriExtensions createMapper() {
        return new MappingUriExtensions(conf) {
            @Override
            protected String calcServerLocation(String configFolder) {
                // ensure test execution is independent from the current working directory
                return removeTrailingSlash(confFolder.toPath().toUri().toString());
            }
        };
    }

}
