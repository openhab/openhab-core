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

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.SERVICE_NAME_PROCESS;
import static org.openhab.core.config.discovery.addon.AddonFinderConstants.SERVICE_TYPE_PROCESS;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
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

    private static final String COMMAND = "command";

    public static final String SERVICE_TYPE = SERVICE_TYPE_PROCESS;
    public static final String SERVICE_NAME = SERVICE_NAME_PROCESS;

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
        Set<String> processList = ProcessHandle.allProcesses().map(this::getProcessCommandProcess)
                .filter(Predicate.not(String::isEmpty)).collect(Collectors.toUnmodifiableSet());
        Set<AddonInfo> result = new HashSet<>();
        for (AddonInfo candidate : addonCandidates) {
            for (AddonDiscoveryMethod method : candidate.getDiscoveryMethods().stream()
                    .filter(method -> SERVICE_TYPE.equals(method.getServiceType())).toList()) {
                Map<String, Pattern> matchProperties = method.getMatchProperties().stream()
                        .collect(Collectors.toMap(property -> property.getName(), property -> property.getPattern()));

                // make sure addon.xml specifies required match properties
                Set<String> propertyNames = new HashSet<>(matchProperties.keySet());
                if (!matchProperties.containsKey(COMMAND)) {
                    logger.warn("Add-on '{}' addon.xml file does not specify match property \"{}\"", candidate.getUID(),
                            COMMAND);
                    break;
                }
                // make sure addon.xml does not specify unknown properties
                propertyNames.remove(COMMAND);
                if (!propertyNames.isEmpty()) {
                    logger.warn("Add-on '{}' addon.xml file contains unsupported 'match-property' [{}]",
                            candidate.getUID(), String.join(",", propertyNames));
                    break;
                }

                // now check if a process matches the pattern defined in addon.xml
                logger.debug("Checking candidate: {}", candidate.getUID());

                Pattern p = matchProperties.get(COMMAND);

                boolean match = processList.stream().anyMatch(c -> p.matcher(c).matches());

                if (match) {
                    result.add(candidate);
                    logger.debug("Suggested add-on found: {}", candidate.getUID());
                    break;
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
