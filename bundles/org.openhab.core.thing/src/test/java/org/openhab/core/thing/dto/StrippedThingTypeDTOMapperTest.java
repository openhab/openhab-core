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
package org.openhab.core.thing.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.thing.type.ThingTypeBuilder;

/**
 * Tests the {@link StrippedThingTypeDTOMapper}.
 *
 * @author Spectre - Initial contribution
 */
@NonNullByDefault
public class StrippedThingTypeDTOMapperTest {

    @Test
    public void bundleSymbolicNameIsMapped() {
        StrippedThingTypeDTO dto = StrippedThingTypeDTOMapper.map(ThingTypeBuilder.instance("binding", "type", "Label")
                .withBundleSymbolicName("org.openhab.binding.test").build(), Locale.ENGLISH);

        assertEquals("binding:type", dto.UID);
        assertEquals("org.openhab.binding.test", dto.bundleSymbolicName);
    }

    @Test
    public void bundleSymbolicNameIsNullWithoutBundle() {
        StrippedThingTypeDTO dto = StrippedThingTypeDTOMapper
                .map(ThingTypeBuilder.instance("binding", "type", "Label").build(), Locale.ENGLISH);

        assertNull(dto.bundleSymbolicName);
    }
}
