/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.voice.text;

import java.util.Locale;

/**
 * This exception is thrown when the particular language is not supported by a {@link Skill} - for instance, if it has
 * no training data for that language.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class UnsupportedLanguageException extends Exception {

    private Locale language;

    private static final long serialVersionUID = -7147837667959343830L;

    /**
     * Constructor for a particular language
     *
     * @param language the language (ISO-639 code)
     */
    public UnsupportedLanguageException(String language) {
        this.language = Locale.forLanguageTag(language);
    }

    public UnsupportedLanguageException(Locale locale) {
        this.language = locale;
    }

    @Override
    public String getMessage() {
        return String.format("Unsupported language: %s", language == null ? "null" : language.toString());
    }
}
