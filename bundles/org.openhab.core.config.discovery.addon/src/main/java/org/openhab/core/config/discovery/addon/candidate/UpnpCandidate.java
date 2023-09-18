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
package org.openhab.core.config.discovery.addon.candidate;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.model.meta.RemoteDevice;

/**
 * This is a {@link UpnpCandidate} upnp version of class for candidates for
 * suggested addons.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class UpnpCandidate extends BaseCandidate {

    public UpnpCandidate(String bindingId, Map<String, String> propertyMatchRegexMap) {
        super(bindingId, propertyMatchRegexMap);
    }

    /**
     * Check if the data in the provided RemoteDevice matches our own match
     * criteria.
     */
    @Override
    public boolean equals(@Nullable Object object) {
        if (object instanceof RemoteDevice device) {
            return propertyMatches("deviceType", device.getType().getType())
                    && propertyMatches("manufacturer", device.getDetails().getManufacturerDetails().getManufacturer())
                    && propertyMatches("modelName", device.getDetails().getModelDetails().getModelName())
                    && propertyMatches("serialNumber", device.getDetails().getSerialNumber())
                    && propertyMatches("udn", device.getIdentity().getUdn().getIdentifierString());
        }
        return false;
    }
}
