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
package org.openhab.core.addon.internal.xml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.OpenHAB;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
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
 * The {@link AddonInfoAddonsXmlProvider} reads all {@code userdata/addons/*.xml} files, each of which
 * should contain a list of {@code addon} elements, and convert their combined contents into a list
 * of {@link AddonInfo} objects can be accessed via the {@link AddonInfoProvider} interface.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonInfoProvider.class, name = AddonInfoAddonsXmlProvider.SERVICE_NAME)
public class AddonInfoAddonsXmlProvider implements AddonInfoProvider {

    public static final String SERVICE_NAME = "addons-info-provider";

    private final Logger logger = LoggerFactory.getLogger(AddonInfoAddonsXmlProvider.class);
    private final String folder = OpenHAB.getUserDataFolder() + File.separator + "addons";
    private final Set<AddonInfo> addonInfos = new HashSet<>();

    @Activate
    public AddonInfoAddonsXmlProvider() {
        initialize();
        testAddonDeveloperRegexSyntax();
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
        File fileFolder = new File(folder);
        try {
            if (!fileFolder.isDirectory()) {
                logger.debug("Folder '{}' does not exist", folder);
                return;
            }
        } catch (SecurityException e) {
            logger.warn("Folder '{}' threw a security exception: {}", folder, e.getMessage());
            return;
        }
        AddonInfoListReader reader = new AddonInfoListReader();
        Stream.of(fileFolder.listFiles()).filter(f -> f.isFile() && f.getName().endsWith(".xml")).forEach(f -> {
            try {
                String xml = Files.readString(f.toPath());
                if (xml != null && !xml.isBlank()) {
                    addonInfos.addAll(reader.readFromXML(xml).getAddons().stream().collect(Collectors.toSet()));
                } else {
                    logger.warn("File '{}' contents are null or empty", f.getName());
                }
            } catch (IOException e) {
                logger.warn("File '{}' could not be read", f.getName());
            } catch (ConversionException e) {
                logger.warn("File '{}' has invalid content", f.getName());
            } catch (XStreamException e) {
                logger.warn("File '{}' could not be deserialized", f.getName());
            } catch (SecurityException e) {
                logger.warn("File '{}' threw a security exception: {}", folder, e.getMessage());
            }
        });
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
            logger.warn("The following errors were found:\n\t{}", String.join("\n\t", patternErrors));
        }
    }
}
