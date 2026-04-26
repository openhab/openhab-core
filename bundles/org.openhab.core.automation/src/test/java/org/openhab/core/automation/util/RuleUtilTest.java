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
package org.openhab.core.automation.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.core.automation.util.RuleUtil.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class RuleUtilTest {

    @Test
    public void testIsValidRuleUID() {
        assertFalse(isValidRuleUID(""));
        assertTrue(isValidRuleUID("a"));
        assertTrue(isValidRuleUID("a b"));
        assertTrue(isValidRuleUID("OH™"));
        assertFalse(isValidRuleUID("/OH™"));
        assertFalse(isValidRuleUID("\\OH™"));
        assertFalse(isValidRuleUID("OH/™"));
        assertFalse(isValidRuleUID("OH\\™"));
        assertFalse(isValidRuleUID("OH™/"));
        assertFalse(isValidRuleUID("OH™\\"));
        assertTrue(isValidRuleUID("rule:xania:❣⟺⌘:3"));
        assertFalse(isValidRuleUID("rule:xania:❣⟺⌘:3\t"));
        assertFalse(isValidRuleUID("\nnope"));
        assertFalse(isValidRuleUID(" a"));
        assertFalse(isValidRuleUID("a "));
    }

    @Test
    public void testAssertValidRuleUID() {
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID(""));
        assertDoesNotThrow(() -> assertValidRuleUID("a"));
        assertDoesNotThrow(() -> assertValidRuleUID("a b"));
        assertDoesNotThrow(() -> assertValidRuleUID("OH™"));
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID("/OH™"));
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID("\\OH™"));
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID("OH/™"));
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID("OH\\™"));
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID("OH™/"));
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID("OH™\\"));
        assertDoesNotThrow(() -> assertValidRuleUID("rule:xania:❣⟺⌘:3"));
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID("rule:xania:❣⟺⌘:3\t"));
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID("\nnope"));
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID(" a"));
        assertThrows(IllegalArgumentException.class, () -> assertValidRuleUID("a "));
    }
}
