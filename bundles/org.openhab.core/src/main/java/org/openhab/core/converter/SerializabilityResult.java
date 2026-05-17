/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.converter;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A container that holds the result of a serializability check.
 *
 * @author Nadahar - Initial contribution
 */
@NonNullByDefault
public record SerializabilityResult<T> (T uid, boolean ok, String failureReason) {

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(" [uid=").append(uid).append(", ok=").append(ok);
        if (!ok) {
            sb.append(", failureReason=").append(failureReason);
        }
        sb.append("]");
        return sb.toString();
    }
}
