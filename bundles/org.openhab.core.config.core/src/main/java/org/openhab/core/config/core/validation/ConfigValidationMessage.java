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
package org.openhab.core.config.core.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;

/**
 * The {@link ConfigValidationMessage} is the result of a specific {@link ConfigDescriptionParameter}
 * validation, e.g. the validation of the required flag or of the min/max attribute. It contains the name of the
 * configuration parameter whose value does not meet its declaration in the {@link ConfigDescription}, a default
 * message, a message key to be used for internationalization and an optional content to be passed as parameters into
 * the message.
 *
 * @author Thomas Höfer - Initial contribution
 */
public final class ConfigValidationMessage {

    /** The name of the configuration parameter whose value does not meet its {@link ConfigDescription} declaration. */
    public final String parameterName;

    /** The default message describing the validation issue. */
    public final String defaultMessage;

    /** The key of the message to be used for internationalization. */
    public final String messageKey;

    /** The optional content to be passed as message parameters into the message. */
    public final Object[] content;

    /**
     * Creates a new {@link ConfigValidationMessage}
     *
     * @param parameterName the parameter name
     * @param defaultMessage the default message
     * @param messageKey the message key to be used for internationalization
     */
    public ConfigValidationMessage(String parameterName, String defaultMessage, String messageKey) {
        this(parameterName, defaultMessage, messageKey, Collections.<String> emptyList());
    }

    /**
     * Creates a new {@link ConfigValidationMessage}
     *
     * @param parameterName the parameter name
     * @param defaultMessage the default message
     * @param messageKey the message key to be used for internationalization
     * @param content the content to be passed as parameters into the message
     */
    public ConfigValidationMessage(String parameterName, String defaultMessage, String messageKey, Object... content) {
        Objects.requireNonNull(parameterName, "Parameter Name must not be null");
        Objects.requireNonNull(defaultMessage, "Default message must not be null");
        Objects.requireNonNull(messageKey, "Message key must not be null");
        Objects.requireNonNull(content, "Content must not be null");
        this.parameterName = parameterName;
        this.defaultMessage = defaultMessage;
        this.messageKey = messageKey;
        this.content = content;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(content);
        result = prime * result + ((defaultMessage == null) ? 0 : defaultMessage.hashCode());
        result = prime * result + ((messageKey == null) ? 0 : messageKey.hashCode());
        result = prime * result + ((parameterName == null) ? 0 : parameterName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConfigValidationMessage other = (ConfigValidationMessage) obj;
        if (!Arrays.equals(content, other.content)) {
            return false;
        }
        if (defaultMessage == null) {
            if (other.defaultMessage != null) {
                return false;
            }
        } else if (!defaultMessage.equals(other.defaultMessage)) {
            return false;
        }
        if (messageKey == null) {
            if (other.messageKey != null) {
                return false;
            }
        } else if (!messageKey.equals(other.messageKey)) {
            return false;
        }
        if (parameterName == null) {
            if (other.parameterName != null) {
                return false;
            }
        } else if (!parameterName.equals(other.parameterName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ConfigDescriptionValidationMessage [parameterName=" + parameterName + ", defaultMessage="
                + defaultMessage + ", messageKey=" + messageKey + ", content=" + Arrays.toString(content) + "]";
    }
}
