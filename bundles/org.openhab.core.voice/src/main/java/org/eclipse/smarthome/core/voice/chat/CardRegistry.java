/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.voice.chat;

import java.util.Collection;
import java.util.Set;

import org.eclipse.smarthome.core.common.registry.Registry;
import org.eclipse.smarthome.core.voice.internal.chat.CardProvider;

/**
 * {@link CardRegistry} tracks all {@link Card}s supplied by {@link CardProvider} and provides access to them.
 *
 * @author Laurent Garnier - Initial contribution
 */
public interface CardRegistry extends Registry<Card, String> {

    /**
     * Returns cards having ALL the specified tags
     *
     * @param tags the tags to lookup
     * @return matching cards
     */
    public Collection<Card> getCardByTags(Set<String> tags);

    /**
     * Returns cards matching the specified object and/or location attribute(s)
     *
     * @param object optional object attribute
     * @param location optional location attribute
     * @return matching cards - if one of the 2 arguments is not or empty, matching cards do NOT have the attribute. If
     *         both are provided, matching cards have both.
     */
    public Collection<Card> getCardMatchingAttributes(String object, String location);

    /**
     * Returns the most recent cards according to their timestamp
     *
     * @param skip number of elements to skip, for paging
     * @param count number of elements to retrieve, for paging (default 10)
     * @return the recent cards, in decreasing order of age
     */
    public Collection<Card> getRecent(int skip, int count);

    /**
     * Returns all the cards that are not ephemeral
     *
     * @return the non-ephemeral cards
     */
    public Collection<Card> getNonEphemeral();

}
