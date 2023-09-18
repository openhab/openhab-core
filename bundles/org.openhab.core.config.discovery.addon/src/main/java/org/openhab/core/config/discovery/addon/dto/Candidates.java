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

/**
 * DTO for serialization of addon suggestion candidates.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class Candidates {
    private @NonNullByDefault({}) List<Candidate> candidates;

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public Candidates setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
        return this;
    }
}
