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
package org.openhab.core.addon.infoproviders;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoList;
import org.openhab.core.addon.AddonInfoListReader;
import org.openhab.core.addon.AddonInfoProvider;
import org.openhab.core.addon.AddonMatchProperty;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.ConversionException;

/**
 * The {@link KarafAddonsInfoProvider} provides information from the addon.xml file of
 * the addons that will are packaged in the openhab-addons .kar file.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonInfoProvider.class, name = KarafAddonsInfoProvider.SERVICE_NAME)
public class KarafAddonsInfoProvider implements AddonInfoProvider {

    public static final String SERVICE_NAME = "karaf-addons-info-provider";

    private static final boolean TEST_ADDON_DEVELOPER_REGEX_SYNTAX = true;

    private final Logger logger = LoggerFactory.getLogger(KarafAddonsInfoProvider.class);
    private final String addonsXmlPathName = OpenHAB.getUserDataFolder() + File.separator + "addons.xml";
    private final Set<AddonInfo> addonInfos = new HashSet<>();

    @Activate
    public KarafAddonsInfoProvider() {
        initialize();
        if (TEST_ADDON_DEVELOPER_REGEX_SYNTAX) {
            testAddonDeveloperRegexSyntax();
        }
    }

    @Deactivate
    public void deactivate() {
        addonInfos.clear();
    }

    @Override
    public @Nullable AddonInfo getAddonInfo(@Nullable String uid, @Nullable Locale locale) {
        return addonInfos.stream().filter(a -> a.getUID().equals(uid)).findFirst().orElse(null);
    }

    @Override
    public Set<AddonInfo> getAddonInfos(@Nullable Locale locale) {
        return addonInfos;
    }

    private void initialize() {
        String addonsXml;
        try (InputStream stream = new FileInputStream(addonsXmlPathName)) {
            addonsXml = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.warn("The 'addons.xml' file is missing");
            return;
        }
        if (addonsXml.isBlank()) {
            logger.warn("The 'addons.xml' file is empty");
            return;
        }
        try {
            AddonInfoList addonInfoList = new AddonInfoListReader().readFromXML(addonsXml);
            addonInfos.addAll(addonInfoList.getAddons().stream().collect(Collectors.toSet()));
        } catch (ConversionException e) {
            logger.warn("The 'addons.xml' file has invalid content");
            return;
        } catch (XStreamException e) {
            logger.warn("The 'addons.xml' file cannot be deserialized");
            return;
        }
    }

    /*
     * The openhab-addons Maven build process checks individual developer addon.xml contributions
     * against the 'addon-1.0.0.xsd' schema, but it can't check the discovery-method match-property
     * regex syntax. Invalid regexes do throw exceptions at run-time, but the log can't identify the
     * culprit addon. Ideally we need to add syntax checks to the Maven build; and this test is an
     * interim solution.
     */
    private void testAddonDeveloperRegexSyntax() {
        List<String> patternErrors = new ArrayList<>();
        for (AddonInfo addonInfo : addonInfos) {
            for (AddonDiscoveryMethod discoveryMethod : addonInfo.getDiscoveryMethods()) {
                for (AddonMatchProperty matchProperty : discoveryMethod.getMatchProperties()) {
                    try {
                        matchProperty.getPattern();
                    } catch (PatternSyntaxException e) {
                        patternErrors.add(String.format(
                                "Regex syntax error in org.openhab.%s.%s addon.xml => %s in \"%s\" position %d",
                                addonInfo.getType(), addonInfo.getId(), e.getDescription(), e.getPattern(),
                                e.getIndex()));
                    }
                }
            }
        }
        if (!patternErrors.isEmpty()) {
            logger.warn("The 'addons.xml' file has errors\n\t{}", String.join("\n\t", patternErrors));
        }
    }
}
