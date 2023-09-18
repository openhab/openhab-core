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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This is a {@link BaseCandidate} abstract base class for candidates
 * for suggested addons.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public abstract class BaseCandidate {
    protected final String addonId;
    protected final Map<String, String> propertyMatchRegexMap = new HashMap<>();

    public BaseCandidate(String addonId, Map<String, String> propertyMatchRegexMap) {
        this.addonId = addonId;
        this.propertyMatchRegexMap.putAll(propertyMatchRegexMap);
    }

    public String getAddonId() {
        return addonId;
    }

    /**
     * Helper method to check if the given property name is in the
     * propertyMatchRegexMap and the given property value matches the respective
     * regular expression.
     * 
     * @param propertyName
     * @param propertyValue
     * @return true a) if the property name exists and the property value matches
     *         the regular expression, or b) the property name does not exist.
     */
    protected boolean propertyMatches(String propertyName, String propertyValue) {
        String matchRegex = propertyMatchRegexMap.get(propertyName);
        return matchRegex == null ? true : propertyValue.matches(matchRegex);
    }
}
