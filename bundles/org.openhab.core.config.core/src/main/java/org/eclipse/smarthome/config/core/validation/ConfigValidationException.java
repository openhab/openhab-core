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
package org.eclipse.smarthome.config.core.validation;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A runtime exception to be thrown if given {@link Configuration} parameters do not match their declaration in the
 * {@link ConfigDescription}.
 *
 * @author Thomas Höfer - Initial contribution
 */
public final class ConfigValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(ConfigValidationException.class);
    private final Bundle bundle;
    private final Collection<ConfigValidationMessage> configValidationMessages;
    private final TranslationProvider translationProvider;

    /**
     * Creates a new {@link ConfigValidationException} for the given {@link ConfigValidationMessage}s. It
     * requires the bundle from which this exception is thrown in order to be able to provide internationalized
     * messages.
     *
     * @param bundle the bundle from which this exception is thrown
     * @param configValidationMessages the configuration description validation messages
     * @throws NullPointException if given bundle or configuration description validation messages are null
     */
    public ConfigValidationException(Bundle bundle, TranslationProvider translationProvider,
            Collection<ConfigValidationMessage> configValidationMessages) {
        Objects.requireNonNull(bundle, "Bundle must not be null");
        Objects.requireNonNull(configValidationMessages, "Config validation messages must not be null");
        this.bundle = bundle;
        this.translationProvider = translationProvider;
        this.configValidationMessages = configValidationMessages;
    }

    /**
     * Retrieves the default validation messages (cp. {@link ConfigValidationMessage#defaultMessage}) for this
     * exception.
     *
     * @return an immutable map of validation messages having affected configuration parameter name as key and the
     *         default message as value
     */
    public Map<String, String> getValidationMessages() {
        Map<String, String> ret = new HashMap<>();
        for (ConfigValidationMessage configValidationMessage : configValidationMessages) {
            ret.put(configValidationMessage.parameterName,
                    MessageFormat.format(configValidationMessage.defaultMessage, configValidationMessage.content));
        }
        return Collections.unmodifiableMap(ret);
    }

    /**
     * Retrieves the internationalized validation messages for this exception. If there is no text found to be
     * internationalized then the default message is delivered.
     * <p>
     * If there is no TranslationProvider available then this operation will return the default validation messages by
     * using {@link ConfigValidationException#getValidationMessages()}.
     *
     * @param locale the locale to be used; if null then the default locale will be used
     * @return an immutable map of internationalized validation messages having affected configuration parameter name as
     *         key and the internationalized message as value (in case of there was no text found to be
     *         internationalized then the default message (cp. {@link ConfigValidationMessage#defaultMessage}) is
     *         delivered)
     */
    public Map<String, String> getValidationMessages(Locale locale) {
        Map<String, String> ret = new HashMap<>();
        for (ConfigValidationMessage configValidationMessage : configValidationMessages) {
            if (translationProvider == null) {
                logger.warn(
                        "TranslationProvider is not available. Will provide default validation message for parameter '{}'.",
                        configValidationMessage.parameterName);
                ret.put(configValidationMessage.parameterName,
                        MessageFormat.format(configValidationMessage.defaultMessage, configValidationMessage.content));
            } else {
                String text = translationProvider.getText(bundle, configValidationMessage.messageKey,
                        configValidationMessage.defaultMessage, locale, configValidationMessage.content);
                ret.put(configValidationMessage.parameterName, text);
            }
        }
        return Collections.unmodifiableMap(ret);
    }

}
