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
package org.openhab.core.config.discovery.addon;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonInfo;

/**
 * This is a {@link BaseAddonFinder} abstract class for finding suggested add-ons.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public abstract class BaseAddonFinder implements AddonFinder {

    /**
     * Helper method to check if the given {@code propertyName} is in the {@code propertyPatternMap} and if so, the
     * given {@code propertyValue} matches the respective regular expression {@code Pattern}.
     *
     * @param propertyPatternMap map of property names and regex patterns for value matching
     * @param propertyName
     * @param propertyValue
     * @return true a) if the property name exists and the property value is not null and matches the regular
     *         expression, or b) the property name does not exist.
     */
    protected static boolean propertyMatches(Map<String, Pattern> propertyPatternMap, String propertyName,
            @Nullable String propertyValue) {
        Pattern pattern = propertyPatternMap.get(propertyName);
        return pattern == null || (propertyValue != null && pattern.matcher(propertyValue).matches());
    }

    protected volatile List<AddonInfo> addonCandidates = List.of();

    @Override
    public void setAddonCandidates(List<AddonInfo> candidates) {
        addonCandidates = candidates;
    }

    @Override
    public void unsetAddonCandidates() {
        addonCandidates = List.of();
    }

    public abstract String getServiceName();
}
