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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common base class for all normalizers, doing the specific type conversion.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Thomas Höfer - renamed normalizer interface and added javadoc
 */
abstract class AbstractNormalizer implements Normalizer {

    protected final Logger logger = LoggerFactory.getLogger(AbstractNormalizer.class);

    @Override
    public final Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String && "".equals(value)) {
            return "";
        }
        return doNormalize(value);
    }

    /**
     * Executes the concrete normalization of the given value.
     *
     * @param value the value to be normalized
     * @return the normalized value or the given value, if it was not possible to normalize it
     */
    abstract Object doNormalize(Object value);
}
