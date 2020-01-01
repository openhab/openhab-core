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
package org.openhab.core.i18n;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Common class (used as key) for i18n to store a localized object in a cache.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class LocalizedKey {

    final Object key;
    final @Nullable String locale;

    public LocalizedKey(Object id, @Nullable String locale) {
        this.key = id;
        this.locale = locale;
    }

    public Object getKey() {
        return key;
    }

    public @Nullable String getLocale() {
        return locale;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + key.hashCode();
        result = prime * result + ((locale != null) ? locale.hashCode() : 0);
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
        LocalizedKey other = (LocalizedKey) obj;
        if (!Objects.equals(key, other.key)) {
            return false;
        }
        if (!Objects.equals(locale, other.locale)) {
            return false;
        }
        return true;
    }
}
