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
 * The {@link TransformationException} is a clone of {@link org.openhab.core.transform.TransformationException} to
 * make it available to DSL rules
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TransformationException extends Exception {

    private static final long serialVersionUID = -1L;

    public TransformationException(@Nullable String message) {
        super(message);
    }

    public TransformationException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
