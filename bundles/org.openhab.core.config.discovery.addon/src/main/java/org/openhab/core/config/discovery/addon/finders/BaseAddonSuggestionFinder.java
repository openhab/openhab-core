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
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonInfo;

/**
 * This is a {@link BaseAddonSuggestionFinder} abstract class for finding suggested Addons.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public abstract class BaseAddonSuggestionFinder implements AddonSuggestionFinder {

    public static final String ADDON_SUGGESTION_FINDER_ENABLED_PROPERTY = "enabled";
    protected static final String ADDON_SUGGESTION_FINDER = "-addon-suggestion-finder";
    protected static final String ADDON_SUGGESTION_FINDER_CONFIG_PID = "discovery.addon.";

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
        return pattern == null ? true : propertyValue == null ? false : pattern.matcher(propertyValue).matches();
    }

    protected final List<AddonInfo> addonCandidates = Collections.synchronizedList(new ArrayList<>());
    private boolean connected = false;

    /**
     * Implementation classes must implement their specific constructor and OSGI annotate it so that the OSGI framework
     * will call it when the component is activated. Such methods must call this {@code activate()} method. Depending on
     * the given configuration properties, it either connects (enables) or disconnects (disables) the service.
     * 
     * @param configProperties the configuration properties.
     */
    protected void activate(@Nullable Map<String, Object> configProperties) {
        if (connected ^ getTargetConnected(configProperties)) {
            if (connected) {
                disconnect();
            } else
                connect();
        }
    }

    /**
     * Implementation classes must override this method in order to connect (enable) the service. Implementations must
     * call {@code super.connect()}.
     */
    protected void connect() {
        connected = true;
    }

    /**
     * Implementation classes must override this method and OSGI annotate it so that the OSGI framework will call it
     * when the component is deactivated. Overridden methods must call {@code super.deactivate()}. Generally it should
     * clear the finder state and disable the service.
     */
    public void deactivate() {
        if (connected) {
            disconnect();
        }
        addonCandidates.clear();
    }

    /**
     * Implementation classes must override this method in order to disconnect (disable) the service. Implementations
     * must call {@code super.disconnect()}.
     */
    protected void disconnect() {
        connected = false;
    }

    /**
     * Helper method that reads a configuration property and determines the target connected state.
     * 
     * @param configProperties the configuration properties.
     * @return true if the target is for the service to be connected, false otherwise.
     */
    private boolean getTargetConnected(@Nullable Map<String, Object> configProperties) {
        if (configProperties != null) {
            Object property = configProperties.get(ADDON_SUGGESTION_FINDER_ENABLED_PROPERTY);
            if (property instanceof String string) {
                return Boolean.valueOf(string);
            } else {
                return !Boolean.FALSE.equals(property);
            }
        }
        return true;
    }

    /**
     * Implementation classes must override this method and OSGI annotate it so that the OSGI framework will call it
     * when the component configuration is modified. Overridden methods must call {@code super.modified()}. Generally
     * it should have the same effect as the {@code activate()} method.
     * 
     * @param configProperties the modified configuration properties.
     */
    public void modified(@Nullable Map<String, Object> configProperties) {
        activate(configProperties);
    }

    public synchronized void setAddonCandidates(List<AddonInfo> candidates) {
        addonCandidates.clear();
        addonCandidates.addAll(candidates);
    }
}
