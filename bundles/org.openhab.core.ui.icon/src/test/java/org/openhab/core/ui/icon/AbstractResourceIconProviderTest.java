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
package org.openhab.core.ui.icon;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.ui.icon.IconSet.Format;

/**
 * Tests for {@link AbstractResourceIconProvider}.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class AbstractResourceIconProviderTest {

    private @NonNullByDefault({}) IconProvider provider;

    private @Mock @NonNullByDefault({}) TranslationProvider i18nProviderMock;

    @BeforeEach
    public void setUp() {
        provider = new AbstractResourceIconProvider(i18nProviderMock) {
            @Override
            protected @Nullable InputStream getResource(String iconset, String resourceName) {
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
            public Set<IconSet> getIconSets(@Nullable Locale locale) {
                return Set.of();
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
            assertThat(new String(is.readAllBytes(), StandardCharsets.UTF_8), is("x-30.png"));
        }
    }

    @Test
    public void testWithStringTypeState() throws IOException {
        try (InputStream is = provider.getIcon("x", "classic", "y z", Format.PNG)) {
            assertThat(new String(is.readAllBytes(), StandardCharsets.UTF_8), is("x-y z.png"));
        }
    }
}
