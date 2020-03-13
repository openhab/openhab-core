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
package org.openhab.core.io.rest.auth.internal;

/**
 * A DTO object for an unsuccessful token endpoint response, as per RFC 6749, Section 5.2.
 *
 * {@linkplain https://tools.ietf.org/html/rfc6749#section-5.2}
 *
 * @author Yannick Schaus - initial contribution
 */
public class TokenResponseErrorDTO {
    public String error;
    public String error_description;
    public String error_uri;

    /**
     * Builds a token endpoint response for a specific error
     *
     * @param the error
     */
    public TokenResponseErrorDTO(String error) {
        this.error = error;
    }
}
