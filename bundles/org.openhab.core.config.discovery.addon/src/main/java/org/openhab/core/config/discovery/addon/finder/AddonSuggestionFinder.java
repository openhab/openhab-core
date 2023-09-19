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
package org.openhab.core.config.discovery.addon.finder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.UpnpService;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.discovery.addon.candidate.MdnsCandidate;
import org.openhab.core.config.discovery.addon.candidate.UpnpCandidate;
import org.openhab.core.config.discovery.addon.dto.SuggestedAddonCandidates;
import org.openhab.core.config.discovery.addon.xml.AddonCandidatesSerializer;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStreamException;

/**
 * This is a {@link AddonSuggestionFinder} which discovers suggested addons for
 * the user to install.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class AddonSuggestionFinder implements AutoCloseable {

    /**
     * Inner ServiceListener implementation that ignores call-backs.
     */
    private static class NoOp implements ServiceListener {

        @Override
        public void serviceAdded(@Nullable ServiceEvent event) {
        }

        @Override
        public void serviceRemoved(@Nullable ServiceEvent event) {
        }

        @Override
        public void serviceResolved(@Nullable ServiceEvent event) {
        }
    }

    private static final String FINDER_THREADPOOL_NAME = "addon-suggestion-finder";

    private final Logger logger = LoggerFactory.getLogger(AddonSuggestionFinder.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(FINDER_THREADPOOL_NAME);
    private final NoOp noop = new NoOp();
    private final Set<String> suggestedAddonUids = ConcurrentHashMap.newKeySet();
    private final List<MdnsCandidate> mdnsCandidates = new ArrayList<>();
    private final List<UpnpCandidate> upnpCandidates = new ArrayList<>();
    private final AddonCandidatesSerializer addonCandidatesSerializer = new AddonCandidatesSerializer();

    private final MDNSClient mdnsClient;
    private final UpnpService upnpService;

    @Activate
    public AddonSuggestionFinder(@Reference MDNSClient mdnsClient, @Reference UpnpService upnpService) {
        this.mdnsClient = mdnsClient;
        this.upnpService = upnpService;
    }

    @Override
    public void close() throws Exception {
        mdnsCandidates.forEach(candidate -> mdnsClient.removeServiceListener(candidate.getMdnsServiceType(), noop));
        mdnsCandidates.clear();
        upnpCandidates.clear();
        suggestedAddonUids.clear();
    }

    /**
     * Get the list of suggested addon Uids.
     */
    public List<String> getSuggestedAddonUids() {
        return suggestedAddonUids.stream().toList();
    }

    /**
     * Initialize the AddonSuggestionFinder with XML data containing the list of
     * potential addon candidates to be suggested.
     * 
     * @param xml an XML serial image.
     * @throws XStreamException if the object cannot be deserialized.
     */
    public void loadXML(String xml) throws XStreamException {
        try {
            close();
        } catch (Exception e) {
            // exception should not occur
        }
        SuggestedAddonCandidates candidates = addonCandidatesSerializer.fromXML(xml);
        candidates.getCandidates().stream()
                .forEach(candidate -> candidate.getDiscoveryMethods().stream().forEach(discoveryMethod -> {
                    switch (discoveryMethod.getServiceType()) {
                        case MDNS:
                            mdnsCandidates.add(new MdnsCandidate(candidate.getAddonUid(),
                                    discoveryMethod.getMatchProperties(), discoveryMethod.getMdnsServiceType()));
                            break;
                        case UPNP:
                            upnpCandidates.add(
                                    new UpnpCandidate(candidate.getAddonUid(), discoveryMethod.getMatchProperties()));
                            break;
                        default:
                            break;
                    }
                }));
        mdnsCandidates.forEach(candidate -> mdnsClient.addServiceListener(candidate.getMdnsServiceType(), noop));
    }

    /**
     * Run a scheduled task to search for matching addon suggestion candidates using
     * the MDNS service.
     */
    @SuppressWarnings("unlikely-arg-type")
    private void startMdnsScan() {
        scheduler.submit(() -> mdnsCandidates.forEach(candidate -> Arrays
                .stream(mdnsClient.list(candidate.getMdnsServiceType())).filter(service -> candidate.equals(service))
                .forEach(service -> suggestionFound(candidate.getAddonId()))));
    }

    /**
     * Start the search process to find addons to suggest to be installed.
     */
    public void startScan() {
        if (!mdnsCandidates.isEmpty()) {
            startMdnsScan();
        }
        if (!upnpCandidates.isEmpty()) {
            startUpnpScan();
        }
    }

    /**
     * Run a scheduled task to search for matching addon suggestion candidates using
     * the UPnP service.
     */
    @SuppressWarnings("unlikely-arg-type")
    private void startUpnpScan() {
        scheduler.submit(() -> upnpService.getRegistry().getRemoteDevices()
                .forEach(device -> upnpCandidates.stream().filter(candidate -> candidate.equals(device))
                        .forEach(candidate -> suggestionFound(candidate.getAddonId()))));
    }

    /**
     * Called back when a new addon suggestion is found.
     * 
     * @param addonUid the Uid of the found addon.
     */
    private synchronized void suggestionFound(String addonUid) {
        if (!suggestedAddonUids.contains(addonUid)) {
            logger.debug("found suggested addon id:{}", addonUid);
            suggestedAddonUids.add(addonUid);
        }
    }
}
