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
package org.eclipse.smarthome.core.common.osgi;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.osgi.framework.Bundle;

public class ResourceBundleClassLoaderTest {

    static URL createTmpTestPropetiesFile(Path root, String relativeFile) throws Exception {
        Path file = Paths.get(relativeFile);
        Path fileDir = file.getParent();

        new File(root.toFile(), fileDir.toString()).mkdirs();
        File _file = new File(root.toFile(), relativeFile);
        _file.createNewFile();

        return _file.toURL();
    }

    @Test
    public void testName() throws Exception {
        Path tmp = Files.createTempDirectory("tmp");
        URL hostPropertiesURL = createTmpTestPropetiesFile(tmp, "host/ESH-INF/i18n/test.properties");
        URL fragmentPropertiesURL = createTmpTestPropetiesFile(tmp, "fragment/ESH-INF/i18n/test.properties");

        Bundle bundleMock = mock(Bundle.class);
        when(bundleMock.findEntries(any(), any(), anyBoolean()))
                .thenAnswer(answer -> Collections.enumeration(Arrays.asList(hostPropertiesURL, fragmentPropertiesURL)));
        when(bundleMock.getEntry(any())).thenReturn(hostPropertiesURL);

        ResourceBundleClassLoader classloader = new ResourceBundleClassLoader(bundleMock, "/ESH-INF/i18n",
                "*.properties");

        ArrayList<URL> propertiesURLs = Collections.list(bundleMock.findEntries("/ESH-INF/i18n", "*.properties", true));

        assertEquals(2, propertiesURLs.size());
        assertTrue(propertiesURLs.contains(hostPropertiesURL));
        assertTrue(propertiesURLs.contains(fragmentPropertiesURL));

        assertEquals(hostPropertiesURL, bundleMock.getEntry(null));
        assertEquals(hostPropertiesURL, bundleMock.getEntry("always-return-hostPropertiesURL"));

        URL resource = classloader.getResource("test.properties");
        assertEquals(fragmentPropertiesURL, resource);
    }
}
