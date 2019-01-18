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
package org.eclipse.smarthome.io.rest;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Provides helper method for working with locales.
 *
 * @author Lyubomir Papazov - initial contribution
 *
 */
@NonNullByDefault
public interface LocaleService {

    /**
     * Returns the locale in respect to the given "Accept-Language" HTTP header.
     *
     * @param language value of the "Accept-Language" HTTP header (can be null).
     * @return Locale for the "Accept-Language" HTTP header or default locale if
     *         header is not set or can not be parsed.
     */
    Locale getLocale(@Nullable String acceptLanguageHttpHeader);
}
