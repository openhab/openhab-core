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
package org.openhab.core.automation.rest.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ThingActionsResource} - verifies the regular expression used in the REST path template for
 * executing thing actions, i.e. the accepted forms of an action UID.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class ThingActionsResourceTest {

    private static final Pattern ACTION_UID_PATTERN = Pattern.compile(ThingActionsResource.ACTION_UID_PATH_REGEX);

    @Test
    public void shouldMatchActionUidWithSingleSegmentScope() {
        assertTrue(ACTION_UID_PATTERN.matcher("wled.savePreset").matches());
    }

    @Test
    public void shouldMatchActionUidWithMultiSegmentScope() {
        assertTrue(ACTION_UID_PATTERN.matcher("mqtt.awtrixlight.reboot").matches());
        assertTrue(ACTION_UID_PATTERN.matcher("mqtt.espmilighthub.reboot").matches());
    }

    @Test
    public void shouldMatchActionUidWithSignatureHash() {
        assertTrue(ACTION_UID_PATTERN.matcher("wled.savePreset%230123456789abcdef").matches());
        assertTrue(ACTION_UID_PATTERN.matcher("mqtt.awtrixlight.reboot%230123456789abcdef").matches());
    }

    @Test
    public void shouldMatchActionUidWithHyphenatedSegments() {
        assertTrue(ACTION_UID_PATTERN.matcher("scope-name.action-type").matches());
    }

    @Test
    public void shouldNotMatchActionUidWithoutSeparatorDot() {
        assertFalse(ACTION_UID_PATTERN.matcher("savePreset").matches());
    }

    @Test
    public void shouldNotMatchMalformedActionUid() {
        assertFalse(ACTION_UID_PATTERN.matcher("mqtt.awtrixlight.").matches());
        assertFalse(ACTION_UID_PATTERN.matcher(".savePreset").matches());
        assertFalse(ACTION_UID_PATTERN.matcher("").matches());
    }
}
