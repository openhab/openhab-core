/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.tools.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.tools.ExtractedAddonUpgrader;

/**
 * The {@link HomeAssistantAddonUpgrader} checks if the MQTT addon was previously
 * installed, and if Home Assistant things exist, and if so installs the
 * Home Assistant addon.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
public class HomeAssistantAddonUpgrader extends ExtractedAddonUpgrader {
    @Override
    public String getName() {
        return "homeAssistantAddonUpgrader";
    }

    @Override
    public String getDescription() {
        return "Installs the Home Assistant addon if Home Assistant Things are present";
    }

    @Override
    public String getParentAddonName() {
        return "mqtt";
    }

    @Override
    public String getAddonName() {
        return "homeassistant";
    }

    @Override
    public boolean thingTypeMatches(String thingTypeUID) {
        return thingTypeUID.startsWith("mqtt:homeassistant");
    }
}
