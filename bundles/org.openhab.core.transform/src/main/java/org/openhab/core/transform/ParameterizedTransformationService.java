/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A ParameterizedTransformationService extends TransformationService in order to allow
 * providing specific parameters to the transform function, instead of a single `value`.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public interface ParameterizedTransformationService extends TransformationService {
    @Nullable
    String transform(String value, Map<String, @Nullable Object> parameters) throws TransformationException;
}
