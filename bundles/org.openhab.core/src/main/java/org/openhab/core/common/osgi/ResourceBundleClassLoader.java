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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.osgi.framework.Bundle;

/**
 * The {@link ResourceBundleClassLoader} is a user defined classloader which is
 * responsible to map files within an <i>OSGi</i> bundle to {@link URL}s. This
 * implementation only supports the method {@link #getResource(String)} for
 * mappings.
 *
 * @author Michael Grammling - Initial contribution
 * @author Martin Herbst - UTF-8 replaced by ISO-8859-1 to follow Java standards
 *
 */
public class ResourceBundleClassLoader extends ClassLoader {

    private Bundle bundle;
    private String path;
    private String filePattern;

    /**
     * Creates a new instance of this class with the specified parameters.
     *
     * @param bundle the bundle whose files should be mapped (must not be null)
     * @param path the path within the bundle which should be considered to be
     *            mapped. If null is set, all files within the bundle are
     *            considered.
     * @param filePattern the pattern for files to be considered within the specified
     *            path. If null is set, all files within the specified path are
     *            considered.
     * @throws IllegalArgumentException if the bundle is null
     */
    public ResourceBundleClassLoader(Bundle bundle, String path, String filePattern) throws IllegalArgumentException {
        if (bundle == null) {
            throw new IllegalArgumentException("The bundle must not be null!");
        }

        this.bundle = bundle;
        this.path = (path != null) ? path : "/";
        this.filePattern = (filePattern != null) ? filePattern : "*";
    }

    @Override
    public URL getResource(String name) {
        Enumeration<URL> resourceFiles = this.bundle.findEntries(this.path, this.filePattern, true);

        List<URL> allResources = new LinkedList<>();
        if (resourceFiles != null) {
            while (resourceFiles.hasMoreElements()) {
                URL resourceURL = resourceFiles.nextElement();
                String resourcePath = resourceURL.getFile();
                File resourceFile = new File(resourcePath);
                String resourceFileName = resourceFile.getName();

                if (resourceFileName.equals(name)) {
                    allResources.add(resourceURL);
                }
            }
        }

        if (allResources.isEmpty()) {
            return null;
        }

        if (allResources.size() == 1) {
            return allResources.get(0);
        }

        // handle fragment resources. return first one.
        for (URL url : allResources) {
            boolean isHostResource = bundle.getEntry(url.getPath()) != null
                    && bundle.getEntry(url.getPath()).equals(url);
            if (isHostResource) {
                continue;
            }
            return url;
        }

        return null;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        URL resourceURL = getResource(name);
        if (resourceURL != null) {
            try (InputStream resourceStream = resourceURL.openStream()) {
                if (resourceStream != null) {
                    try (Reader resourceReader = new InputStreamReader(resourceStream, StandardCharsets.ISO_8859_1)) {
                        Properties props = new Properties();
                        props.load(resourceReader);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        props.store(baos, "converted");
                        return new ByteArrayInputStream(baos.toByteArray());
                    }
                }
            } catch (IOException e) {
            }
            return super.getResourceAsStream(name);
        }
        return null;
    }
}
