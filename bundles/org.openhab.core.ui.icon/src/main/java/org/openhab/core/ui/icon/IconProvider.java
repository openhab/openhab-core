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

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.ui.icon.IconSet.Format;

/**
 * An icon provider can provide {@link InputStream}s for icons.
 * The source of the images can depend on the provider implementation.
 * The byte stream represents a PNG or SVG image, depending on the format.
 *
 * The icon category corresponds to the list of available channel categories.
 *
 * In order to provide means to user interfaces to know, what kind of icon sets are available in the system (and offered
 * by some icon provider), the provider can additionally provide a set of {@link IconSet}s.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface IconProvider {

    /**
     * Returns a set of icon set definitions (meta-data) which this provider defines.
     *
     * @return a set of icon sets in the default locale
     */
    Set<IconSet> getIconSets();

    /**
     * Returns a set of localized icon set definitions (meta-data) which this provider defines.
     *
     * @param locale the locale to use for the results
     * @return a set of icon sets in the requested locale
     */
    Set<IconSet> getIconSets(@Nullable Locale locale);

    /**
     * determines whether this provider can deliver an icon for a given name
     *
     * @param category the icon category
     * @param iconSetId the id of the icon set for which the icon is requested
     * @param format the format of the stream (usually either png or svg)
     * @return a non-negative Integer value defining the priority (higher is more important) or <code>null</code>, if
     *         this provider cannot deliver an icon. Default for full icon sets should be 0, so that others have the
     *         chance to override icons.
     */
    @Nullable
    Integer hasIcon(String category, String iconSetId, Format format);

    /**
     * retrieves the {@link InputStream} of an icon
     *
     * @param iconName the icon name
     * @param iconSetId the id of the icon set for which the icon is requested
     * @param the string representation of the state (for the case that the icon differs for different states)
     * @param format the format of the stream (usually either png or svg)
     * @return a byte stream of the icon in the given format or null, if no icon exists
     */
    @Nullable
    InputStream getIcon(String category, String iconSetId, @Nullable String state, Format format);
}
