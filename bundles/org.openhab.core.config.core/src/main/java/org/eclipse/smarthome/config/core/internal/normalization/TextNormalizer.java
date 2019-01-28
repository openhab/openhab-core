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

import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;

/**
 * The normalizer for the {@link ConfigDescriptionParameter.Type#TEXT}. It basically ensures that the given value will
 * turned into its {@link String} representation.
 *
 * @author Simon Kaufmann - initial contribution and API.
 * @author Thomas Höfer - made class final and minor javadoc changes
 */
final class TextNormalizer extends AbstractNormalizer {

    @Override
    public Object doNormalize(Object value) {
        if (value == null) {
            return value;
        }
        if (value instanceof String) {
            return value;
        }
        return value.toString();
    }

}
