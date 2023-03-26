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
package org.openhab.core.tools.i18n.plugin;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.internal.xml.AddonInfoXmlResult;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.thing.xml.internal.ChannelGroupTypeXmlResult;
import org.openhab.core.thing.xml.internal.ChannelTypeXmlResult;
import org.openhab.core.thing.xml.internal.ThingTypeXmlResult;

import com.google.gson.JsonObject;

/**
 * The bundle information provided by the openHAB XML files in the <code>OH-INF</code> directory.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
public class BundleInfo {

    private String addonId = "";
    private @Nullable AddonInfoXmlResult addonInfoXml;
    private List<ConfigDescription> configDescriptions = new ArrayList<>(5);
    private List<ChannelGroupTypeXmlResult> channelGroupTypesXml = new ArrayList<>(5);
    private List<ChannelTypeXmlResult> channelTypesXml = new ArrayList<>(5);
    private List<ThingTypeXmlResult> thingTypesXml = new ArrayList<>(5);
    private List<JsonObject> moduleTypesJson = new ArrayList<>(5);
    private List<JsonObject> ruleTemplateJson = new ArrayList<>(5);

    public String getAddonId() {
        return addonId;
    }

    public void setAddonId(String addonId) {
        this.addonId = addonId;
    }

    public @Nullable AddonInfoXmlResult getAddonInfoXml() {
        return addonInfoXml;
    }

    public void setAddonInfoXml(AddonInfoXmlResult addonInfo) {
        this.addonInfoXml = addonInfo;
    }

    public List<ConfigDescription> getConfigDescriptions() {
        return configDescriptions;
    }

    public void setConfigDescriptions(List<ConfigDescription> configDescriptions) {
        this.configDescriptions = configDescriptions;
    }

    public List<ChannelGroupTypeXmlResult> getChannelGroupTypesXml() {
        return channelGroupTypesXml;
    }

    public void setChannelGroupTypesXml(List<ChannelGroupTypeXmlResult> channelGroupTypesXml) {
        this.channelGroupTypesXml = channelGroupTypesXml;
    }

    public List<ChannelTypeXmlResult> getChannelTypesXml() {
        return channelTypesXml;
    }

    public void setChannelTypesXml(List<ChannelTypeXmlResult> channelTypesXml) {
        this.channelTypesXml = channelTypesXml;
    }

    public List<JsonObject> getModuleTypesJson() {
        return moduleTypesJson;
    }

    public void setModuleTypesJson(List<JsonObject> moduleTypesJson) {
        this.moduleTypesJson = moduleTypesJson;
    }

    public List<JsonObject> getRuleTemplateJson() {
        return ruleTemplateJson;
    }

    public void setRuleTemplateJson(List<JsonObject> ruleTemplateJson) {
        this.ruleTemplateJson = ruleTemplateJson;
    }

    public List<ThingTypeXmlResult> getThingTypesXml() {
        return thingTypesXml;
    }

    public void setThingTypesXml(List<ThingTypeXmlResult> thingTypesXml) {
        this.thingTypesXml = thingTypesXml;
    }

    public Optional<ConfigDescription> getConfigDescription(@Nullable URI uri) {
        if (uri == null) {
            return Optional.empty();
        }

        return configDescriptions.stream().filter(configDescription -> configDescription.getUID().equals(uri))
                .findFirst();
    }
}
