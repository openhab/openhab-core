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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.UpnpService;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.config.discovery.addon.candidate.MdnsCandidate;
import org.openhab.core.config.discovery.addon.candidate.UpnpCandidate;
import org.openhab.core.config.discovery.addon.dto.Candidates;
import org.openhab.core.config.discovery.addon.xml.CandidatesSerializer;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link AddonSuggestionFinder} which discovers suggested addons for
 * the user to install.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component
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
    private final Set<String> addonIds = ConcurrentHashMap.newKeySet();
    private final List<MdnsCandidate> mdnsCandidates = new ArrayList<>();
    private final List<UpnpCandidate> upnpCandidates = new ArrayList<>();
    private final CandidatesSerializer candidatesSerializer = new CandidatesSerializer();

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
        addonIds.clear();
    }

    /**
     * Get the list of suggested addon ids.
     */
    public List<String> getSuggestions() {
        return addonIds.stream().toList();
    }

    /**
     * Initialize the AddonSuggestionFinder with XML data containing the list of
     * potential addon candidates to be suggested.
     * 
     * @param xml
     */
    public void loadXML(String xml) {
        try {
            close();
        } catch (Exception e) {
            // exception should not occur
        }
        Candidates candidates = candidatesSerializer.fromXML(xml);
        candidates.getCandidates().forEach(candidate -> {
            switch (candidate.getDiscoveryType()) {
                case MDNS:
                    mdnsCandidates.add(new MdnsCandidate(candidate.getAddonId(), candidate.getPropertyRegexMap(),
                            candidate.getMdnsServiceType()));
                    break;
                case UPNP:
                    upnpCandidates.add(new UpnpCandidate(candidate.getAddonId(), candidate.getPropertyRegexMap()));
                    break;
                default:
                    break;
            }
        });
        mdnsCandidates.forEach(candidate -> mdnsClient.addServiceListener(candidate.getMdnsServiceType(), noop));
    }

    /**
     * Run a scheduled task to search for matching addon suggestion candidates using
     * the MDNS service.
     */
    @SuppressWarnings("unlikely-arg-type")
    private void startMdnsScan() {
        scheduler.submit(() -> {
            mdnsCandidates.forEach(candidate -> {
                for (ServiceInfo service : mdnsClient.list(candidate.getMdnsServiceType())) {
                    if (candidate.equals(service)) {
                        suggestionFound(candidate.getAddonId());
                    }
                }
            });
        });
    }

    /**
     * Start the search process to find addons to suggest to be installed.
     */
    public void startScan() {
        startMdnsScan();
        startUpnpScan();
    }

    /**
     * Run a scheduled task to search for matching addon suggestion candidates using
     * the UPnP service.
     */
    @SuppressWarnings("unlikely-arg-type")
    private void startUpnpScan() {
        scheduler.submit(() -> {
            upnpService.getRegistry().getRemoteDevices().forEach(device -> {
                upnpCandidates.forEach(candidate -> {
                    if (candidate.equals(device)) {
                        suggestionFound(candidate.getAddonId());
                    }
                });
            });
        });
    }

    /**
     * Called back when a new addon suggestion is found.
     * 
     * @param addonId
     */
    private synchronized void suggestionFound(String addonId) {
        if (!addonIds.contains(addonId)) {
            logger.debug("found suggested addon id:{}", addonId);
            addonIds.add(addonId);
        }
    }
}
