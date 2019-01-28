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
package org.eclipse.smarthome.magic.binding.internal;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.eclipse.smarthome.config.core.ConfigOptionProvider;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.eclipse.smarthome.magic.binding.MagicService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Henning Treu - Initial contribution
 */
@Component(configurationPid = "org.eclipse.smarthome.magic", service = ConfigOptionProvider.class, immediate = true, property = {
        "service.pid=org.eclipse.smarthome.magic", "service.config.description.uri=test:magic",
        "service.config.label=Magic", "service.config.category=test" })
public class MagicServiceImpl implements MagicService {
    private final Logger logger = LoggerFactory.getLogger(MagicServiceImpl.class);

    static final String PARAMETER_BACKEND_DECIMAL = "select_decimal_limit";

    @Override
    public Collection<ParameterOption> getParameterOptions(URI uri, String param, Locale locale) {
        if (!uri.equals(CONFIG_URI)) {
            return null;
        }

        if (param.equals(PARAMETER_BACKEND_DECIMAL)) {
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
