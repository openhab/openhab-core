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
package org.openhab.core.config.core;

import static java.util.Collections.*;
import static org.openhab.core.config.core.ConfigUtil.normalizeTypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.internal.ConfigMapper;

/**
 * This class is a wrapper for configuration settings of {@link Thing}s.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Kai Kreuzer - added constructors and normalization
 * @author Gerhard Riegler - added converting BigDecimal values to the type of the configuration class field
 * @author Chris Jackson - fix concurrent modification exception when removing properties
 * @author Markus Rathgeb - add copy constructor
 * @author Michael Riess - fix concurrent modification exception when setting properties
 * @author Michael Riess - fix equals() implementation
 */
public class Configuration {
    private final Map<String, Object> properties;

    public Configuration() {
        this(emptyMap(), true);
    }

    /**
     * Create a new configuration.
     *
     * <p>
     * The new configuration is initialized with the values of the given configuration.
     *
     * @param configuration the configuration that should be cloned (may be null)
     */
    public Configuration(final @Nullable Configuration configuration) {
        this(configuration == null ? emptyMap() : configuration.properties, true);
    }

    /**
     * Create a new configuration.
     *
     * @param properties the properties the configuration should be filled. If null, an empty configuration is created.
     */
    public Configuration(Map<String, Object> properties) {
        this(properties == null ? emptyMap() : properties, false);
    }

    /**
     * Create a new configuration.
     *
     * @param properties the properties to initialize (may be null)
     * @param alreadyNormalized flag if the properties are already normalized
     */
    private Configuration(final Map<String, Object> properties, final boolean alreadyNormalized) {
        this.properties = synchronizedMap(alreadyNormalized ? new HashMap<>(properties) : normalizeTypes(properties));
    }

    public <T> T as(Class<T> configurationClass) {
        synchronized (properties) {
            return ConfigMapper.as(properties, configurationClass);
        }
    }

    /**
     * Check if the given key is present in the configuration.
     *
     * @param key the key that existence should be checked
     * @return true if the key is part of the configuration, false if not
     */
    public boolean containsKey(String key) {
        return properties.containsKey(key);
    }

    /**
     * @deprecated Use {@link #get(String)} instead.
     */
    @Deprecated
    public Object get(Object key) {
        return this.get((String) key);
    }

    public Object get(String key) {
        return properties.get(key);
    }

    public Object put(String key, Object value) {
        return properties.put(key, ConfigUtil.normalizeType(value, null));
    }

    /**
     * @deprecated Use {@link #remove(String)} instead.
     */
    @Deprecated
    public Object remove(Object key) {
        return remove((String) key);
    }

    public Object remove(String key) {
        return properties.remove(key);
    }

    public Set<String> keySet() {
        synchronized (properties) {
            return Collections.unmodifiableSet(new HashSet<>(properties.keySet()));
        }
    }

    public Collection<Object> values() {
        synchronized (properties) {
            return Collections.unmodifiableCollection(new ArrayList<>(properties.values()));
        }
    }

    public Map<String, Object> getProperties() {
        synchronized (properties) {
            return Collections.unmodifiableMap(new HashMap<>(properties));
        }
    }

    public void setProperties(Map<String, Object> newProperties) {
        synchronized (properties) {
            this.properties.clear();
            newProperties.entrySet().forEach(e -> put(e.getKey(), e.getValue()));
        }
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof Configuration) && this.properties.equals(((Configuration) obj).properties);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Configuration[");

        synchronized (properties) {
            boolean first = true;
            for (final Map.Entry<String, Object> prop : properties.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                Object value = prop.getValue();
                sb.append(String.format("{key=%s; type=%s; value=%s}", prop.getKey(),
                        value != null ? value.getClass().getSimpleName() : "?", value));
            }
        }

        sb.append("]");
        return sb.toString();
    }
}
