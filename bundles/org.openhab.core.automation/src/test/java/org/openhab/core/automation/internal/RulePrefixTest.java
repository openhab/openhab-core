/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.automation.internal;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.RulePredicates;

/**
 * Testing the prefix functionality.
 *
 * @author Victor Toni - Initial contribution
 */
@NonNullByDefault
public class RulePrefixTest {

    private static final String TESTING_PREFIX = "Testing";

    /**
     * Testing Rules without UID / without prefix / empty prefix.
     *
     * @see RulePredicates#PREFIX_SEPARATOR
     *
     */
    @Test
    public void testEmptyPrefix() {
        final RuleImpl rule0 = new RuleImpl(null);
        assertNotNull(rule0.getUID(), "Returned UID is null instead of generated one");
        assertNull(RulePredicates.getPrefix(rule0), "Returned a prefix instead of null");

        final String somethingWithoutSeparator = "something_without_separator";
        final RuleImpl rule1 = new RuleImpl(somethingWithoutSeparator);
        assertEquals(somethingWithoutSeparator, rule1.getUID(), "Returned wrong UID");
        assertNull(RulePredicates.getPrefix(rule1), "Returned a prefix instead of null");

        final String withSeparatorButEmpty = RulePredicates.PREFIX_SEPARATOR + "with_separator_but_empty";
        final RuleImpl rule2 = new RuleImpl(withSeparatorButEmpty);
        assertEquals(withSeparatorButEmpty, rule2.getUID(), "Returned wrong UID");
        assertNull(RulePredicates.getPrefix(rule2), "Returned a prefix instead of null");
    }

    /**
     * Testing Rules with manually created prefix / empty parts after the separator / multiple separators.
     *
     * @see RulePredicates#PREFIX_SEPARATOR
     *
     */
    @Test
    public void testManualPrefix() {
        final String testingPrefixPrefix = TESTING_PREFIX + RulePredicates.PREFIX_SEPARATOR;

        final String someName = "someName";
        final RuleImpl rule0 = new RuleImpl(testingPrefixPrefix + someName);
        assertEquals(TESTING_PREFIX, RulePredicates.getPrefix(rule0), "Returned wrong prefix");
        assertEquals(testingPrefixPrefix + someName, rule0.getUID(), "Returned wrong UID");

        final String multipleSeparatorName = RulePredicates.PREFIX_SEPARATOR + "nameBetweenSeparator"
                + RulePredicates.PREFIX_SEPARATOR;
        final RuleImpl rule1 = new RuleImpl(testingPrefixPrefix + multipleSeparatorName);
        assertEquals(TESTING_PREFIX, RulePredicates.getPrefix(rule1), "Returned wrong prefix");
        assertEquals(testingPrefixPrefix + someName, rule0.getUID(), "Returned wrong UID");

        final String emptyName = "";
        final RuleImpl rule2 = new RuleImpl(testingPrefixPrefix + emptyName);
        assertEquals(TESTING_PREFIX, RulePredicates.getPrefix(rule2), "Returned wrong prefix");
        assertEquals(testingPrefixPrefix + emptyName, rule2.getUID(), "Returned wrong UID");
    }
}
