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
package org.openhab.core.hli;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Registry;
import org.openhab.core.events.EventPublisher;
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
 * @author Artur Fedjukevits - Refactored code
 */
@NonNullByDefault
@Component(service = CardRegistry.class, immediate = true)
public class CardRegistry extends AbstractRegistry<Card, String, CardProvider> {

    private static final Logger logger = LoggerFactory.getLogger(CardRegistry.class);
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_EPHEMERAL_CARDS = 10;

    /**
     * Comparator for sorting cards by timestamp in descending order (newest first).
     * Cards without timestamps are considered oldest.
     */
    private static final Comparator<Card> BY_TIMESTAMP_DESC = Comparator.<Card, Instant> comparing(
            card -> card.getTimestamp() != null ? card.getTimestamp().toInstant() : Instant.MIN).reversed();

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
        return filterCards(card -> cardHasTags(card, tags));
    }

    /**
     * Returns cards matching the specified object and/or location attribute(s)
     *
     * @param object optional object attribute
     * @param location optional location attribute
     * @return matching cards - if one of the 2 arguments is null or empty, matching cards do NOT have the attribute.
     *         If both are provided, matching cards have both.
     */
    public Collection<Card> getCardMatchingAttributes(@Nullable String object, @Nullable String location) {
        return filterCards(card -> cardMatchesAttributes(card, object, location));
    }

    @Override
    public Card add(Card element) {
        cleanupOldEphemeralCards();
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
        int limit = count < 1 ? DEFAULT_PAGE_SIZE : count;

        return getAll().stream().sorted(BY_TIMESTAMP_DESC).skip(skip).limit(limit).toList();
    }

    /**
     * Returns all the cards that are not ephemeral
     *
     * @return the non-ephemeral cards
     */
    public Collection<Card> getNonEphemeral() {
        return filterCards(card -> !card.isEphemeral());
    }

    /**
     * Returns all ephemeral cards
     *
     * @return the ephemeral cards
     */
    public Collection<Card> getEphemeral() {
        return filterCards(Card::isEphemeral);
    }

    /**
     * Returns the count of cards matching the given predicate
     *
     * @param predicate the filter condition
     * @return the count of matching cards
     */
    public long countCards(Predicate<Card> predicate) {
        return getAll().stream().filter(predicate).count();
    }

    /**
     * Returns the count of all cards
     *
     * @return the total count of cards
     */
    public long getCardCount() {
        return getAll().size();
    }

    /**
     * Checks if any card exists matching the given predicate
     *
     * @param predicate the filter condition
     * @return true if at least one card matches the predicate
     */
    public boolean hasCards(Predicate<Card> predicate) {
        return getAll().stream().anyMatch(predicate);
    }

    /**
     * Generic filter method for cards
     *
     * @param predicate the filter condition
     * @return filtered collection of cards
     */
    private Collection<Card> filterCards(Predicate<Card> predicate) {
        return getAll().stream().filter(predicate).toList();
    }

    /**
     * Removes old ephemeral cards to keep only the most recent ones
     */
    private void cleanupOldEphemeralCards() {
        var oldEphemeralCards = getAll().stream().filter(Card::isEphemeral).sorted(BY_TIMESTAMP_DESC)
                .skip(MAX_EPHEMERAL_CARDS).toList();

        oldEphemeralCards.forEach(card -> {
            logger.debug("Removing old ephemeral card {}", card.getUID());
            remove(card.getUID());
        });
    }

    /**
     * NOTE: unlike the ItemRegistry this method returns true only if the card has ALL the provided tags!
     */
    private boolean cardHasTags(Card card, @Nullable Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return true; // No tags specified means match all cards
        }

        var cardTags = card.getTags();
        return cardTags != null && cardTags.containsAll(tags);
    }

    /**
     * Checks if a card matches the specified object and location attributes.
     * Uses XOR logic: if attribute is null/empty, card should NOT have it; if provided, card should have it.
     */
    private boolean cardMatchesAttributes(Card card, @Nullable String object, @Nullable String location) {
        boolean objectMatches = isNullOrEmpty(object) != card.hasObjectAttribute(object);
        boolean locationMatches = isNullOrEmpty(location) != card.hasLocationAttribute(location);

        return objectMatches && locationMatches;
    }

    /**
     * Utility method to check if a string is null or empty
     */
    private boolean isNullOrEmpty(@Nullable String value) {
        return value == null || value.isEmpty();
    }

    // OSGi Component References - Maintaining backward compatibility

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
