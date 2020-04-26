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
package org.openhab.core.ui.icon;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.ui.icon.IconSet.Format;

/**
 * Tests for {@link AbstractResourceIconProvider}.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class AbstractResourceIconProviderTest {

    private IconProvider provider;

    @Before
    public void setUp() {
        provider = new AbstractResourceIconProvider() {
            @Override
            protected InputStream getResource(String iconset, String resourceName) {
                switch (resourceName) {
                    case "x-30.png":
                        return new ByteArrayInputStream("x-30.png".getBytes());
                    case "x-y z.png":
                        return new ByteArrayInputStream("x-y z.png".getBytes());
                    default:
                        return null;
                }
            }

            private String substringAfterLast(String str, String separator) {
                int index = str.lastIndexOf(separator);
                return index == -1 || index == str.length() - separator.length() ? ""
                        : str.substring(index + separator.length());
            }

            private String substringBeforeLast(String str, String separator) {
                int index = str.lastIndexOf(separator);
                return index == -1 ? str : str.substring(0, index);
            }

            @Override
            protected boolean hasResource(String iconset, String resourceName) {
                String state = substringAfterLast(resourceName, "-");
                state = substringBeforeLast(state, ".");
                return "30".equals(state) || "y z".equals(state);
            };

            @Override
            public Set<IconSet> getIconSets(Locale locale) {
                return Collections.emptySet();
            };

            @Override
            public Integer getPriority() {
                return 0;
            };
        };
    }

    @Test
    public void testScanningForState() throws IOException {
        try (InputStream is = provider.getIcon("x", "classic", "34", Format.PNG)) {
            assertNotNull(is);
        }

        try (InputStream is = provider.getIcon("x", "classic", "25", Format.PNG)) {
            assertNull(is);
        }
    }

    @Test
    public void testWithQuantityTypeState() throws IOException {
        try (InputStream is = provider.getIcon("x", "classic", "34 °C", Format.PNG)) {
            assertThat(IOUtils.toString(is), is("x-30.png"));
        }
    }

    @Test
    public void testWithStringTypeState() throws IOException {
        try (InputStream is = provider.getIcon("x", "classic", "y z", Format.PNG)) {
            assertThat(IOUtils.toString(is), is("x-y z.png"));
        }
    }
}
