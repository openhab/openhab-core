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
package org.openhab.core.config.discovery.addon.finders;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.AddonInfo;

/**
 * This is a {@link AddonSuggestionFinder} interface for classes that find Addons that are suggested to be installed.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public interface AddonSuggestionFinder {

    /**
     * The OH framework calls this method to scan through the candidate list of {@link AddonInfo} and return a subset of
     * those that it suggests to be installed.
     */
    public Set<AddonInfo> getSuggestedAddons();

    /**
     * The OH framework calls this method to provide a list of {@link AddonInfo} elements which contain potential
     * candidates that this finder can iterate over in order to detect which ones to return via the
     * {@code getSuggestedAddons()} method.
     * 
     * @param candidates a list of AddonInfo candidates.
     */
    public void setAddonCandidates(List<AddonInfo> candidates);
}
