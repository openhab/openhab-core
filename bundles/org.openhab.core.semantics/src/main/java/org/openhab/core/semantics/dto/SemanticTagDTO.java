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
package org.openhab.core.semantics.dto;

import java.util.List;

/**
 * This is a data transfer object that is used to serialize semantic tags.
 *
 * @author Laurent Garnier - Initial contribution
 */
public class SemanticTagDTO {

    public String uid;
    public String label;
    public String description;
    public List<String> synonyms;

    public SemanticTagDTO() {
    }
}
