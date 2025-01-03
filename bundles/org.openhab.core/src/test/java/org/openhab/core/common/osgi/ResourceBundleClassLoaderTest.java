/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.framework.Bundle;

/**
 * @author Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class ResourceBundleClassLoaderTest {

    private @TempDir @NonNullByDefault({}) Path tempDir;

    static URL createTmpTestPropertiesFile(Path root, String relativeFile) throws Exception {
        Path filePath = Path.of(relativeFile);
        Path dirPath = filePath.getParent();

        new File(root.toFile(), dirPath.toString()).mkdirs();
        File file = new File(root.toFile(), relativeFile);
        file.createNewFile();

        return file.toURI().toURL();
    }

    @Test
    public void testName() throws Exception {
        URL hostPropertiesURL = createTmpTestPropertiesFile(tempDir, "host/OH-INF/i18n/test.properties");
        URL fragmentPropertiesURL = createTmpTestPropertiesFile(tempDir, "fragment/OH-INF/i18n/test.properties");

        Bundle bundleMock = mock(Bundle.class);
        when(bundleMock.findEntries(any(), any(), anyBoolean()))
                .thenAnswer(answer -> Collections.enumeration(List.of(hostPropertiesURL, fragmentPropertiesURL)));
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
