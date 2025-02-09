/*
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
package org.openhab.core.config.core.internal.normalization;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;

/**
 * The {@link NormalizerFactory} can be used in order to obtain the {@link Normalizer} for any concrete
 * {@link ConfigDescriptionParameter.Type}.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Thomas Höfer - introduced normalizers map and added precondition check as well as some additional javadoc
 */
@NonNullByDefault
public final class NormalizerFactory {

    private static final Normalizer BOOLEAN_NORMALIZER = new BooleanNormalizer();
    private static final Normalizer TEXT_NORMALIZER = new TextNormalizer();
    private static final Normalizer INT_NORMALIZER = new IntNormalizer();
    private static final Normalizer DECIMAL_NORMALIZER = new DecimalNormalizer();

    private NormalizerFactory() {
        // prevent instantiation
    }

    /**
     * Returns the {@link Normalizer} for the type of the given config description parameter.
     *
     * @param configDescriptionParameter the config description parameter (must not be null)
     * @return the corresponding {@link Normalizer} (not null)
     * @throws IllegalArgumentException if the given config description parameter is null
     */
    public static Normalizer getNormalizer(@Nullable ConfigDescriptionParameter configDescriptionParameter) {
        if (configDescriptionParameter == null) {
            throw new IllegalArgumentException("The config description parameter must not be null.");
        }

        Normalizer ret = getNormalizer(configDescriptionParameter.getType());
        return configDescriptionParameter.isMultiple() ? new ListNormalizer(ret) : ret;
    }

    /**
     * Returns the {@link Normalizer} for the given ConfigDescriptionParameter type.
     *
     * @param type the type
     * @return the corresponding {@link Normalizer} (not null)
     */
    public static Normalizer getNormalizer(Type type) {
        return switch (type) {
            case BOOLEAN -> BOOLEAN_NORMALIZER;
            case DECIMAL -> DECIMAL_NORMALIZER;
            case INTEGER -> INT_NORMALIZER;
            case TEXT -> TEXT_NORMALIZER;
        };
    }
}
