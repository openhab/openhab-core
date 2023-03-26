/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.storage.VolatileStorage;

/**
 * The {@link OAuthStoreHandlerTest} contains tests for
 * {@link org.openhab.core.auth.oauth2client.OAuthStoreHandlerImpl}
 *
 * @author Jacob Laursen - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class OAuthStoreHandlerTest {
    private @Mock @NonNullByDefault({}) StorageService storageService;

    private static final String STORE_NAME = "StorageHandler.For.OAuthClientService";
    private @NonNullByDefault({}) Storage<String> storage;
    private @NonNullByDefault({}) OAuthStoreHandlerImpl storeHandler;

    @BeforeEach
    void initialize() throws IOException {
        storage = new VolatileStorage<>();
        Mockito.doReturn(storage).when(storageService).getStorage(STORE_NAME);
        storeHandler = new OAuthStoreHandlerImpl(storageService);
    }

    @Test
    void loadAccessTokenResponseWhenCreatedOnIsLocalDateTime() throws GeneralSecurityException {
        final String handle = "test";
        final String createdOn = "2022-08-14T21:21:05.568991";
        final Instant expected = LocalDateTime.parse(createdOn).atZone(ZoneId.systemDefault()).toInstant();

        storage.put(StorageRecordType.ACCESS_TOKEN_RESPONSE.getKey(handle), getJsonforCreatedOn(createdOn));
        AccessTokenResponse response = storeHandler.loadAccessTokenResponse(handle);

        assertThat(response, is(notNullValue()));
        if (response != null) {
            assertThat(response.getCreatedOn(), is(expected));
        }
    }

    @Test
    void loadAccessTokenResponseWhenCreatedOnIsInstant() throws GeneralSecurityException {
        final String handle = "test";
        final String createdOn = "2022-08-14T19:21:05.568991Z";
        final Instant expected = Instant.parse(createdOn);

        storage.put(StorageRecordType.ACCESS_TOKEN_RESPONSE.getKey(handle), getJsonforCreatedOn(createdOn));
        AccessTokenResponse response = storeHandler.loadAccessTokenResponse(handle);

        assertThat(response, is(notNullValue()));
        if (response != null) {
            assertThat(response.getCreatedOn(), is(expected));
        }
    }

    @Test
    void savePersistedParamsShouldNotThrow() {
        final String handle = "test";

        storeHandler.savePersistedParams(handle, new PersistedParams());
    }

    private String getJsonforCreatedOn(String createdOn) {
        return "{\"accessToken\": \"x\", \"tokenType\": \"Bearer\", \"expiresIn\": 2592000, \"refreshToken\": \"x\", \"createdOn\": \""
                + createdOn + "\"}";
    }
}
