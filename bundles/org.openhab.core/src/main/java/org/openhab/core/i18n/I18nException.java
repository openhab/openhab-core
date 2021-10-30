/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides an exception class for openHAB that incorporates support for internationalization
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class I18nException extends Exception {

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(I18nException.class);

    private String msgKey;
    private @Nullable Object @Nullable [] msgParams;
    private @Nullable Bundle bundle;
    private @Nullable TranslationProvider i18nProvider;
    private @Nullable Locale locale;

    /**
     *
     * @param message the exception message; use "@text/key" to reference "key" entry in the properties file
     * @param msgParams the optional arguments of the message defined in the properties file
     */
    public I18nException(String message, @Nullable Object @Nullable... msgParams) {
        this(message, null, msgParams);
    }

    /**
     *
     * @param message the exception message; use "@text/key" to reference "key" entry in the properties file
     * @param cause the cause (which is saved for later retrieval by the getCause() method). A null value is permitted,
     *            and indicates that the cause is nonexistent or unknown.
     * @param msgParams the optional arguments of the message defined in the properties file
     */
    public I18nException(String message, @Nullable Throwable cause, @Nullable Object @Nullable... msgParams) {
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
    public I18nException(Throwable cause) {
        super(cause);
        this.msgKey = "";
    }

    /**
     * Setup the internationalization support
     *
     * In case the message starts with "@text/", one of both setupI18n() methods needs to be called before using the
     * getMessage() and getLocalizedMessage() methods.
     *
     * The getLocalizedMessage() method will then consider the English message.
     *
     * @param bundle the bundle containing the i18n properties
     * @param i18nProvider the translation provider
     */
    public void setupI18n(Bundle bundle, TranslationProvider i18nProvider) {
        setupI18n(bundle, i18nProvider, Locale.ENGLISH);
    }

    /**
     * Setup the internationalization support
     *
     * In case the message starts with "@text/", one of both setupI18n() methods needs to be called before using the
     * getMessage() and getLocalizedMessage() methods.
     *
     * The getLocalizedMessage() method will then consider the language provided by the locale parameter.
     *
     * @param bundle the bundle containing the i18n properties
     * @param i18nProvider the translation provider
     * @param locale the language to use for localizing the message
     */
    public void setupI18n(Bundle bundle, TranslationProvider i18nProvider, Locale locale) {
        this.bundle = bundle;
        this.i18nProvider = i18nProvider;
        this.locale = locale;
    }

    @Override
    public @Nullable String getMessage() {
        Bundle localeBundle = this.bundle;
        TranslationProvider localI18nProvider = this.i18nProvider;
        if (msgKey.isBlank()) {
            return super.getMessage();
        } else if (localeBundle == null || localI18nProvider == null) {
            logger.warn("Internationalization support is not setup for the exception");
            return super.getMessage();
        } else {
            return localI18nProvider.getText(localeBundle, msgKey, null, Locale.ENGLISH, msgParams);
        }
    }

    @Override
    public @Nullable String getLocalizedMessage() {
        Bundle localeBundle = this.bundle;
        TranslationProvider localI18nProvider = this.i18nProvider;
        Locale localLocale = this.locale;
        if (msgKey.isBlank()) {
            return super.getMessage();
        } else if (localeBundle == null || localI18nProvider == null) {
            logger.warn("Internationalization support is not setup for the exception");
            return super.getMessage();
        } else {
            return localI18nProvider.getText(localeBundle, msgKey, null,
                    localLocale != null ? localLocale : Locale.ENGLISH, msgParams);
        }
    }

    /**
     * Provides the untranslated message
     *
     * If the message does not start with "@text/", it returns the same as the getMessage() method.
     * If the message starts with "@text/" and no optional arguments are set, it returns a string of this
     * kind: @text/key
     * If the message starts with "@text/" and optional arguments are set, it returns a string of this kind: @text/key [
     * "param1", "param2" ]
     *
     * @return the untranslated message or null if the message is undefined
     */
    public @Nullable String getUntranslatedMessage() {
        if (msgKey.isBlank()) {
            return super.getMessage();
        }
        String result = "@text/" + msgKey;
        Object @Nullable [] params = msgParams;
        if (params != null && params.length > 0) {
            result += " [";
            boolean first = true;
            for (Object param : params) {
                if (first) {
                    first = false;
                } else {
                    result += ",";
                }
                result += String.format(" \"%s\"", param == null ? "" : param.toString());
            }
            result += " ]";
        }
        return result;
    }
}
