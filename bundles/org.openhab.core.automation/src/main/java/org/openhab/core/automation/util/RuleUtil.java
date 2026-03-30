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

    private static final String RULE_UID_PATTERN = "[\\w-]+(:[\\w-]+)*";

    /**
     * Returns {@code true} if the specified uid is a valid rule UID, otherwise {@code false}.
     * <p>
     * A valid rule UID must contain one or several segments separated by the character ":".
     * Each segment must <i>only</i> consists of the following characters:
     * <ul>
     * <li>a-z</li>
     * <li>A-Z</li>
     * <li>0..9</li>
     * <li>_ (underscore)</li>
     * <li>- (hyphen)</li>
     * </ul>
     *
     * @param uid the UID of the rule to be checked
     * @return true if the specified uid is a valid rule UID, otherwise false
     */
    public static boolean isValidRuleUID(final String uid) {
        return uid.matches(RULE_UID_PATTERN);
    }

    /**
     * Ensures that the specified UID of the rule is valid.
     * <p>
     * If the UID of the rule is invalid an {@link IllegalArgumentException} is thrown, otherwise this method returns
     * silently.
     * <p>
     * A valid rule UID must contain one or several segments separated by the character ":".
     * Each segment must <i>only</i> consists of the following characters:
     * <ul>
     * <li>a-z</li>
     * <li>A-Z</li>
     * <li>0..9</li>
     * <li>_ (underscore)</li>
     * <li>- (hyphen)</li>
     * </ul>
     *
     * @param uid the UID of the rule to be checked
     * @throws IllegalArgumentException if the UID of the rule is invalid
     */
    public static void assertValidRuleUID(String uid) throws IllegalArgumentException {
        if (!isValidRuleUID(uid)) {
            throw new IllegalArgumentException("The specified UID of the rule '" + uid + "' is not valid!");
        }
    }
}
