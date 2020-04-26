/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.items.dto;

import java.util.List;
import java.util.Set;

/**
 * This is a data transfer object that is used to serialize items.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Andre Fuechsel - added tag support
 */
public class ItemDTO {

    public String type;
    public String name;
    public String label;
    public String category;
    public Set<String> tags;
    public List<String> groupNames;

    public ItemDTO() {
    }
}
