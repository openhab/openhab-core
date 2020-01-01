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
package org.openhab.core.library.types;

import java.util.Arrays;
import java.util.Base64;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

/**
 * This type can be used for all binary data such as images, documents, sounds etc.
 * Note that it is NOT adequate for any kind of streams, but only for fixed-size data.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - add MIME type
 */
@NonNullByDefault
public class RawType implements PrimitiveType, State {

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    protected byte[] bytes;
    protected String mimeType;

    @Deprecated
    public RawType() {
        this(new byte[0], DEFAULT_MIME_TYPE);
    }

    @Deprecated
    public RawType(byte[] bytes) {
        this(bytes, DEFAULT_MIME_TYPE);
    }

    public RawType(byte[] bytes, String mimeType) {
        if (mimeType.isEmpty()) {
            throw new IllegalArgumentException("mimeType argument must not be blank");
        }
        this.bytes = bytes;
        this.mimeType = mimeType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public static RawType valueOf(String value) {
        int idx, idx2;
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Argument must not be blank");
        } else if (!value.startsWith("data:") || ((idx = value.indexOf(",")) < 0)) {
            throw new IllegalArgumentException("Invalid data URI syntax for argument " + value);
        } else if ((idx2 = value.indexOf(";")) <= 5) {
            throw new IllegalArgumentException("Missing MIME type in argument " + value);
        }
        return new RawType(Base64.getDecoder().decode(value.substring(idx + 1)), value.substring(5, idx2));
    }

    @Override
    public String toString() {
        return String.format("raw type (%s): %d bytes", mimeType, bytes.length);
    }

    @Override
    public String toFullString() {
        return String.format("data:%s;base64,%s", mimeType, Base64.getEncoder().encodeToString(bytes));
    }

    @Override
    public String format(String pattern) {
        return toFullString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(bytes);
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        RawType other = (RawType) obj;
        if (!mimeType.equals(other.mimeType)) {
            return false;
        }
        if (!Arrays.equals(bytes, other.bytes)) {
            return false;
        }
        return true;
    }

}
