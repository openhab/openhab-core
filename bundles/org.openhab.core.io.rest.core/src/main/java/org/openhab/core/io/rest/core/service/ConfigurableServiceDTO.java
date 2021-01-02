/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.service;

/**
 * {@link ConfigurableServiceDTO} is a data transfer object for configurable services.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Stefan Triller - added multiple field
 */
public class ConfigurableServiceDTO {

    public String id;
    public String label;
    public String category;
    public String configDescriptionURI;
    public boolean multiple;

    public ConfigurableServiceDTO(String id, String label, String category, String configDescriptionURI,
            boolean multiple) {
        this.id = id;
        this.label = label;
        this.category = category;
        this.configDescriptionURI = configDescriptionURI;
        this.multiple = multiple;
    }
}
