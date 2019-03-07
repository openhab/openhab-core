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
package org.eclipse.smarthome.config.core;

import static java.util.Collections.*;
import static org.eclipse.smarthome.config.core.ConfigUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.internal.ConfigMapper;

/**
 * This class is a wrapper for configuration settings of {@link Thing}s.
 *
 * @author Dennis Nobel - Initial API and contribution, Changed Logging
 * @author Kai Kreuzer - added constructors and normalization
 * @author Gerhard Riegler - added converting BigDecimal values to the type of the configuration class field
 * @author Chris Jackson - fix concurrent modification exception when removing properties
 * @author Markus Rathgeb - add copy constructor
 */
public class Configuration {
    private final Map<String, Object> properties;

    public Configuration() {
        this(null, true);
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
        this(configuration != null ? configuration.properties : null, true);
    }

    /**
     * Create a new configuration.
     *
     * @param properties the properties the configuration should be filled. If null, an empty configuration is created.
     */
    public Configuration(Map<String, Object> properties) {
        this(properties, false);
    }

    /**
     * Create a new configuration.
     *
     * @param properties        the properties to initialize (may be null)
     * @param alreadyNormalized flag if the properties are already normalized
     */
    private Configuration(final @Nullable Map<String, Object> properties, final boolean alreadyNormalized) {
        if (properties == null) {
            this.properties = new HashMap<>();
            return;
        }
        this.properties = alreadyNormalized ? new HashMap<>(properties) : normalizeTypes(properties);
    }

    public <T> T as(Class<T> configurationClass) {
        synchronized (this) {
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
        synchronized (this) {
            return properties.containsKey(key);
        }
    }

    /**
     * @deprecated Use {@link #get(String)} instead.
     */
    @Deprecated
    public Object get(Object key) {
        return this.get((String) key);
    }

    public Object get(String key) {
        synchronized (this) {
            return properties.get(key);
        }
    }

    public Object put(String key, Object value) {
        synchronized (this) {
            return properties.put(key, normalizeType(value, null));
        }
    }

    /**
     * @deprecated Use {@link #remove(String)} instead.
     */
    @Deprecated
    public Object remove(Object key) {
        return remove((String) key);
    }

    public Object remove(String key) {
        synchronized (this) {
            return properties.remove(key);
        }
    }

    public Set<String> keySet() {
        synchronized (this) {
            return unmodifiableSet(new HashSet<>(properties.keySet()));
        }
    }

    public Collection<Object> values() {
        synchronized (this) {
            return unmodifiableCollection(new ArrayList<>(properties.values()));
        }
    }

    public Map<String, Object> getProperties() {
        synchronized (this) {
            return unmodifiableMap(new HashMap<>(properties));
        }
    }

    public void setProperties(Map<String, Object> properties) {
        synchronized (this) {
            this.properties.clear();
            properties.entrySet().forEach(e -> {
                this.properties.put(e.getKey(), normalizeType(e.getValue(), null));
            });
        }
    }

    @Override
    public int hashCode() {
        synchronized (this) {
            return properties.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Configuration)) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }

    @Override
    public String toString() {
        synchronized (this) {
            if (properties.isEmpty()) {
                return "Configuration[]";
            }

            final StringBuilder sb = new StringBuilder();

            properties.entrySet().forEach(entry -> {
                sb.append(sb.length() == 0 ? "Configuration[" : ", ");

                String key = entry.getKey();
                Object value = entry.getValue();
                Object type = value != null ? value.getClass().getSimpleName() : "?";

                sb.append(String.format("{key=%s; type=%s; value=%s}", key, type, value));
            });

            sb.append("]");
            return sb.toString();
        }
    }

}
