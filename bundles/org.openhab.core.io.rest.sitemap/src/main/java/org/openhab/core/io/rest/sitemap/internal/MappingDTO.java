/**
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
package org.openhab.core.io.rest.sitemap.internal;

/**
 * This is a data transfer object that is used to serialize command mappings.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Laurent Garnier - New fields position and icon
 * @author Laurent Garnier - Replace field position by fields row and column
 * @author Laurent Garnier - New field releaseCommand
 */
public class MappingDTO {

    public Integer row;
    public Integer column;
    public String command;
    public String releaseCommand;
    public String label;
    public String icon;

    public MappingDTO() {
    }
}
