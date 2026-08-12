/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import static java.util.Collections.synchronizedMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

/**
 * This class is a wrapper for configuration settings of {@code org.openhab.core.thing.Thing}s.
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
    private final Map<String, Object> normalizedProperties;

    public Configuration() {
        this(Map.of(), Map.of());
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
        this(configuration == null ? Map.of() : configuration.properties,
                configuration == null ? Map.of() : configuration.normalizedProperties);
    }

    /**
     * Create a new configuration.
     *
     * @param properties the raw properties the configuration should be filled. If null, an empty configuration is
     *            created.
     */
    public Configuration(@Nullable Map<String, Object> properties) {
        this(properties == null ? Map.of() : properties, Map.of());
    }

    /**
     * Create a new configuration.
     *
     * @param properties the properties to initialize
     * @param normalizedProperties the normalized properties to initialize
     */
    private Configuration(final Map<String, Object> properties, final Map<String, Object> normalizedProperties) {
        this.properties = synchronizedMap(new HashMap<>(properties));
        this.normalizedProperties = synchronizedMap(new HashMap<>(normalizedProperties));
    }

    public <T> T as(Class<T> configurationClass) {
        synchronized (properties) {
            return ConfigParser.configurationAs(resolvedProperties(), configurationClass);
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

    public Object get(String key) {
        synchronized (properties) {
            if (!properties.containsKey(key)) {
                normalizedProperties.remove(key);
                return null;
            } else if (normalizedProperties.containsKey(key)) {
                return normalizedProperties.get(key);
            }

            Object rawValue = properties.get(key);
            if (rawValue == null) {
                return null;
            }

            Object resolvedValue = resolveValue(rawValue);
            Object normalizedValue = ConfigUtil.normalizeType(resolvedValue);
            normalizedProperties.put(key, normalizedValue);
            return normalizedValue;
        }
    }

    public Object put(String key, @Nullable Object value) {
        synchronized (properties) {
            normalizedProperties.remove(key);
            return properties.put(key, value);
        }
    }

    public Object putNormalized(String key, @Nullable Object value) {
        synchronized (properties) {
            if (!properties.containsKey(key)) {
                properties.put(key, value);
            }
            return normalizedProperties.put(key, value);
        }
    }

    public Object remove(String key) {
        synchronized (properties) {
            normalizedProperties.remove(key);
            return properties.remove(key);
        }
    }

    public Set<String> keySet() {
        synchronized (properties) {
            return Collections.unmodifiableSet(new HashSet<>(properties.keySet()));
        }
    }

    public Collection<Object> values() {
        synchronized (properties) {
            return Collections.unmodifiableCollection(new ArrayList<>(resolvedProperties().values()));
        }
    }

    public Map<String, Object> getProperties() {
        synchronized (properties) {
            return Collections.unmodifiableMap(resolvedProperties());
        }
    }

    /**
     * Returns a copy of the raw, uninterpolated configuration properties.
     *
     * @return raw configuration properties
     */
    public Map<String, Object> getRawProperties() {
        synchronized (properties) {
            return Collections.unmodifiableMap(new HashMap<>(properties));
        }
    }

    public void setProperties(Map<String, Object> newProperties) {
        synchronized (properties) {
            this.normalizedProperties.clear();
            this.properties.clear();
            this.properties.putAll(newProperties);
        }
    }

    private Map<String, Object> resolvedProperties() {
        Map<String, Object> copy = new HashMap<>(properties.size());
        properties.keySet().forEach(key -> copy.put(key, get(key)));
        return copy;
    }

    private static @Nullable Object resolveValue(@Nullable Object value) {
        if (value == null) {
            return value;
        }
        return ConfigUtil.resolveVariables(value);
    }

    @Override
    public int hashCode() {
        return properties.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return (obj instanceof Configuration c) && this.properties.equals(c.properties);
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
