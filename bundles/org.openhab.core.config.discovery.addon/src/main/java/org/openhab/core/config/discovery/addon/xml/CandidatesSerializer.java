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
import com.thoughtworks.xstream.XStreamException;
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
        xStream.alias("addon-suggestions", Candidates.class);
        xStream.alias("candidate", Candidate.class);
        xStream.alias("property", PropertyRegex.class);
        xStream.addImplicitCollection(Candidates.class, "candidates");
        xStream.addImplicitCollection(Candidate.class, "properties");
        xStream.allowTypesByWildcard(new String[] { "org.openhab.**" });
    }

    /**
     * Deserialize the XML into a Candidates DTO.
     * 
     * @param xml an XML serial image.
     * @return a deserialized Candidates DTO.
     * @throws XStreamException if unable to deserialize the XML.
     */
    public Candidates fromXML(String xml) throws XStreamException {
        return (Candidates) xStream.fromXML(xml);
    }

    /**
     * Serialize a Candidates DTO to XML.
     * 
     * @param candidates the DTO to be serialized.
     * @return an XML serial image of the DTO.
     * @throws XStreamException if unable to serialize the DTO.
     */
    public String toXML(Candidates candidates) throws XStreamException {
        return xStream.toXML(candidates);
    }
}
