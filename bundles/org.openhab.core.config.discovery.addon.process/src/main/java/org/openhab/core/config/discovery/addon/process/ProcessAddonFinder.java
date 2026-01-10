/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.addon.process;

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.ADDON_SUGGESTION_FINDER;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link ProcessAddonFinder} for finding suggested add-ons by checking processes running
 * on the openHAB server.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonFinder.class, name = ProcessAddonFinder.SERVICE_NAME)
public class ProcessAddonFinder extends BaseAddonFinder {

    public static final String SERVICE_TYPE = "process";
    public static final String CFG_FINDER_PROCESS = "suggestionFinderProcess";
    public static final String SERVICE_NAME = SERVICE_TYPE + ADDON_SUGGESTION_FINDER;

    private static final String COMMAND = "command";
    private static final String COMMAND_LINE = "commandLine";
    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(COMMAND, COMMAND_LINE);

    private final Logger logger = LoggerFactory.getLogger(ProcessAddonFinder.class);

    /**
     * Private record to extract match property parameters from a {@link ProcessHandle.Info} object.
     * Tries to mitigate differences on different operating systems.
     */
    protected static record ProcessInfo(@Nullable String command, @Nullable String commandLine) {

        /**
         * Initializes the command and commandLine fields.
         * If the command field is not present, it parses the first token in the command line.
         */
        protected static ProcessInfo from(ProcessHandle.Info info) {
            String commandLine = info.commandLine().orElse(null);
            String cmd = info.command().orElse(null);
            if ((cmd == null || cmd.isEmpty()) && commandLine != null) {
                cmd = commandLine;
                String[] args = info.arguments().orElse(null);
                if (args != null) {
                    for (int i = args.length - 1; i >= 0; i--) {
                        int index = cmd.lastIndexOf(args[i]);
                        if (index >= 0) {
                            cmd = cmd.substring(0, index);
                        }
                    }
                }
                cmd = cmd.stripTrailing();
            }
            return new ProcessInfo(cmd, commandLine);
        }
    }

    @Override
    public Set<AddonInfo> getSuggestedAddons() {
        logger.trace("ProcessAddonFinder::getSuggestedAddons");
        Set<AddonInfo> result = new HashSet<>();
        Set<ProcessInfo> processInfos;

        try {
            processInfos = ProcessHandle.allProcesses().map(process -> ProcessInfo.from(process.info()))
                    .filter(info -> (info.command != null) || (info.commandLine != null))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (SecurityException | UnsupportedOperationException unused) {
            logger.info("Cannot obtain process list, suggesting add-ons based on running processes is not possible");
            return result;
        }

        for (AddonInfo candidate : addonCandidates) {
            for (AddonDiscoveryMethod method : candidate.getDiscoveryMethods().stream()
                    .filter(method -> SERVICE_TYPE.equals(method.getServiceType())).toList()) {

                Map<String, Pattern> matchProperties = method.getMatchProperties().stream()
                        .collect(Collectors.toMap(AddonMatchProperty::getName, AddonMatchProperty::getPattern));

                if (matchProperties.isEmpty()) {
                    logger.warn("Add-on info for '{}' contains no 'match-property'", candidate.getUID());
                    break;
                }

                Set<String> propertyNames = new HashSet<>(matchProperties.keySet());
                boolean noSupportedProperty = !propertyNames.removeAll(SUPPORTED_PROPERTIES);

                if (!propertyNames.isEmpty()) {
                    logger.warn("Add-on info for '{}' contains unsupported 'match-property' [{}]", candidate.getUID(),
                            String.join(",", propertyNames));

                    if (noSupportedProperty) {
                        break;
                    }
                }

                logger.trace("Checking candidate: {}", candidate.getUID());
                for (ProcessInfo processInfo : processInfos) {
                    if (propertyMatches(matchProperties, COMMAND, processInfo.command)
                            && propertyMatches(matchProperties, COMMAND_LINE, processInfo.commandLine)) {
                        result.add(candidate);
                        logger.debug("Suggested add-on found: {}", candidate.getUID());
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
}
