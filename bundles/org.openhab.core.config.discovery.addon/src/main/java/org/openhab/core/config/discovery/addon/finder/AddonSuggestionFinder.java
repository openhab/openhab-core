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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.addon.discovery.AddonSuggestionParticipant;
import org.openhab.core.config.discovery.addon.discovery.MdnsAddonSuggestionParticipant;
import org.openhab.core.config.discovery.addon.discovery.UpnpAddonSuggestionParticipant;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link AddonSuggestionFinder} which discovers suggested bindings to install.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component
public class AddonSuggestionFinder implements AddonSuggestionListener {

	private final Logger logger = LoggerFactory.getLogger(AddonSuggestionFinder.class);

	private final Set<String> bindingIds = new HashSet<>();
	private final List<AddonSuggestionParticipant> addonSuggestionParticipants = new ArrayList<>();
	
	protected void initialize() {
		AddonSuggestionParticipant mdnsParticipant = new MdnsAddonSuggestionParticipant(this, "aaa", Map.of(), "bbb");
		AddonSuggestionParticipant upnpParticipant = new UpnpAddonSuggestionParticipant(this, "ccc", Map.of());
		addonSuggestionParticipants.add(mdnsParticipant);
		addonSuggestionParticipants.add(upnpParticipant);
	}

	protected void dispose() {
		addonSuggestionParticipants.clear();
	}

	@Override
	public void onAddonSuggestionFound(String bindingId) {
		logger.debug("found binding id:{}", bindingId);
		bindingIds.add(bindingId);
	}
}
