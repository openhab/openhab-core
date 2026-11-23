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
package org.openhab.core.internal.service;

import static org.eclipse.jdt.annotation.Checks.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openhab.core.internal.items.MetadataCommandDescriptionProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandDescriptionBuilder;
import org.openhab.core.types.CommandDescriptionProvider;
import org.openhab.core.types.CommandOption;
import org.osgi.framework.Constants;

/**
 * Tests for {@link CommandDescriptionServiceImpl}.
 *
 * @author Cody Cutrer - Initial contribution
 */
@NonNullByDefault
class CommandDescriptionServiceImplTest {

    private static final String ITEM_NAME = "Trigger";
    private static final CommandDescription CHANNEL_DESCRIPTION = CommandDescriptionBuilder.create()
            .withCommandOption(new CommandOption("PERFORM", "PERFORM")).build();
    private static final CommandDescription METADATA_DESCRIPTION = CommandDescriptionBuilder.create()
            .withCommandOption(new CommandOption("PERFORM", "Start cleaning")).build();

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void metadataOverridesChannelRegardlessOfRegistrationOrder(boolean metadataFirst) {
        MetadataRegistry registry = mock(MetadataRegistry.class);
        MetadataKey key = new MetadataKey("commandDescription", ITEM_NAME);
        when(registry.get(key)).thenReturn(new Metadata(key, "", Map.of("options", "PERFORM=Start cleaning")));
        CommandDescriptionProvider metadata = new MetadataCommandDescriptionProvider(registry, Map.of());
        CommandDescriptionProvider channel = (itemName, locale) -> CHANNEL_DESCRIPTION;
        CommandDescriptionServiceImpl service = new CommandDescriptionServiceImpl();
        service.addCommandDescriptionProvider(metadataFirst ? metadata : channel);
        service.addCommandDescriptionProvider(metadataFirst ? channel : metadata);
        StringItem item = new StringItem(ITEM_NAME);
        item.setCommandDescriptionService(service);

        assertEquals(METADATA_DESCRIPTION.getCommandOptions(),
                requireNonNull(item.getCommandDescription()).getCommandOptions());

        when(registry.get(key)).thenReturn(null);
        assertSame(CHANNEL_DESCRIPTION, item.getCommandDescription());

        when(registry.get(key)).thenReturn(new Metadata(key, "", Map.of("options", "PERFORM=Clean again")));
        assertEquals(List.of(new CommandOption("PERFORM", "Clean again")),
                requireNonNull(item.getCommandDescription()).getCommandOptions());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void highestRankWinsRegardlessOfRegistrationOrder(boolean higherFirst) {
        CommandDescriptionProvider lower = new RankedProvider(Integer.MIN_VALUE, CHANNEL_DESCRIPTION);
        CommandDescriptionProvider higher = new RankedProvider(Integer.MAX_VALUE, METADATA_DESCRIPTION);
        CommandDescriptionServiceImpl service = new CommandDescriptionServiceImpl();
        service.addCommandDescriptionProvider(higherFirst ? higher : lower);
        service.addCommandDescriptionProvider(higherFirst ? lower : higher);

        assertSame(METADATA_DESCRIPTION, service.getCommandDescription(ITEM_NAME, null));
    }

    @Test
    void equalRanksPreserveRegistrationOrderAndProviders() {
        CommandDescriptionProvider first = (itemName, locale) -> CHANNEL_DESCRIPTION;
        CommandDescriptionProvider second = (itemName, locale) -> METADATA_DESCRIPTION;
        CommandDescriptionServiceImpl service = new CommandDescriptionServiceImpl();
        service.addCommandDescriptionProvider(first);
        service.addCommandDescriptionProvider(second);
        assertSame(CHANNEL_DESCRIPTION, service.getCommandDescription(ITEM_NAME, null));

        service.removeCommandDescriptionProvider(first);
        assertSame(METADATA_DESCRIPTION, service.getCommandDescription(ITEM_NAME, null));
        service.removeCommandDescriptionProvider(second);
        assertNull(service.getCommandDescription(ITEM_NAME, null));
    }

    @Test
    void fallsBackWhenHigherRankedProviderHasNoDescription() {
        CommandDescriptionServiceImpl service = new CommandDescriptionServiceImpl();
        service.addCommandDescriptionProvider(new RankedProvider(10, null));
        service.addCommandDescriptionProvider((itemName, locale) -> CHANNEL_DESCRIPTION);

        assertSame(CHANNEL_DESCRIPTION, service.getCommandDescription(ITEM_NAME, null));
    }

    @Test
    void lateArrivalRemovalAndReplacement() {
        CommandDescriptionServiceImpl service = new CommandDescriptionServiceImpl();
        assertNull(service.getCommandDescription(ITEM_NAME, null));
        service.addCommandDescriptionProvider((itemName, locale) -> CHANNEL_DESCRIPTION);
        CommandDescriptionProvider higher = new RankedProvider(1, METADATA_DESCRIPTION);
        service.addCommandDescriptionProvider(higher);
        assertSame(METADATA_DESCRIPTION, service.getCommandDescription(ITEM_NAME, null));

        service.removeCommandDescriptionProvider(higher);
        assertSame(CHANNEL_DESCRIPTION, service.getCommandDescription(ITEM_NAME, null));
        service.addCommandDescriptionProvider(new RankedProvider(1, METADATA_DESCRIPTION));
        assertSame(METADATA_DESCRIPTION, service.getCommandDescription(ITEM_NAME, null));
    }

    @Test
    void metadataHonorsConfiguredRanking() {
        MetadataRegistry registry = mock(MetadataRegistry.class);
        MetadataKey key = new MetadataKey("commandDescription", ITEM_NAME);
        when(registry.get(key)).thenReturn(new Metadata(key, "", Map.of("options", "PERFORM=Start cleaning")));
        CommandDescriptionServiceImpl service = new CommandDescriptionServiceImpl();
        service.addCommandDescriptionProvider(
                new MetadataCommandDescriptionProvider(registry, Map.of(Constants.SERVICE_RANKING, -1)));
        service.addCommandDescriptionProvider((itemName, locale) -> CHANNEL_DESCRIPTION);

        assertSame(CHANNEL_DESCRIPTION, service.getCommandDescription(ITEM_NAME, null));
    }

    private record RankedProvider(int rank,
            @Nullable CommandDescription description) implements CommandDescriptionProvider {
        @Override
        public Integer getRank() {
            return rank;
        }

        @Override
        public @Nullable CommandDescription getCommandDescription(String itemName, @Nullable Locale locale) {
            return description;
        }
    }
}
