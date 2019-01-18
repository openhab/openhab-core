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
 * A {@link Normalizer} tries to normalize a given value according to the {@link ConfigDescriptionParameter.Type}
 * definition of a config description parameter. As an example a boolean normalizer would convert a given numeric value
 * 0 to false and a given numeric value 1 to true.
 *
 * @author Simon Kaufmann - initial contribution and API
 * @author Thomas Höfer - renamed from INormalizer and minor javadoc changes
 */
public interface Normalizer {

    /**
     * Normalizes the given object to the expected type, if possible. The expected type is defined by the
     * {@link ConfigDescriptionParameter.Type} of the corresponding config description parameter.
     *
     * @param value the object to be normalized
     * @return the well-defined type or the given object, if it was not possible to convert it
     */
    public Object normalize(Object value);

}
