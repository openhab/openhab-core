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
package org.openhab.core.transform;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A TransformationProcessor transforms a given input and returns the transformed
 * result. Transformations could make sense in various situations, for example:
 * <ul>
 * <li>extract certain informations from a weather forecast website</li>
 * <li>extract the status of your TV which provides it's status on a webpage</li>
 * <li>postprocess the output from a serial device to be human readable</li>
 * </ul>
 * One could provide his own processors by providing a new implementation of this
 * Interface.
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface TransformationService {

    public static final String SERVICE_PROPERTY_NAME = "openhab.transform";
    public static final String TRANSFORM_FOLDER_NAME = "transform";
    public static final String TRANSFORM_PROFILE_SCOPE = "transform";

    /**
     * Transforms the input <code>source</code> by means of the given <code>function</code> and returns the transformed
     * output. The transformation may return <code>null</code> to express its operation resulted in a <code>null</code>
     * output. In case of any error a {@link TransformationException} should be thrown.
     *
     * @param function the function to be used to transform the input
     * @param source the input to be transformed
     * @return the transformed result or <code>null</code> if the
     *         transformation's output is <code>null</code>.
     * @throws TransformationException if any error occurs
     */
    @Nullable
    String transform(String function, String source) throws TransformationException;
}
