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
package org.openhab.core.thing.xml.internal;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.openhab.core.config.core.ConfigDescription;

/**
 *
 * @author Connor Petty - Initial contribution
 */
public class ConfigDescriptionXmlResult {

    private URI uri;
    private ConfigDescription configDescription;

    public ConfigDescriptionXmlResult(URI uri) {
        this.uri = uri;
    }

    public ConfigDescriptionXmlResult(ConfigDescription configDescription) {
        this.uri = configDescription.getUID();
        this.configDescription = configDescription;
    }

    public URI getUri() {
        return uri;
    }

    public ConfigDescription getConfigDescription() {
        return configDescription;
    }

    static List<URI> toConfigDescriptionURIs(List<ConfigDescriptionXmlResult> configDescriptionReferences) {
        List<URI> configDescriptionURIs = null;
        if (configDescriptionReferences != null && !configDescriptionReferences.isEmpty()) {
            configDescriptionURIs = new ArrayList<>(configDescriptionReferences.size());
            for (ConfigDescriptionXmlResult configDescriptionReference : configDescriptionReferences) {
                configDescriptionURIs.add(configDescriptionReference.getUri());
            }
        }
        return configDescriptionURIs;
    }

    static List<ConfigDescription> toConfigDescriptions(List<ConfigDescriptionXmlResult> configDescriptionReferences) {
        List<ConfigDescription> configDescriptions = null;
        if (configDescriptionReferences != null && !configDescriptionReferences.isEmpty()) {
            configDescriptions = new ArrayList<>(configDescriptionReferences.size());
            for (ConfigDescriptionXmlResult configDescriptionReference : configDescriptionReferences) {
                ConfigDescription configDescription = configDescriptionReference.getConfigDescription();
                if (configDescription != null) {
                    configDescriptions.add(configDescription);
                }
            }
        }
        return configDescriptions;
    }
}
