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
package org.eclipse.smarthome.automation.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeRegistry;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigUtil;
import org.eclipse.smarthome.config.core.Configuration;

/**
 * This class provides utility methods used by {@link RuleRegistry} to resolve and normalize the {@link RuleImpl}s
 * configuration values.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class ConfigurationNormalizer {

    /**
     * Normalizes the configurations of the provided {@link ModuleImpl}s.
     *
     * @param modules a list of {@link ModuleImpl}s to normalize.
     * @param mtRegistry the {@link ModuleTypeRegistry} that provides the meta-data needed for the normalization.
     * @see ConfigurationNormalizer#normalizeConfiguration(Configuration, Map)
     */
    public static <T extends Module> void normalizeModuleConfigurations(List<T> modules,
            ModuleTypeRegistry mtRegistry) {
        for (Module module : modules) {
            ModuleType mt = mtRegistry.get(module.getTypeUID());
            if (mt != null) {
                Map<String, ConfigDescriptionParameter> mapConfigDescriptions = getConfigDescriptionMap(
                        mt.getConfigurationDescriptions());
                normalizeConfiguration(module.getConfiguration(), mapConfigDescriptions);
            }
        }
    }

    /**
     * Converts a list of {@link ConfigDescriptionParameter}s to a map with the parameter's names as keys.
     *
     * @param configDesc the list to convert.
     * @return a map that maps parameter names to {@link ConfigDescriptionParameter} instances.
     */
    public static Map<String, ConfigDescriptionParameter> getConfigDescriptionMap(
            List<ConfigDescriptionParameter> configDesc) {
        Map<String, ConfigDescriptionParameter> mapConfigDescs = new HashMap<String, ConfigDescriptionParameter>();
        for (ConfigDescriptionParameter configDescriptionParameter : configDesc) {
            mapConfigDescs.put(configDescriptionParameter.getName(), configDescriptionParameter);
        }
        return mapConfigDescs;
    }

    /**
     * Normalizes the types of the configuration's parameters to the allowed ones. References are ignored. Null values
     * are replaced with the defaults and then normalized.
     *
     * @param configuration the configuration to normalize.
     * @param configDescriptionMap the meta-data of the configuration.
     */
    public static void normalizeConfiguration(Configuration configuration,
            Map<String, ConfigDescriptionParameter> configDescriptionMap) {
        for (Entry<String, ConfigDescriptionParameter> entry : configDescriptionMap.entrySet()) {
            ConfigDescriptionParameter parameter = entry.getValue();
            if (parameter != null) {
                String parameterName = entry.getKey();
                final Object value = configuration.get(parameterName);
                final Object defaultValue = parameter.getDefault();
                if (value instanceof String && ((String) value).contains("${")) {
                    continue; // It is a reference
                }
                if (value == null) {
                    if (defaultValue == null) {
                        configuration.remove(parameterName);
                    } else {
                        configuration.put(parameterName, ConfigUtil.normalizeType(defaultValue, parameter));
                    }
                } else {
                    configuration.put(parameterName, ConfigUtil.normalizeType(value, parameter));
                }
            }
        }
    }
}
