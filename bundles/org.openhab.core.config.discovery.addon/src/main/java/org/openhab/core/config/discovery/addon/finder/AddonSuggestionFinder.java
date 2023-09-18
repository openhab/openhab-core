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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

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
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletName;
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
@Component(immediate = true, service = Servlet.class)
@HttpWhiteboardServletName(AddonSuggestionFinder.SERVLET_PATH)
public class AddonSuggestionFinder extends HttpServlet implements AutoCloseable {

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

    public static final String SERVLET_PATH = "/suggestions";
    private static final long serialVersionUID = -358506179462414301L;
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
     * Process GET request by returning a comma separated list of suggested addon
     * ids.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(MediaType.TEXT_PLAIN);
        resp.getWriter().write(String.join(",", getSuggestions()));
    }

    /**
     * Process POST requests containing an XML payload which should contain the
     * suggestion candidates, load the XML payload, and finally start scanning for
     * respective suggestions.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getContentLength() <= 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "no content");
            return;
        }
        if (!MediaType.TEXT_XML.equals(req.getContentType())) {
            resp.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "content not xml");
            return;
        }
        try {
            loadXML(new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
            startScan();
        } catch (XStreamException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "content invalid");
            throw new ServletException(e);
        }
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
     * @param xml an XML serial image.
     * @throws XStreamException if the object cannot be deserialized.
     */
    public void loadXML(String xml) throws XStreamException {
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
