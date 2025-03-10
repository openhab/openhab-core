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

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * This {@link DeviceCodeResponse} is a DTO with fields that encapsulate the data from RFC-8628 device code responses.
 * See {@link AccessTokenResponse} for reference.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
public final class DeviceCodeResponse implements Serializable, Cloneable {
    /**
     * For Serializable
     */
    @Serial
    private static final long serialVersionUID = 4261783375996959200L;

    private String deviceCode;
    private long expiresIn;
    private long interval;
    private String userCode;
    private String verificationUri;
    private String verificationUriComplete;
    private Instant createdOn;

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("not possible", e);
        }
    }

    public Instant getCreatedOn() {
        return createdOn;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public long getInterval() {
        return interval;
    }

    public String getUserCode() {
        return userCode;
    }

    public String getVerificationUri() {
        return verificationUri;
    }

    public String getVerificationUriComplete() {
        return verificationUriComplete;
    }

    /**
     * Calculate if the device code response is expired against the given time.
     * It also returns true even if the response is not initialized (i.e. newly created).
     *
     * @param givenTime To calculate if the response is expired against the givenTime.
     * @param tokenExpiresInBuffer A positive integer in seconds to act as additional buffer to the calculation.
     *            This causes the response to expire earlier then the stated expiry-time given.
     * @return true if object is not-initialized, or expired, or expired early due to buffer.
     */
    public boolean isExpired(Instant givenTime, int tokenExpiresInBuffer) {
        return createdOn == null
                || createdOn.plusSeconds(expiresIn).minusSeconds(tokenExpiresInBuffer).isBefore(givenTime);
    }

    public void setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public void setVerificationUri(String verificationUri) {
        this.verificationUri = verificationUri;
    }

    public void setVerificationUriComplete(String verificationUriComplete) {
        this.verificationUriComplete = verificationUriComplete;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DeviceCodeResponse other = (DeviceCodeResponse) obj;
        return Objects.equals(createdOn, other.createdOn) && Objects.equals(deviceCode, other.deviceCode)
                && expiresIn == other.expiresIn && interval == other.interval
                && Objects.equals(userCode, other.userCode) && Objects.equals(verificationUri, other.verificationUri)
                && Objects.equals(verificationUriComplete, other.verificationUriComplete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdOn, deviceCode, expiresIn, interval, userCode, verificationUri,
                verificationUriComplete);
    }

    @Override
    public String toString() {
        return "DeviceCodeResponse [deviceCode=" + deviceCode + ", expiresIn=" + expiresIn + ", interval=" + interval
                + ", userCode=" + userCode + ", verificationUri=" + verificationUri + ", verificationUriComplete="
                + verificationUriComplete + ", createdOn=" + createdOn + "]";
    }
}
