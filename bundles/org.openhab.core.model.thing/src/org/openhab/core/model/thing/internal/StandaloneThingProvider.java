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
package org.openhab.core.model.thing.internal;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Thing;

/**
 * {@link StandaloneThingProvider} is the interface to implement by a {@link Thing} provider that is able
 * to create a list of things from a model without impacting the thing registry.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface StandaloneThingProvider {

    /**
     * Parse the provided syntax and return the corresponding {@link Thing} objects without impacting
     * the thing registry.
     *
     * @param modelName the model name
     * @return the list of corresponding {@link Thing}
     */
    List<Thing> getThingsFromStandaloneModel(String modelName);
}
