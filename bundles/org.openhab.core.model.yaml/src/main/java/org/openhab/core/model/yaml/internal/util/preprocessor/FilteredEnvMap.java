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
package org.openhab.core.model.yaml.internal.util.preprocessor;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/****
 * A filtered view of the system environment variables that only exposes variables with the "OPENHAB_" prefix.
 * This allows users to reference environment variables in their YAML models.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
class FilteredEnvMap extends AbstractMap<String, String> {
    private static final String PREFIX = "OPENHAB_";
    private final Map<String, String> systemEnv;

    public FilteredEnvMap() {
        this(System.getenv());
    }

    // For tests to inject a custom environment map
    FilteredEnvMap(Map<String, String> systemEnv) {
        this.systemEnv = Objects.requireNonNull(systemEnv);
    }

    @Override
    public String get(Object key) {
        if (!(key instanceof String stringKey)) {
            return null;
        }
        String resolvedKey = stringKey.startsWith(PREFIX) ? stringKey : PREFIX + stringKey;
        return systemEnv.get(resolvedKey);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return systemEnv.entrySet().stream().filter(e -> e.getKey().startsWith(PREFIX)).collect(Collectors.toSet());
    }
}
