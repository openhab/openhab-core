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
package org.openhab.core.config.core.internal.normalization;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;

/**
 * The {@link NormalizerFactory} can be used in order to obtain the {@link Normalizer} for any concrete
 * {@link ConfigDescriptionParameter.Type}.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Thomas Höfer - introduced normalizers map and added precondition check as well as some additional javadoc
 */
public final class NormalizerFactory {

    private static final Map<Type, Normalizer> NORMALIZERS = Collections.unmodifiableMap(Stream
            .of(new SimpleEntry<>(Type.BOOLEAN, new BooleanNormalizer()),
                    new SimpleEntry<>(Type.TEXT, new TextNormalizer()),
                    new SimpleEntry<>(Type.INTEGER, new IntNormalizer()),
                    new SimpleEntry<>(Type.DECIMAL, new DecimalNormalizer()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));

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
    public static Normalizer getNormalizer(ConfigDescriptionParameter configDescriptionParameter) {
        if (configDescriptionParameter == null) {
            throw new IllegalArgumentException("The config description parameter must not be null.");
        }

        Normalizer ret = NORMALIZERS.get(configDescriptionParameter.getType());
        return configDescriptionParameter.isMultiple() ? new ListNormalizer(ret) : ret;
    }
}
