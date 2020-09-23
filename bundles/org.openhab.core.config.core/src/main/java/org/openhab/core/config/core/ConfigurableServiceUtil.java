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

import java.lang.annotation.Annotation;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Provides utility methods for working with {@link ConfigurableService} so the property names can remain hidden.
 * These methods cannot be part of {@link ConfigurableService} as that introduces an annotation cycle.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class ConfigurableServiceUtil {

    /**
     * The config description URI for the configurable service. See also {@link ConfigDescription}.
     */
    static final String SERVICE_PROPERTY_DESCRIPTION_URI = ConfigurableService.PREFIX_ + "description.uri";

    /**
     * The label of the service to be configured.
     */
    static final String SERVICE_PROPERTY_LABEL = ConfigurableService.PREFIX_ + "label";

    /**
     * The category of the service to be configured (e.g. binding).
     */
    static final String SERVICE_PROPERTY_CATEGORY = ConfigurableService.PREFIX_ + "category";

    /**
     * Marker for multiple configurations for this service ("true" = multiple configurations possible)
     */
    static final String SERVICE_PROPERTY_FACTORY_SERVICE = ConfigurableService.PREFIX_ + "factory";

    // all singleton services without multi-config services
    public static final String CONFIGURABLE_SERVICE_FILTER = "(&(" + SERVICE_PROPERTY_DESCRIPTION_URI + "=*)(!("
            + SERVICE_PROPERTY_FACTORY_SERVICE + "=*)))";

    // all multi-config services without singleton services
    public static final String CONFIGURABLE_MULTI_CONFIG_SERVICE_FILTER = "(" + SERVICE_PROPERTY_FACTORY_SERVICE
            + "=*)";

    public static ConfigurableService asConfigurableService(Function<String, @Nullable Object> propertyResolver) {
        return new ConfigurableService() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ConfigurableService.class;
            }

            @Override
            public String label() {
                return resolveString(propertyResolver, SERVICE_PROPERTY_LABEL);
            }

            @Override
            public boolean factory() {
                return resolveBoolean(propertyResolver, SERVICE_PROPERTY_FACTORY_SERVICE);
            }

            @Override
            public String description_uri() {
                return resolveString(propertyResolver, SERVICE_PROPERTY_DESCRIPTION_URI);
            }

            @Override
            public String category() {
                return resolveString(propertyResolver, SERVICE_PROPERTY_CATEGORY);
            }
        };
    }

    private static String resolveString(Function<String, @Nullable Object> propertyResolver, String key) {
        String value = (String) propertyResolver.apply(key);
        return value == null ? "" : value;
    }

    private static boolean resolveBoolean(Function<String, @Nullable Object> propertyResolver, String key) {
        Boolean value = (Boolean) propertyResolver.apply(key);
        return value == null ? false : value.booleanValue();
    }
}
