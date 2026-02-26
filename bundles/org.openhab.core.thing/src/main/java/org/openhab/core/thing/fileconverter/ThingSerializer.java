/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.thing.fileconverter;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.ObjectSerializer;
import org.openhab.core.thing.Thing;

/**
 * {@link ThingSerializer} is the interface to implement by any file generator for {@link Thing} object.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface ThingSerializer extends ObjectSerializer<Thing> {

    /**
     * Specify the {@link List} of {@link Thing}s to serialize and associate them with an identifier.
     *
     * @param id the identifier of the {@link Thing} format generation.
     * @param things the {@link List} of {@link Thing}s to serialize.
     * @param hideDefaultChannels {@code true} to hide the non extensible channels having a default configuration.
     * @param hideDefaultParameters {@code true} to hide the configuration parameters having a default value.
     */
    void setThingsToBeGenerated(String id, List<Thing> things, boolean hideDefaultChannels,
            boolean hideDefaultParameters);
}
