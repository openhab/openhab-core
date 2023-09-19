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
package org.openhab.core.config.discovery.addon.dto;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * DTO for serialization of a collection of potential suggested addon candidates.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class SuggestedAddonCandidates {
    private @Nullable List<Candidate> candidates;

    public List<Candidate> getCandidates() {
        List<Candidate> candidates = this.candidates;
        return candidates != null ? candidates : List.of();
    }

    public SuggestedAddonCandidates setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
        return this;
    }
}
