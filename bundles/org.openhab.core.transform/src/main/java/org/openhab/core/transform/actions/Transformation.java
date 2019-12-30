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
package org.openhab.core.transform.actions;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.transform.TransformationException;
import org.openhab.core.transform.TransformationHelper;
import org.openhab.core.transform.TransformationService;
import org.openhab.core.transform.internal.TransformationActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class holds static "action" methods that can be used from within rules to execute
 * transformations.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class Transformation {

    private static @Nullable String trans(String type, String function, String value) throws TransformationException {
        String result;
        TransformationService service = TransformationHelper
                .getTransformationService(TransformationActivator.getContext(), type);
        if (service != null) {
            result = service.transform(function, value);
        } else {
            throw new TransformationException("No transformation service '" + type + "' could be found.");
        }
        return result;
    }

    /**
     * Applies a transformation of a given type with some function to a value.
     *
     * @param type the transformation type, e.g. REGEX or MAP
     * @param function the function to call, this value depends on the transformation type
     * @param value the value to apply the transformation to
     * @return the transformed value or the original one, if there was no service registered for the
     *         given type or a transformation exception occurred.
     */
    public static @Nullable String transform(String type, String function, String value) {
        Logger logger = LoggerFactory.getLogger(Transformation.class);
        String result;
        try {
            result = trans(type, function, value);
        } catch (TransformationException e) {
            logger.debug("Error executing the transformation '{}': {}", type, e.getMessage());
            result = value;
        }
        return result;
    }

    /**
     * Applies a transformation of a given type with some function to a value.
     *
     * @param type the transformation type, e.g. REGEX or MAP
     * @param function the function to call, this value depends on the transformation type
     * @param value the value to apply the transformation to
     * @return the transformed value
     * @throws TransformationException, if there was no service registered for the
     *             given type or a transformation exception occurred
     */
    public static @Nullable String transformRaw(String type, String function, String value)
            throws TransformationException {
        return trans(type, function, value);
    }

}
