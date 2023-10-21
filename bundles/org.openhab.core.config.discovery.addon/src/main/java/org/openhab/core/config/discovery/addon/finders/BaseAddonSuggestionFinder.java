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
package org.openhab.core.config.discovery.addon.finders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.AddonInfo;

/**
 * This is a {@link BaseAddonSuggestionFinder} abstract class for finding suggested Addons.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public abstract class BaseAddonSuggestionFinder implements AddonSuggestionFinder {

    protected static final String ADDON_SUGGESTION_FINDER = "-addon-suggestion-finder";

    protected final List<AddonInfo> addonCandidates = Collections.synchronizedList(new ArrayList<>());
    protected final Set<String> addonSuggestionUIDs = ConcurrentHashMap.newKeySet();

    protected boolean scanDone;

    public Set<String> getAddonSuggestionUIDs() {
        return addonSuggestionUIDs;
    }

    /**
     * Helper method to check if the given property name is in the propertyRegexMap
     * and the given property value matches the respective regular expression.
     * 
     * @param propertyRegexMap map of property names and regexes for value matching
     * @param propertyName
     * @param propertyValue
     * @return true a) if the property name exists and the property value matches
     *         the regular expression, or b) the property name does not exist.
     */
    protected static boolean propertyMatches(Map<String, String> propertyRegexMap, String propertyName,
            String propertyValue) {
        String matchRegex = propertyRegexMap.get(propertyName);
        return matchRegex == null ? true : propertyValue.matches(matchRegex);
    }

    public void reset() {
        addonCandidates.clear();
        addonSuggestionUIDs.clear();
        scanDone = false;
    }

    public boolean scanDone() {
        return scanDone;
    }

    public abstract void scanTask();

    public void setAddonCandidates(List<AddonInfo> candidates) {
        reset();
        addonCandidates.addAll(candidates);
    }
}
