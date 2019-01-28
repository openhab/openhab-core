/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.i18n;

import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 *
 * @author Denis Nobel - Initial contribution
 */
@NonNullByDefault
public class I18nUtil {

    /** The 'text' pattern (prefix) which marks constants. */
    private static final String CONSTANT_PATTERN = "@text/";

    public static boolean isConstant(@Nullable String key) {
        return key != null && key.startsWith(CONSTANT_PATTERN);
    }

    public static String stripConstant(String key) {
        return key.replace(CONSTANT_PATTERN, "");
    }

    /**
     * If key is a constant strip the constant part, otherwise use the supplier provided string.
     *
     * @param key the key
     * @param supplier the supplier that return value is used if key is identified as a constant
     * @return the key with the stripped constant marker or the supplier provided key if it is not identified as a
     *         constant
     */
    public static String stripConstantOr(final @Nullable String key, Supplier<String> supplier) {
        if (key != null && key.startsWith(CONSTANT_PATTERN)) {
            return stripConstant(key);
        } else {
            return supplier.get();
        }
    }

}
