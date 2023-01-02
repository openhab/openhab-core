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
package org.openhab.core.transform;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Registry;

/**
 * The {@link TransformationRegistry} is the interface for the transformation registry
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public interface TransformationRegistry extends Registry<Transformation, String> {
    Pattern CONFIG_UID_PATTERN = Pattern.compile("config:(?<type>\\w+):(?<name>\\w+)(:(?<language>\\w+))?");

    /**
     * Get a localized version of the transformation for a given UID
     *
     * @param uid the configuration UID
     * @param locale a locale (system locale is used if <code>null</code>)
     * @return the requested {@link Transformation} (or <code>null</code> if not found).
     */
    @Nullable
    Transformation get(String uid, @Nullable Locale locale);

    /**
     * Get all transformations which match the given types
     *
     * @param types a {@link Collection} of configuration types
     * @return a {@link Collection} of {@link Transformation}s
     */
    Collection<Transformation> getTransformations(Collection<String> types);
}
