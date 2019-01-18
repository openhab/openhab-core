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

import java.util.ArrayList;
import java.util.List;

/**
 * The normalizer for configuration parameters allowing multiple values. It converts all collections/arrays to a
 * {@link List} and applies the underlying normalizer to each of the values inside that list.
 *
 * @author Simon Kaufmann - initial contribution and API.
 * @author Thomas Höfer - made class final and minor javadoc changes
 */
final class ListNormalizer extends AbstractNormalizer {

    private Normalizer delegate;

    ListNormalizer(Normalizer delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object doNormalize(Object value) {
        if (!isList(value)) {
            List ret = new ArrayList(1);
            ret.add(delegate.normalize(value));
            return ret;
        }
        if (isArray(value)) {
            List ret = new ArrayList(((Object[]) value).length);
            for (Object object : ((Object[]) value)) {
                ret.add(delegate.normalize(object));
            }
            return ret;
        }
        if (value instanceof List) {
            List ret = new ArrayList(((List) value).size());
            for (Object object : (List) value) {
                ret.add(delegate.normalize(object));
            }
            return ret;
        }
        if (value instanceof Iterable) {
            List ret = new ArrayList();
            for (Object object : (Iterable) value) {
                ret.add(delegate.normalize(object));
            }
            return ret;
        }
        return value;
    }

    static boolean isList(Object value) {
        return isArray(value) || value instanceof Iterable;
    }

    private static boolean isArray(Object object) {
        return object != null && object.getClass().isArray();
    }

}
