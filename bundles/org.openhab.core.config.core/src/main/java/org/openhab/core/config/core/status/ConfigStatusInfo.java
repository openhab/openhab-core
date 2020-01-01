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
package org.openhab.core.config.core.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.openhab.core.config.core.status.ConfigStatusMessage.Type;

/**
 * The {@link ConfigStatusInfo} contains {@link ConfigStatusMessage}s to represent the current configuration status of
 * an entity. Furthermore it provides some convenience operations to filter for specific {@link ConfigStatusMessage}s.
 *
 * @author Thomas Höfer - Initial contribution
 */
public final class ConfigStatusInfo {

    private final Collection<ConfigStatusMessage> configStatusMessages = new ArrayList<>();

    /**
     * Creates a new {@link ConfigStatusInfo}.
     */
    public ConfigStatusInfo() {
        super();
    }

    /**
     * Creates a new {@link ConfigStatusInfo} with the given {@link ConfigStatusMessages}.
     *
     * @param configStatusMessages the configuration status messages to be added
     */
    public ConfigStatusInfo(Collection<ConfigStatusMessage> configStatusMessages) {
        add(configStatusMessages);
    }

    /**
     * Retrieves all configuration status messages.
     *
     * @return an unmodifiable collection of available configuration status messages
     */
    public Collection<ConfigStatusMessage> getConfigStatusMessages() {
        return Collections.unmodifiableCollection(configStatusMessages);
    }

    /**
     * Retrieves all configuration status messages that have one of the given types.
     *
     * @param types the types to be filtered for; if empty then all messages are delivered
     * @return an unmodifiable collection of the corresponding configuration status messages
     */
    public Collection<ConfigStatusMessage> getConfigStatusMessages(Type... types) {
        final Collection<Type> typesCollection = Arrays.asList(types);
        return filter(typesCollection, configStatusMessage -> typesCollection.contains(configStatusMessage.type));
    }

    /**
     * Retrieves all configuration status messages that have one of the given parameter names.
     *
     * @param parameterNames the parameter names to be filtered for; if empty then all messages are delivered
     * @return an unmodifiable collection of the corresponding configuration status messages
     */
    public Collection<ConfigStatusMessage> getConfigStatusMessages(String... parameterNames) {
        final Collection<String> parameterNamesCollection = Arrays.asList(parameterNames);
        return filter(parameterNamesCollection,
                configStatusMessage -> parameterNamesCollection.contains(configStatusMessage.parameterName));
    }

    /**
     * Retrieves all configuration status messages that have one of the given parameter names or types.
     *
     * @param types the types to be filtered for (must not be null)
     * @param parameterNames the parameter names to be filtered for (must not be null)
     * @return an unmodifiable collection of the corresponding configuration status messages
     * @throws NullPointerException if one of types or parameter names collection is empty
     */
    public Collection<ConfigStatusMessage> getConfigStatusMessages(final Collection<Type> types,
            final Collection<String> parameterNames) {
        Objects.requireNonNull(types);
        Objects.requireNonNull(parameterNames);
        return filterConfigStatusMessages(getConfigStatusMessages(),
                configStatusMessage -> types.contains(configStatusMessage.type)
                        || parameterNames.contains(configStatusMessage.parameterName));
    }

    /**
     * Adds the given {@link ConfigStatusMessage}.
     *
     * @param configStatusMessage the configuration status message to be added
     * @throws IllegalArgumentException if given configuration status message is null
     */
    public void add(ConfigStatusMessage configStatusMessage) {
        if (configStatusMessage == null) {
            throw new IllegalArgumentException("Config status message must not be null");
        }
        configStatusMessages.add(configStatusMessage);
    }

    /**
     * Adds the given given {@link ConfigStatusMessage}s.
     *
     * @param configStatusMessages the configuration status messages to be added
     * @throws IllegalArgumentException if given collection is null
     */
    public void add(Collection<ConfigStatusMessage> configStatusMessages) {
        if (configStatusMessages == null) {
            throw new IllegalArgumentException("Config status messages must not be null");
        }
        for (ConfigStatusMessage configStatusMessage : configStatusMessages) {
            add(configStatusMessage);
        }
    }

    private Collection<ConfigStatusMessage> filter(Collection<?> filter, Predicate<ConfigStatusMessage> predicate) {
        if (filter.isEmpty()) {
            return getConfigStatusMessages();
        }
        return filterConfigStatusMessages(getConfigStatusMessages(), predicate);
    }

    private static Collection<ConfigStatusMessage> filterConfigStatusMessages(
            Collection<ConfigStatusMessage> configStatusMessages, Predicate<? super ConfigStatusMessage> predicate) {
        return configStatusMessages.stream().filter(predicate).collect(Collectors.toList());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((configStatusMessages == null) ? 0 : configStatusMessages.hashCode());
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
        ConfigStatusInfo other = (ConfigStatusInfo) obj;
        if (configStatusMessages == null) {
            if (other.configStatusMessages != null) {
                return false;
            }
        } else if (!configStatusMessages.equals(other.configStatusMessages)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ConfigStatusInfo [configStatusMessages=" + configStatusMessages + "]";
    }
}
