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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.AuthenticationException;

/**
 * An exception when the token endpoint encounters an error and must return an error response, according to RFC 6749
 * Section 5.2.
 *
 * {@linkplain https://tools.ietf.org/html/rfc6749#section-5.2}
 *
 * @author Yannick Schaus - initial contribution
 */
@NonNullByDefault
public class TokenEndpointException extends AuthenticationException {
    private static final long serialVersionUID = 610324537843397832L;

    /**
     * Represents the error types which are supported in token issuing error responses.
     *
     * @author Yannick Schaus - initial contribution
     */
    public enum ErrorType {
        INVALID_REQUEST("invalid_request"),
        INVALID_GRANT("invalid_grant"),
        INVALID_CLIENT("invalid_client"),
        INVALID_SCOPE("invalid_scope"),
        UNAUTHORIZED_CLIENT("unauthorized_client"),
        UNSUPPORTED_GRANT_TYPE("unsupported_grant_type");

        private String error;

        ErrorType(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }

    /**
     * Constructs a {@link TokenEndpointException} for the specified error type.
     *
     * @param errorType the error type
     */
    public TokenEndpointException(ErrorType errorType) {
        super(errorType.getError());
    }

    /**
     * Gets a {@link TokenResponseErrorDTO} representing the exception
     *
     * @return the error response object
     */
    public TokenResponseErrorDTO getErrorDTO() {
        return new TokenResponseErrorDTO(getMessage());
    }
}
