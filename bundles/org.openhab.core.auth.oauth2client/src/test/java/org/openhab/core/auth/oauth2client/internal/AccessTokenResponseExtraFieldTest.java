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
package org.openhab.core.auth.oauth2client.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * JUnit tests for {@link AccessTokenResponse}
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
class AccessTokenResponseExtraFieldTest {

    @Test
    public void testExtraFieldDeserialization() {
        Gson gson = OAuthConnector.getGson(new GsonBuilder());

        // \"created_on\":\"2026-02-26T15:10:49.965249200Z\"
        String json = "{\"access_token\":\"AccessToken\",\"expires_in\":60,\"refresh_token\":\"RefreshToken\",\"app_client_id\":\"ApplicationClientId\"}";
        AccessTokenResponse atr = gson.fromJson(json, AccessTokenResponse.class);

        if (atr != null) {
            assertEquals("AccessToken", atr.getAccessToken());
            assertEquals(60, atr.getExpiresIn());
            assertEquals("RefreshToken", atr.getRefreshToken());

            Map<String, String> extraFields = atr.getExtraFields();

            assertEquals(1, extraFields.size());
            assertTrue(extraFields.containsKey("app_client_id"));
            assertEquals("ApplicationClientId", extraFields.get("app_client_id"));
        }
    }
}
