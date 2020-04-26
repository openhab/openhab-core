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
package org.openhab.core.automation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This class add support for prefixes for {@link Rule} UIDs and provide default predicates for prefixes and tags.
 *
 * @author Victor Toni - Initial contribution
 */
public class RulePredicates {

    /**
     * Constant defining separator between prefix and UID.
     */
    public static final String PREFIX_SEPARATOR = ":";

    /**
     * Gets the prefix of the {@link Rule}'s UID, if any exist. The UID is either set automatically when the
     * {@link Rule} is added or by the creating party. It's an optional property.
     * <br/>
     * <br/>
     * Implementation note:
     * <br/>
     * The name space is part of the UID and the prefix thereof.
     * <br/>
     * If the UID does not contain a {@link PREFIX_SEPARATOR} {@code null} will be returned.
     * <br/>
     * If the UID does contain a {@link PREFIX_SEPARATOR} the prefix until the first occurrence will be returned.
     * <br/>
     * If the prefix would have a zero length {@code null} will be returned.
     *
     * @return prefix of this {@link Rule}, or {@code null} if no prefix or an empty prefix is found.
     */
    public static String getPrefix(Rule rule) {
        if (null != rule) {
            final String uid = rule.getUID();
            final int index = uid.indexOf(PREFIX_SEPARATOR);
            // only when a delimiter was found and the prefix is not empty
            if (0 < index) {
                return uid.substring(0, index);
            }
        }
        return null;
    }

    /**
     * Creates a {@link Predicate} which can be used to filter {@link Rule}s for a given prefix or {@code null} prefix.
     *
     * @param prefix to search for.
     * @return created {@link Predicate}.
     */
    public static Predicate<Rule> hasPrefix(final String prefix) {
        if (null == prefix) {
            return r -> null == getPrefix(r);
        } else {
            return r -> prefix.equals(getPrefix(r));
        }
    }

    /**
     * Creates a {@link Predicate} which can be used to match {@link Rule}s for any of the given prefixes and even
     * {@code null} prefix.
     *
     * @param prefixes to search for.
     * @return created {@link Predicate}.
     */
    public static Predicate<Rule> hasAnyOfPrefixes(String... prefixes) {
        final HashSet<String> namespaceSet = new HashSet<>(prefixes.length);
        for (final String namespace : prefixes) {
            namespaceSet.add(namespace);
        }

        // this will even work for null namespace
        return r -> namespaceSet.contains(getPrefix(r));
    }

    /**
     * Creates a {@link Predicate} which can be used to match {@link Rule}s with one or more tags.
     *
     * @param tags to search for.
     * @return created {@link Predicate}.
     */
    public static Predicate<Rule> hasTags() {
        // everything with a tag is matching
        // Rule.getTags() is never null
        return r -> !r.getTags().isEmpty();
    }

    /**
     * Creates a {@link Predicate} which can be used to match {@link Rule}s without tags.
     *
     * @param tags to search for.
     * @return created {@link Predicate}.
     */
    public static Predicate<Rule> hasNoTags() {
        // Rule.getTags() is never null
        return r -> r.getTags().isEmpty();
    }

    /**
     * Creates a {@link Predicate} which can be used to match {@link Rule}s with all given tags or no tags at all.
     * All given tags must match, (the matched {@code Rule} might contain more).
     *
     * @param tags to search for.
     * @return created {@link Predicate}.
     */
    public static Predicate<Rule> hasAllTags(final Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return (Predicate<Rule>) r -> true;
        } else {
            final Set<String> tagSet = new HashSet<>(tags);

            // everything containing _all_ given tags is matching
            // (Rule might might have more tags than the given set)
            return r -> r.getTags().containsAll(tagSet);
        }
    }

    /**
     * Creates a {@link Predicate} which can be used to match {@link Rule}s for all given tags or no tags at all.
     * All given tags must match, (the matched {@code Rule} might contain more).
     *
     * @param tags to search for.
     * @return created {@link Predicate}.
     */
    public static Predicate<Rule> hasAllTags(final String... tags) {
        return hasAllTags(tags == null ? null : Arrays.asList(tags));
    }

    /**
     * Creates a {@link Predicate} which can be used to match {@link Rule}s for any of the given tags or {@link Rule}s
     * without tags.
     *
     * @param tags to search for.
     * @return created {@link Predicate}.
     */
    public static Predicate<Rule> hasAnyOfTags(final Collection<String> tags) {
        if (null == tags || tags.isEmpty()) {
            // everything without a tag is matching
            return hasNoTags();
        } else {
            final Set<String> tagSet = new HashSet<>(tags);

            // everything containing _any_ of the given tags is matching (more than one tag might match)
            // if the collections are NOT disjoint, they have something in common
            return r -> !Collections.disjoint(r.getTags(), tagSet);
        }
    }

    /**
     * Creates a {@link Predicate} which can be used to match {@link Rule}s for any of the given tags or {@link Rule}s
     * without tags.
     *
     * @param tags to search for.
     * @return created {@link Predicate}.
     */
    public static Predicate<Rule> hasAnyOfTags(final String... tags) {
        if (null == tags || 0 == tags.length) {
            // everything without a tag is matching
            return hasNoTags();
        } else {
            return hasAnyOfTags(Arrays.asList(tags));
        }
    }
}
