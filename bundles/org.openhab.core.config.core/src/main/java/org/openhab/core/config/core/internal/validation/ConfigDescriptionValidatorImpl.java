/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.config.core.internal.validation;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.validation.ConfigDescriptionValidator;
import org.openhab.core.config.core.validation.ConfigValidationException;
import org.openhab.core.config.core.validation.ConfigValidationMessage;
import org.openhab.core.i18n.TranslationProvider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ConfigDescriptionValidatorImpl} validates a given set of {@link Configuration} parameters against a
 * given {@link ConfigDescription} URI. So it can be used as a static pre-validation to avoid that the configuration of
 * an entity is updated with parameters which do not match with the declarations in the configuration description.
 * If the validator detects one or more mismatches then a {@link ConfigValidationException} is thrown.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Chris Jackson - Handle checks on multiple selection parameters
 */
@Component
@NonNullByDefault
public final class ConfigDescriptionValidatorImpl implements ConfigDescriptionValidator {

    private static final List<ConfigDescriptionParameterValidator> VALIDATORS = List.of( //
            ConfigDescriptionParameterValidatorFactory.createRequiredValidator(), //
            ConfigDescriptionParameterValidatorFactory.createTypeValidator(), //
            ConfigDescriptionParameterValidatorFactory.createMinMaxValidator(), //
            ConfigDescriptionParameterValidatorFactory.createPatternValidator(), //
            ConfigDescriptionParameterValidatorFactory.createOptionsValidator() //
    );

    private final Logger logger = LoggerFactory.getLogger(ConfigDescriptionValidatorImpl.class);

    private final ConfigDescriptionRegistry configDescriptionRegistry;
    private final TranslationProvider translationProvider;
    private final BundleContext bundleContext;

    @Activate
    public ConfigDescriptionValidatorImpl(final BundleContext bundleContext,
            final @Reference ConfigDescriptionRegistry configDescriptionRegistry,
            final @Reference TranslationProvider translationProvider) {
        this.bundleContext = bundleContext;
        this.configDescriptionRegistry = configDescriptionRegistry;
        this.translationProvider = translationProvider;
    }

    /**
     * Validates the given configuration parameters against the given configuration description having the given URI.
     *
     * @param configurationParameters the configuration parameters to be validated
     * @param configDescriptionURI the URI of the configuration description against which the configuration parameters
     *            are to be validated
     *
     * @throws ConfigValidationException if one or more configuration parameters do not match with the configuration
     *             description having the given URI
     * @throws NullPointerException if given config description URI or configuration parameters are null
     */
    @Override
    @SuppressWarnings({ "unchecked", "null" })
    public void validate(Map<String, Object> configurationParameters, URI configDescriptionURI) {
        Objects.requireNonNull(configurationParameters, "Configuration parameters must not be null");
        Objects.requireNonNull(configDescriptionURI, "Config description URI must not be null");

        ConfigDescription configDescription = getConfigDescription(configDescriptionURI);

        if (configDescription == null) {
            logger.warn("Skipping config description validation because no config description found for URI '{}'",
                    configDescriptionURI);
            return;
        }

        Map<String, ConfigDescriptionParameter> map = configDescription.toParametersMap();

        Collection<ConfigValidationMessage> configDescriptionValidationMessages = new ArrayList<>();

        for (String key : map.keySet()) {
            ConfigDescriptionParameter configDescriptionParameter = map.get(key);
            if (configDescriptionParameter != null) {
                // If the parameter supports multiple selection, then it may be provided as an array
                if (configDescriptionParameter.isMultiple() && configurationParameters.get(key) instanceof List) {
                    List<Object> values = (List<Object>) configurationParameters.get(key);
                    // check if multipleLimit is obeyed
                    Integer multipleLimit = configDescriptionParameter.getMultipleLimit();
                    if (multipleLimit != null && values.size() > multipleLimit) {
                        MessageKey messageKey = MessageKey.MULTIPLE_LIMIT_VIOLATED;
                        ConfigValidationMessage message = new ConfigValidationMessage(
                                configDescriptionParameter.getName(), messageKey.defaultMessage, messageKey.key,
                                multipleLimit, values.size());
                        configDescriptionValidationMessages.add(message);
                    }
                    // Perform validation on each value in the list separately
                    for (Object value : values) {
                        ConfigValidationMessage message = validateParameter(configDescriptionParameter, value);
                        if (message != null) {
                            configDescriptionValidationMessages.add(message);
                        }
                    }
                } else {
                    ConfigValidationMessage message = validateParameter(configDescriptionParameter,
                            configurationParameters.get(key));
                    if (message != null) {
                        configDescriptionValidationMessages.add(message);
                    }
                }
            }
        }

        if (!configDescriptionValidationMessages.isEmpty()) {
            throw new ConfigValidationException(bundleContext.getBundle(), translationProvider,
                    configDescriptionValidationMessages);
        }
    }

    /**
     * Validates the given value against the given config description parameter.
     *
     * @param configDescriptionParameter the corresponding config description parameter
     * @param value the actual value
     *
     * @return the {@link ConfigValidationMessage} if the given value is not valid for the config description parameter,
     *         otherwise null
     */
    private @Nullable ConfigValidationMessage validateParameter(ConfigDescriptionParameter configDescriptionParameter,
            @Nullable Object value) {
        for (ConfigDescriptionParameterValidator validator : VALIDATORS) {
            ConfigValidationMessage message = validator.validate(configDescriptionParameter, value);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    /**
     * Retrieves the {@link ConfigDescription} for the given URI.
     *
     * @param configDescriptionURI the URI of the configuration description to be retrieved
     *
     * @return the requested config description or null if config description could not be found (either because of
     *         config description registry is not available or because of config description could not be found for
     *         given URI)
     */
    private @Nullable ConfigDescription getConfigDescription(URI configDescriptionURI) {
        ConfigDescription configDescription = configDescriptionRegistry.getConfigDescription(configDescriptionURI);
        if (configDescription == null) {
            logger.warn("No config description found for URI '{}'", configDescriptionURI);
        }
        return configDescription;
    }
}
