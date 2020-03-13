package org.openhab.core.io.rest.auth.internal;

public class TokenResponseErrorDTO {
    String error;
    String error_description;
    String error_uri;

    public TokenResponseErrorDTO(String error) {
        this.error = error;
    }
}
