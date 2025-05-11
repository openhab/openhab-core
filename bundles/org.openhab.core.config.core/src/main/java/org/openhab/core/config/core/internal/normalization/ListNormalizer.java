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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The normalizer for configuration parameters allowing multiple values. It converts all collections/arrays to a
 * {@link List} and applies the underlying normalizer to each of the values inside that list.
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Thomas Höfer - made class final and minor javadoc changes
 */
@NonNullByDefault
final class ListNormalizer extends AbstractNormalizer {

    private final Normalizer delegate;

    ListNormalizer(Normalizer delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public Object doNormalize(Object value) {
        if (!isList(value)) {
            return Set.of(value).stream().map(delegate::normalize).toList();
        } else if (isArray(value)) {
            return Arrays.stream((Object[]) value).map(delegate::normalize).toList();
        } else if (value instanceof List list) {
            return list.stream().map(delegate::normalize).toList();
        } else if (value instanceof Iterable iterable) {
            return StreamSupport.stream(iterable.spliterator(), false).map(delegate::normalize).toList();
        }
        return value;
    }

    private static boolean isList(Object value) {
        return isArray(value) || value instanceof Iterable;
    }

    private static boolean isArray(Object object) {
        return object.getClass().isArray();
    }
}
