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
package org.eclipse.smarthome.magic.service;

import org.eclipse.smarthome.config.core.ConfigurableService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author Stefan Triller - Initial contribution
 *
 */

@Component(immediate = true, service = MagicMultiInstanceServiceMarker.class, property = {
        Constants.SERVICE_PID + "=org.eclipse.smarthome.magicMultiInstance",
        ConfigurableService.SERVICE_PROPERTY_FACTORY_SERVICE + "=true",
        ConfigurableService.SERVICE_PROPERTY_LABEL + "=MagicMultiInstanceService",
        ConfigurableService.SERVICE_PROPERTY_CATEGORY + "=test",
        ConfigurableService.SERVICE_PROPERTY_DESCRIPTION_URI + "=test:multipleMagic" })
public class MagicMultiInstanceServiceMarker {
    // this is a marker service and represents a service factory so multiple configuration instances of type
    // "org.eclipse.smarthome.magicMultiInstance" can be created.
}
