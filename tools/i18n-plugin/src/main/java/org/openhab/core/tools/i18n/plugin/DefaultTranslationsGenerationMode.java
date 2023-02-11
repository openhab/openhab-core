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
package org.openhab.core.tools.i18n.plugin;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Enumerates all the different modes for generating default translations.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public enum DefaultTranslationsGenerationMode {
    /**
     * Creates XML based default translations files only when these do not yet exist.
     */
    ADD_MISSING_FILES,

    /**
     * Same as {@link #ADD_MISSING_FILES} but also adds missing translations to existing default translations files.
     */
    ADD_MISSING_TRANSLATIONS,

    /**
     * Removes existing default translation files and regenerates them based on the XML based texts only.
     */
    REGENERATE_FILES
}
