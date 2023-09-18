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
package org.openhab.core.config.discovery.addon.xml;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.addon.dto.Candidate;
import org.openhab.core.config.discovery.addon.dto.Candidates;
import org.openhab.core.config.discovery.addon.dto.PropertyRegex;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * Serializer/deserializer for addon suggestion finder.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class CandidatesSerializer {

    private final XStream xStream;

    public CandidatesSerializer() {
        xStream = new XStream(new StaxDriver());
        xStream.alias("addon-suggestion", Candidates.class);
        xStream.alias("candidate", Candidate.class);
        xStream.alias("property", PropertyRegex.class);
        xStream.addImplicitCollection(Candidates.class, "candidates");
        xStream.addImplicitCollection(Candidate.class, "properties");
        xStream.allowTypesByWildcard(new String[] { "org.openhab.**" });
    }

    public Candidates fromXML(String xml) {
        return (Candidates) xStream.fromXML(xml);
    }

    public String toXML(Candidates candidates) {
        return xStream.toXML(candidates);
    }
}
