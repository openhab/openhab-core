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
package org.openhab.core.config.discovery.addon.discovery;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.addon.finder.AddonSuggestionListener;

/**
 * This is a {@link AddonSuggestionParticipant} base class for discovery
 * participants to find suggested addons.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class AddonSuggestionParticipant {
    protected AddonSuggestionListener listener;
    protected final String bindingId;
    protected final Map<String, String> propertyMatchRegexMap = new HashMap<>();

    public AddonSuggestionParticipant(AddonSuggestionListener listener, String bindingId,
            Map<String, String> propertyMatchRegexMap) {
        this.listener = listener;
        this.bindingId = bindingId;
        this.propertyMatchRegexMap.putAll(propertyMatchRegexMap);
    }

    /**
     * Check if the given property name is in the propertyMatchRegexMap and the
     * given property value matches the respective regular expression.
     * 
     * @param propertyName
     * @param propertyValue
     * @return true a) if the property name exists and the property value matches
     *         the regular expression, or b) the property name does not exist.
     */
    protected boolean isPropertyValid(String propertyName, String propertyValue) {
        String matchRegex = propertyMatchRegexMap.get(propertyName);
        return matchRegex != null ? propertyValue.matches(matchRegex) : true;
    }
}
