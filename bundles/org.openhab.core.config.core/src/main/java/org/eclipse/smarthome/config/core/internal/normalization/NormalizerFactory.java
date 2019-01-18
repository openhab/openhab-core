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
package org.eclipse.smarthome.config.core.internal.normalization;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;

/**
 * The {@link NormalizerFactory} can be used in order to obtain the {@link Normalizer} for any concrete
 * {@link ConfigDescriptionParameter.Type}.
 *
 * @author Simon Kaufmann - initial contribution and API.
 * @author Thomas Höfer - introduced normalizers map and added precondition check as well as some additional javadoc
 */
public final class NormalizerFactory {

    private static final Map<Type, Normalizer> NORMALIZERS;

    static {
        Map<Type, Normalizer> map = new HashMap<Type, Normalizer>(11);
        map.put(Type.BOOLEAN, new BooleanNormalizer());
        map.put(Type.TEXT, new TextNormalizer());
        map.put(Type.INTEGER, new IntNormalizer());
        map.put(Type.DECIMAL, new DecimalNormalizer());
        NORMALIZERS = Collections.unmodifiableMap(map);
    }

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

        if (configDescriptionParameter.isMultiple()) {
            ret = new ListNormalizer(ret);
        }

        return ret;
    }

}
