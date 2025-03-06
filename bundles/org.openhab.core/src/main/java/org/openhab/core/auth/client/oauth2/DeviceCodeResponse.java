/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.io.Serializable;
import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This {@link DeviceCodeResponse} wraps an {@link AccessTokenResponse} and re-purposes some
 * of its fields to encapsulate the data from RFC-8628 device code responses. This allows us
 * to use the existing {@link AccessTokenResponse} storage mechanism to store and load the
 * {@link DeviceCodeResponse} data too.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public final class DeviceCodeResponse implements Serializable, Cloneable {
    private static final long serialVersionUID = 4261783375996959200L;

    /**
     * Create a {@link DeviceCodeResponse} from the given {@link AccessTokenResponse}.
     * Returns null if the AccessTokenResponse is null.
     */
    public static @Nullable DeviceCodeResponse createFromAccessTokenResponse(@Nullable AccessTokenResponse atr) {
        return atr != null ? new DeviceCodeResponse(atr) : null;
    }

    /**
     * Get the inner {@link AccessTokenResponse} from the given {@link DeviceCodeResponse}.
     * Returns null if the DeviceCodeResponse is null.
     */
    public static @Nullable AccessTokenResponse getAccessTokenResponse(@Nullable DeviceCodeResponse dcr) {
        return dcr != null ? dcr.getAccessTokenResponse() : null;
    }

    private final AccessTokenResponse atr;

    public DeviceCodeResponse() {
        atr = new AccessTokenResponse();
        atr.setCreatedOn(Instant.now());
    }

    public DeviceCodeResponse(AccessTokenResponse atr) {
        this.atr = atr;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof DeviceCodeResponse that) {
            return this.atr.equals(that.atr);
        }
        return false;
    }

    public AccessTokenResponse getAccessTokenResponse() {
        return atr;
    }

    public String getDeviceCode() {
        return atr.getAccessToken();
    }

    public long getExpiresIn() {
        return atr.getExpiresIn();
    }

    public int getInterval() {
        return Integer.parseInt(atr.getState());
    }

    public String getUserAuthenticationUri() {
        return atr.getRefreshToken();
    }

    public boolean isExpired(Instant givenTime, int tokenExpiresInBuffer) {
        return atr.isExpired(givenTime, tokenExpiresInBuffer);
    }

    public void setDeviceCode(String deviceCode) {
        atr.setAccessToken(deviceCode);
    }

    public void setExpiresIn(int expiresIn) {
        atr.setExpiresIn(expiresIn);
    }

    public void setInterval(int interval) {
        atr.setState(String.valueOf(interval));
    }

    public void setUserAuthenticationUri(String userUri) {
        atr.setRefreshToken(userUri);
    }

    @Override
    public String toString() {
        return "DeviceCodeResponse [deviceCode=" + getDeviceCode() + ", userAuthenticationUri="
                + getUserAuthenticationUri() + ", interval=" + getInterval() + ", expiresIn=" + getExpiresIn()
                + ", createdOn=" + atr.getCreatedOn() + "]";
    }
}
