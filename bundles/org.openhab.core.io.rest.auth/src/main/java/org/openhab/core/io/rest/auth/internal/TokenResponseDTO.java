package org.openhab.core.io.rest.auth.internal;

import org.openhab.core.auth.User;

public class TokenResponseDTO {
    public String access_token;
    public String token_type;
    public Integer expires_in;
    public String refresh_token;
    public String scope;
    public String error;
    public UserDTO user;

    public TokenResponseDTO(String access_token, String token_type, Integer expires_in, String refresh_token,
            String scope, User user) {
        super();
        this.access_token = access_token;
        this.token_type = token_type;
        this.expires_in = expires_in;
        this.refresh_token = refresh_token;
        this.scope = scope;
        this.user = new UserDTO(user);
    }

    public TokenResponseDTO(String error) {
        super();
        this.error = error;
    }

}
