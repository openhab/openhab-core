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
package org.openhab.core.common.osgi;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * @author Simon Kaufmann - Initial contribution
 */
public class ResourceBundleClassLoaderTest {

    static URL createTmpTestPropetiesFile(Path root, String relativeFile) throws Exception {
        Path filePath = Paths.get(relativeFile);
        Path dirPath = filePath.getParent();

        new File(root.toFile(), dirPath.toString()).mkdirs();
        File file = new File(root.toFile(), relativeFile);
        file.createNewFile();

        return file.toURL();
    }

    @Test
    public void testName() throws Exception {
        Path tmp = Files.createTempDirectory("tmp");
        URL hostPropertiesURL = createTmpTestPropetiesFile(tmp, "host/OH-INF/i18n/test.properties");
        URL fragmentPropertiesURL = createTmpTestPropetiesFile(tmp, "fragment/OH-INF/i18n/test.properties");

        Bundle bundleMock = mock(Bundle.class);
        when(bundleMock.findEntries(any(), any(), anyBoolean()))
                .thenAnswer(answer -> Collections.enumeration(Arrays.asList(hostPropertiesURL, fragmentPropertiesURL)));
        when(bundleMock.getEntry(any())).thenReturn(hostPropertiesURL);

        ResourceBundleClassLoader classloader = new ResourceBundleClassLoader(bundleMock, "/OH-INF/i18n",
                "*.properties");

        List<URL> propertiesURLs = Collections.list(bundleMock.findEntries("/OH-INF/i18n", "*.properties", true));

        assertEquals(2, propertiesURLs.size());
        assertTrue(propertiesURLs.contains(hostPropertiesURL));
        assertTrue(propertiesURLs.contains(fragmentPropertiesURL));

        assertEquals(hostPropertiesURL, bundleMock.getEntry(null));
        assertEquals(hostPropertiesURL, bundleMock.getEntry("always-return-hostPropertiesURL"));

        URL resource = classloader.getResource("test.properties");
        assertEquals(fragmentPropertiesURL, resource);
    }
}
