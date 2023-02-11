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
package org.openhab.core.i18n;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.Bundle;

/**
 * Provides an exception class for openHAB that incorporates support for internationalization
 *
 * @author Laurent Garnier - Initial contribution
 */
@SuppressWarnings("serial")
@NonNullByDefault
public abstract class AbstractI18nException extends RuntimeException {

    private String msgKey;
    private @Nullable Object @Nullable [] msgParams;

    /**
     *
     * @param message the exception message; use "@text/key" to reference "key" entry in the properties file
     * @param msgParams the optional arguments of the message defined in the properties file
     */
    public AbstractI18nException(String message, @Nullable Object @Nullable... msgParams) {
        this(message, null, msgParams);
    }

    /**
     *
     * @param message the exception message; use "@text/key" to reference "key" entry in the properties file
     * @param cause the cause (which is saved for later retrieval by the getCause() method). A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.
     * @param msgParams the optional arguments of the message defined in the properties file
     */
    public AbstractI18nException(String message, @Nullable Throwable cause, @Nullable Object @Nullable... msgParams) {
        super(I18nUtil.isConstant(message) ? null : message, cause);
        if (I18nUtil.isConstant(message)) {
            this.msgKey = I18nUtil.stripConstant(message);
            this.msgParams = msgParams;
        } else {
            this.msgKey = "";
        }
    }

    /**
     *
     * @param cause the cause (which is saved for later retrieval by the getCause() method).
     */
    public AbstractI18nException(Throwable cause) {
        super(cause);
        this.msgKey = "";
    }

    /**
     * Returns the detail message string of this exception.
     *
     * In case the message starts with "@text/" and the parameters bundle and i18nProvider are not null, the translation
     * provider is used to look for the message key in the English properties file of the provided bundle.
     *
     * @param bundle the bundle containing the i18n properties
     * @param i18nProvider the translation provider
     * @return the detail message string of this exception instance (which may be null)
     */
    public @Nullable String getMessage(@Nullable Bundle bundle, @Nullable TranslationProvider i18nProvider) {
        return getLocalizedMessage(bundle, i18nProvider, Locale.ENGLISH);
    }

    /**
     * Returns a localized description of this exception.
     *
     * In case the message starts with "@text/" and the parameters bundle and i18nProvider are not null, the translation
     * provider is used to look for the message key in the properties file of the provided bundle containing strings for
     * the requested language.
     * English language is considered if no language is provided.
     *
     * @param bundle the bundle containing the i18n properties
     * @param i18nProvider the translation provider
     * @param locale the language to use for localizing the message
     * @return the localized description of this exception instance (which may be null)
     */
    public @Nullable String getLocalizedMessage(@Nullable Bundle bundle, @Nullable TranslationProvider i18nProvider,
            @Nullable Locale locale) {
        if (msgKey.isBlank() || bundle == null || i18nProvider == null) {
            return super.getMessage();
        } else {
            return i18nProvider.getText(bundle, msgKey, null, locale != null ? locale : Locale.ENGLISH, msgParams);
        }
    }

    /**
     * Provides the raw message
     *
     * If the message does not start with "@text/", it returns the same as the getMessage() method.
     * If the message starts with "@text/" and no optional arguments are set, it returns a string of this
     * kind: @text/key
     * If the message starts with "@text/" and optional arguments are set, it returns a string of this kind: @text/key [
     * "param1", "param2" ]
     *
     * @return the raw message or null if the message is undefined
     */
    public @Nullable String getRawMessage() {
        if (msgKey.isBlank()) {
            return super.getMessage();
        }
        String result = "@text/" + msgKey;
        Object @Nullable [] params = msgParams;
        if (params != null && params.length > 0) {
            result += Stream.of(params).map(param -> String.format("\"%s\"", param == null ? "" : param.toString()))
                    .collect(Collectors.joining(", ", " [ ", " ]"));
        }
        return result;
    }
}
