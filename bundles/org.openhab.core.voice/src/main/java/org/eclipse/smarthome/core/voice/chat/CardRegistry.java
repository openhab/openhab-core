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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.common.registry.AbstractRegistry;
import org.eclipse.smarthome.core.common.registry.Registry;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.voice.internal.chat.CardProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The @link {@link Registry} tracking for {@link Card} elements provided by the @link {@link CardProvider}
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(service = CardRegistry.class, immediate = true)
public class CardRegistry extends AbstractRegistry<Card, String, CardProvider> {

    private final Logger logger = LoggerFactory.getLogger(CardRegistry.class);

    public CardRegistry() {
        super(CardProvider.class);
    }

    /**
     * Returns cards having ALL the specified tags
     *
     * @param tags the tags to lookup
     * @return matching cards
     */
    public Collection<Card> getCardByTags(Set<String> tags) {
        List<Card> filteredCards = new ArrayList<Card>();
        for (Card card : getAll()) {
            if (cardHasTags(card, tags)) {
                filteredCards.add(card);
            }
        }
        return filteredCards;
    }

    /**
     * Returns cards matching the specified object and/or location attribute(s)
     *
     * @param object optional object attribute
     * @param location optional location attribute
     * @return matching cards - if one of the 2 arguments is not or empty, matching cards do NOT have the attribute. If
     *         both are provided, matching cards have both.
     */
    public Collection<Card> getCardMatchingAttributes(String object, String location) {
        List<Card> filteredCards = new ArrayList<Card>();
        for (Card card : getAll()) {
            if (cardMatchesAttributes(card, object, location)) {
                filteredCards.add(card);
            }
        }
        return filteredCards;
    }

    @Override
    public @NonNull Card add(@NonNull Card element) {
        // Remove old ephemeral cards
        Comparator<Card> byTimestamp = (e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp());
        List<Card> oldCards = getAll().stream().filter(card -> card.isEphemeral()).sorted(byTimestamp).skip(10)
                .collect(Collectors.toList());

        for (Card card : oldCards) {
            logger.debug("Removing old ephemeral card {}", card.getUID());
            remove(card.getUID());
        }

        return super.add(element);
    }

    /**
     * Returns the most recent cards according to their timestamp
     *
     * @param skip number of elements to skip, for paging
     * @param count number of elements to retrieve, for paging (default 10)
     * @return the recent cards, in decreasing order of age
     */
    public Collection<Card> getRecent(int skip, int count) {
        int limit = (count < 1) ? 10 : count;
        Comparator<Card> byTimestamp = (e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp());
        List<Card> recentCards = getAll().stream().sorted(byTimestamp).skip(skip).limit(limit)
                .collect(Collectors.toList());

        return recentCards;
    }

    /**
     * Returns all the cards that are not ephemeral
     *
     * @return the non-ephemeral cards
     */
    public Collection<Card> getNonEphemeral() {
        return getAll().stream().filter(c -> !c.isEphemeral()).collect(Collectors.toList());
    }

    /*
     * NOTE: unlike the ItemRegistry this method returns true only if the card has ALL the provided tags!
     */
    private boolean cardHasTags(Card card, Set<String> tags) {
        return (tags != null && card.getTags() != null && card.getTags().equals(tags));
    }

    private boolean cardMatchesAttributes(Card card, String object, String location) {
        boolean objectMatches = (object == null || object.isEmpty()) ^ card.hasObjectAttribute(object);
        boolean locationMatches = (location == null || location.isEmpty()) ^ card.hasLocationAttribute(location);

        return objectMatches && locationMatches;
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(CardProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(CardProvider provider) {
        super.unsetManagedProvider(provider);
    }

    @Override
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setEventPublisher(EventPublisher eventPublisher) {
        super.setEventPublisher(eventPublisher);
    }

    @Override
    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        super.unsetEventPublisher(eventPublisher);
    }
}
