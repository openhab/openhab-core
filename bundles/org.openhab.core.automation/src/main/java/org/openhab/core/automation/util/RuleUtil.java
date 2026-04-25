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

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Rule;

/**
 * The {@link RuleUtil} class contains utility methods for {@link Rule} objects.
 *
 * This class cannot be instantiated, it only contains static methods.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class RuleUtil {

    private static final Pattern RULE_UID_PATTERN = Pattern.compile("\\S(?:[^/\\\\]*\\S)?",
            Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * Returns {@code true} if the specified UID is a valid rule UID, otherwise {@code false}.
     * <p>
     * A valid rule UID is any string that doesn't contain {@code /}, {@code \} and has no leading or trailing
     * whitespace.
     *
     * @param uid the UID of the rule to be checked.
     * @return {@code true} if the specified UID is a valid rule UID, {@code false} otherwise.
     */
    public static boolean isValidRuleUID(final String uid) {
        return RULE_UID_PATTERN.matcher(uid).matches();
    }

    /**
     * Ensures that the specified rule UID of is valid.
     * <p>
     * If the rule UID is invalid an {@link IllegalArgumentException} is thrown, otherwise this method returns
     * silently.
     * <p>
     * A valid rule UID is any string that doesn't contain {@code /}, {@code \} and has no leading or trailing
     * whitespace.
     *
     * @param uid the UID of the rule to be checked.
     * @throws IllegalArgumentException If the specified rule UID is invalid.
     */
    public static void assertValidRuleUID(String uid) throws IllegalArgumentException {
        if (!isValidRuleUID(uid)) {
            throw new IllegalArgumentException("The specified UID of rule '" + uid + "' is invalid!");
        }
    }
}
