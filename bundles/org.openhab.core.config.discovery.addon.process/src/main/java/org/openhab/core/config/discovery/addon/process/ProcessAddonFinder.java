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
package org.openhab.core.config.discovery.addon.process;

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.ADDON_SUGGESTION_FINDER;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.osgi.service.component.annotations.Activate;
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

    private final Logger logger = LoggerFactory.getLogger(ProcessAddonFinder.class);

    @Activate
    public ProcessAddonFinder() {
    }

    // get list of running processes visible to openHAB,
    // also tries to mitigate differences on different operating systems
    String getProcessCommandProcess(ProcessHandle h) {
        Optional<String> command = h.info().command();
        if (command.isPresent())
            return command.get();
        Optional<String[]> args = h.info().arguments();
        if (!args.isPresent())
            return "";
        String[] argsArray = args.get();
        if (argsArray.length < 1)
            return "";
        return argsArray[0];
    }

    @Override
    public Set<AddonInfo> getSuggestedAddons() {
        logger.trace("ProcessAddonFinder::getSuggestedAddons");
        Set<AddonInfo> result = new HashSet<>();
        Set<String> processList = Collections.emptySet();
        try {
            processList = ProcessHandle.allProcesses().map(this::getProcessCommandProcess)
                    .filter(Predicate.not(String::isEmpty)).collect(Collectors.toUnmodifiableSet());
        } catch (SecurityException | UnsupportedOperationException unused) {
            logger.info("Cannot obtain process list, suggesting add-ons based on running processes is not possible");
            return result;
        }

        for (AddonInfo candidate : addonCandidates) {
            for (AddonDiscoveryMethod method : candidate.getDiscoveryMethods().stream()
                    .filter(method -> SERVICE_TYPE.equals(method.getServiceType())).toList()) {

                List<AddonMatchProperty> matchProperties = method.getMatchProperties();
                List<AddonMatchProperty> commands = matchProperties.stream()
                        .filter(amp -> COMMAND.equals(amp.getName())).collect(Collectors.toUnmodifiableList());

                if (matchProperties.size() != commands.size()) {
                    logger.warn("Add-on '{}' addon.xml file contains unsupported 'match-property'", candidate.getUID());
                }

                if (commands.isEmpty()) {
                    logger.warn("Add-on '{}' addon.xml file does not specify match property \"{}\"", candidate.getUID(),
                            COMMAND);
                    break;
                }

                // now check if a process matches the pattern defined in addon.xml
                logger.debug("Checking candidate: {}", candidate.getUID());

                for (AddonMatchProperty command : commands) {
                    logger.trace("Candidate {}, pattern \"{}\"", candidate.getUID(), command.getRegex());
                    boolean match = processList.stream().anyMatch(c -> command.getPattern().matcher(c).matches());

                    if (match) {
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
