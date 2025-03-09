/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.fileformat;

import java.util.Map;

/**
 * This is a data transfer object that is used to serialize a metadata.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class MetadataDTO {

    public String itemName;
    public String namespace;
    public String value;
    public Map<String, Object> configuration;

    public MetadataDTO(String itemName, String namespace, String value, Map<String, Object> configuration) {
        this.itemName = itemName;
        this.namespace = namespace;
        this.value = value;
        this.configuration = configuration;
    }
}
