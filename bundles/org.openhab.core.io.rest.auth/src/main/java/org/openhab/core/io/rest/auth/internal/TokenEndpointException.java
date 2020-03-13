package org.openhab.core.io.rest.auth.internal;

import org.openhab.core.auth.AuthenticationException;

public class TokenEndpointException extends AuthenticationException {
    private static final long serialVersionUID = 610324537843397832L;

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

    public TokenEndpointException(ErrorType errorType) {
        super(errorType.getError());
    }

    public TokenResponseErrorDTO getErrorDTO() {
        return new TokenResponseErrorDTO(getMessage());
    }
}
