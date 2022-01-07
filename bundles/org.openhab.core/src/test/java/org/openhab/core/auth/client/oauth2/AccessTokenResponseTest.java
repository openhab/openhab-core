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
package org.openhab.core.auth.client.oauth2;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * @author Gary Tse - Initial contribution
 */
public class AccessTokenResponseTest {

    @Test
    public void testTokenExpired() {
        AccessTokenResponse token = new AccessTokenResponse();

        // token created 20 seconds ago
        LocalDateTime someTimeAgo = LocalDateTime.now().minusSeconds(20);
        token.setCreatedOn(someTimeAgo);

        // token only has a life time of 10 seconds
        token.setExpiresIn(10);

        assertTrue(token.isExpired(LocalDateTime.now(), 0), "Token should have been expired");
    }

    @Test
    public void testTokenExpiredDueToBuffer() {
        AccessTokenResponse token = new AccessTokenResponse();

        // token created 20 seconds ago
        LocalDateTime someTimeAgo = LocalDateTime.now().minusSeconds(20);
        token.setCreatedOn(someTimeAgo);

        // token only has a life time of 30 seconds
        token.setExpiresIn(30);

        assertTrue(token.isExpired(LocalDateTime.now(), 15), "Token should have been expired due to buffer");
    }

    @Test
    public void testTokenNotExpired() {
        AccessTokenResponse token = new AccessTokenResponse();
        // A token created now
        token.setCreatedOn(LocalDateTime.now());

        // token has life time of 1 minute
        token.setExpiresIn(60);

        assertFalse(token.isExpired(LocalDateTime.now(), 0), "Token should not be expired");
    }

    @Test
    public void testTokenNotExpiredEvenWithBuffer() {
        AccessTokenResponse token = new AccessTokenResponse();

        // token created 20 seconds ago
        LocalDateTime someTimeAgo = LocalDateTime.now().minusSeconds(20);
        token.setCreatedOn(someTimeAgo);

        // token has a life time of 60 seconds
        token.setExpiresIn(60);

        assertTrue(!token.isExpired(LocalDateTime.now(), 10), "Token should have been expired due to buffer");
    }
}
