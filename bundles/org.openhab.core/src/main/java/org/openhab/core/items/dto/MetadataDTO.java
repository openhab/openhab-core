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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a data transfer object that is used to serialize metadata for a certain namespace and item.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class MetadataDTO {

    public @Nullable String value;
    public @Nullable Map<String, Object> config;

}
