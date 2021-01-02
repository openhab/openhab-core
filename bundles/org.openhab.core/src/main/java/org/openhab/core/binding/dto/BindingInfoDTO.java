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
package org.openhab.core.binding.dto;

/**
 * This is a data transfer object that is used to serialize binding info objects.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class BindingInfoDTO {

    public String author;
    public String description;
    public String id;
    public String name;
    public String configDescriptionURI;

    public BindingInfoDTO() {
    }

    public BindingInfoDTO(String id, String name, String author, String description, String configDescriptionURI) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.description = description;
        this.configDescriptionURI = configDescriptionURI;
    }
}
