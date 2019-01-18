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
package org.eclipse.smarthome.core.transform;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A TransformationException is thrown when any step of a transformation went
 * wrong. The originating exception should be attached to increase traceability.
 *
 * @author Thomas.Eichstaedt-Engelen
 */
@NonNullByDefault
public class TransformationException extends Exception {

    /** generated serial Version UID */
    private static final long serialVersionUID = -535237375844795145L;

    public TransformationException(String message) {
        super(message);
    }

    public TransformationException(String message, Throwable cause) {
        super(message, cause);
    }

}
