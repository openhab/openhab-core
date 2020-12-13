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

import org.openhab.core.config.core.ConfigurableService;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author Stefan Triller - Initial contribution
 */

@Component(immediate = true, service = MagicMultiInstanceServiceMarker.class, //
        property = Constants.SERVICE_PID + "=org.openhab.magicMultiInstance")
@ConfigurableService(category = "test", label = "MagicMultiInstanceService", description_uri = "test:multipleMagic", factory = true)
public class MagicMultiInstanceServiceMarker {
    // this is a marker service and represents a service factory so multiple configuration instances of type
    // "org.openhab.core.magicMultiInstance" can be created.
}
