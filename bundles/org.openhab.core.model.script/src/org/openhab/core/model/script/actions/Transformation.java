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
package org.openhab.core.model.script.actions;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link Transformation} is a wrapper for the {@link org.openhab.core.transform.actions.Transformation} class to
 * allow DSL rules to properly use the {@link TransformationException}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class Transformation {

    public static @Nullable String transform(String type, String function, String value) {
        return org.openhab.core.transform.actions.Transformation.transform(type, function, value);
    }

    public static @Nullable String transformRaw(String type, String function, String value)
            throws TransformationException {
        try {
            return org.openhab.core.transform.actions.Transformation.transformRaw(type, function, value);
        } catch (org.openhab.core.transform.TransformationException e) {
            throw new TransformationException(e.getMessage(), e.getCause());
        }
    }
}
