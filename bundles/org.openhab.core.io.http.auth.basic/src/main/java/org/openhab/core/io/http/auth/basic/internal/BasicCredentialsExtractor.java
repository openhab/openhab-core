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
package org.openhab.core.io.http.auth.basic.internal;

import java.security.MessageDigest;

import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.openhab.core.auth.Credentials;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.openhab.core.io.http.auth.CredentialsExtractor;
import org.osgi.service.component.annotations.Component;

/**
 * Extract user name and password from incoming request.
 *
 * @author Łukasz Dywicki - Initial contribution.
 */
@Component(property = { "context=javax.servlet.http.HttpServletRequest" })
public class BasicCredentialsExtractor implements CredentialsExtractor<HttpServletRequest> {
    private static final String UUID_STRING = UUID.randomUUID().toString();
    static MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
    static HashMap<ByteBuffer, UsernamePasswordCredentials> authCache = new HashMap<String, UsernamePasswordCredentials>();

    @Override
    public Optional<Credentials> retrieveCredentials(HttpServletRequest request) {
        String authenticationHeader = request.getHeader("Authorization");

        if (authenticationHeader == null) {
            return Optional.empty();
        }

        String[] tokens = authenticationHeader.split(" ");
        if (tokens.length == 2) {
            final String authType = tokens[0];
            if (HttpServletRequest.BASIC_AUTH.equalsIgnoreCase(authType)) {
                final String authValue = tokens[1];
                ByteBuffer authHash = ByteBuffer.wrap(msgDigest.digest((tokens[1] + UUID_STRING).getBytes()));
                
                UsernamePasswordCredentials cachedValue = BasicCredentialsExtractor.authCache.get(authHash);
                if (cachedValue != null) {
                    return Optional.of(cachedValue);
                }

                String usernameAndPassword = new String(Base64.getDecoder().decode(authValue));
                tokens = usernameAndPassword.split(":");
                if (tokens.length == 2) {
                    final String username = tokens[0];
                    final String password = tokens[1];

                    UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
                    
                    // limit auth cache to a certain size
                    if (BasicCredentialsExtractor.authCache.size() > 20) {
                        Object remName = null;
                        for (Object obj : BasicCredentialsExtractor.authCache.keySet()) {
                            remName = obj;
                            break;
                        }
                        BasicCredentialsExtractor.authCache.remove(remName);
                    }
                    
                    BasicCredentialsExtractor.authCache.put(authHash, creds);
                    return Optional.of(creds);
                }
            }
        }

        return Optional.empty();
    }
}
