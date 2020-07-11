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
package org.openhab.core.magic.binding.internal;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ConfigurableService;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.magic.binding.MagicService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henning Treu - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "org.openhab.magic", service = ConfigOptionProvider.class, immediate = true, property = {
        Constants.SERVICE_PID + "=org.openhab.core.magic",
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=test:magic",
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=Magic",
        ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=test" })
public class MagicServiceImpl implements MagicService {
    private final Logger logger = LoggerFactory.getLogger(MagicServiceImpl.class);

    static final String PARAMETER_BACKEND_DECIMAL = "select_decimal_limit";

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if (!CONFIG_URI.equals(uri)) {
            return null;
        }

        if (PARAMETER_BACKEND_DECIMAL.equals(param)) {
            return Arrays.asList(new ParameterOption(BigDecimal.ONE.toPlainString(), "1"),
                    new ParameterOption(BigDecimal.TEN.toPlainString(), "10"),
                    new ParameterOption(BigDecimal.valueOf(21d).toPlainString(), "21"));
        }

        return null;
    }

    @Activate
    public void activate(Map<String, Object> properties) {
        modified(properties);
    }

    @Modified
    public void modified(Map<String, Object> properties) {
        MagicServiceConfig config = new Configuration(properties).as(MagicServiceConfig.class);
        logger.debug("Magic Service has been modified: {}", config);
    }
}
