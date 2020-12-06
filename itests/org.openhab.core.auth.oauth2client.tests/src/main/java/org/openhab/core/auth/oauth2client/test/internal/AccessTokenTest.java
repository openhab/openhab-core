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
package org.openhab.core.auth.oauth2client.test.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.security.GeneralSecurityException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.oauth2client.internal.AccessTokenDeserializer;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Verify that scope as array can fit in string
 *
 * @author Gaël L'hopital - Initial contribution
 */
public class AccessTokenTest {

    private Gson gson;
    private String RFC_COMPLIANT = "{\"access_token\":\"51c55d04187\",\"refresh_token\":\"51c55d04187\",\"scope\":\"read_station read_thermostat write_thermostat\",\"expires_in\":10800,\"expire_in\":10800}";
    private String DESERIALIZED_SCOPE = "read_station read_thermostat write_thermostat";
    private String SCOPE_AS_ARRAY_ANSWER = "{\"access_token\":\"51c55d04187\",\"refresh_token\":\"51c55d04187\",\"scope\":[\"read_station\",\"read_thermostat\",\"write_thermostat\"],\"expires_in\":10800,\"expire_in\":10800}";

    @BeforeEach
    public void setUp() {
        gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(AccessTokenResponse.class, new AccessTokenDeserializer()).create();
    }

    @Test
    public void testScopeDeserialization() throws GeneralSecurityException {
        AccessTokenResponse jsonResponse = gson.fromJson(RFC_COMPLIANT, AccessTokenResponse.class);
        assertEquals(DESERIALIZED_SCOPE, jsonResponse.getScope(), "Scope should be desirialized as expected");
        jsonResponse = gson.fromJson(SCOPE_AS_ARRAY_ANSWER, AccessTokenResponse.class);
        assertEquals(DESERIALIZED_SCOPE, jsonResponse.getScope(), "Scope should be desirialized as expected");
    }
}
