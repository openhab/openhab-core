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
package org.openhab.core.magic.service;

import java.util.Map;
import java.util.Map.Entry;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testing service for multi-context configurations.
 *
 * @author Stefan Triller - Initial contribution
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, service = MagicMultiInstanceService.class, configurationPid = "org.openhab.magicmultiinstance")
public class MagicMultiInstanceService {

    private final Logger logger = LoggerFactory.getLogger(MagicMultiInstanceService.class);

    @Activate
    public void activate(Map<String, Object> properties) {
        logger.debug("activate");
        for (Entry<String, Object> e : properties.entrySet()) {
            logger.debug("{}: {}", e.getKey(), e.getValue());
        }
    }

    @Modified
    public void modified(Map<String, Object> properties) {
        logger.debug("modified");
        for (Entry<String, Object> e : properties.entrySet()) {
            logger.debug("{}: {}", e.getKey(), e.getValue());
        }
    }
}
