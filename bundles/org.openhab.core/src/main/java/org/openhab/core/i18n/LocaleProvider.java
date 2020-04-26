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

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The interface describe a provider for a locale.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public interface LocaleProvider {

    /**
     * Get a locale.
     *
     * The locale could be used e.g. as a fallback if there is no other one defined explicitly.
     *
     * @return a locale (non-null)
     */
    Locale getLocale();
}
