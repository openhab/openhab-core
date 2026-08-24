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
package org.openhab.core.io.rest.sse.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.OutboundSseEvent.Builder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.io.rest.LocaleService;
import org.openhab.core.io.rest.sse.internal.dto.StateDTO;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.transform.util.ItemDisplayStateUtil;
import org.openhab.core.types.State;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SseItemStatesEventBuilder} builds {@link OutboundSseEvent}s for connections that listen to item state
 * changes.
 *
 * @author Yannick Schaus - Initial contribution
 * @author Wouter Born - Rework SSE item state sinks for dropping Glassfish
 */
@Component(service = SseItemStatesEventBuilder.class)
@NonNullByDefault
public class SseItemStatesEventBuilder {

    private final Logger logger = LoggerFactory.getLogger(SseItemStatesEventBuilder.class);

    private final ItemRegistry itemRegistry;
    private final LocaleService localeService;
    private final TimeZoneProvider timeZoneProvider;
    private final StartLevelService startLevelService;

    @Activate
    public SseItemStatesEventBuilder(final @Reference ItemRegistry itemRegistry,
            final @Reference LocaleService localeService, final @Reference TimeZoneProvider timeZoneProvider,
            final @Reference StartLevelService startLevelService) {
        this.itemRegistry = itemRegistry;
        this.localeService = localeService;
        this.timeZoneProvider = timeZoneProvider;
        this.startLevelService = startLevelService;
    }

    public @Nullable OutboundSseEvent buildEvent(Builder eventBuilder, Set<String> itemNames) {
        Map<String, StateDTO> payload = new HashMap<>(itemNames.size());
        for (String itemName : itemNames) {
            try {
                Item item = itemRegistry.getItem(itemName);
                StateDTO stateDto = new StateDTO();
                stateDto.state = item.getState().toString();
                stateDto.type = getStateType(item.getState());
                String displayState = getDisplayState(item, localeService.getLocale(null));
                // Only include the display state if it's different than the raw state
                if (stateDto.state != null && !stateDto.state.equals(displayState)) {
                    stateDto.displayState = displayState;
                }
                if (item.getState() instanceof DecimalType decimalState) {
                    stateDto.numericState = decimalState.floatValue();
                }
                if (item.getState() instanceof QuantityType quantityState) {
                    stateDto.numericState = quantityState.floatValue();
                    stateDto.unit = quantityState.getUnit().toString();
                }
                payload.put(itemName, stateDto);
            } catch (ItemNotFoundException e) {
                if (startLevelService.getStartLevel() >= StartLevelService.STARTLEVEL_MODEL) {
                    logger.warn("Attempting to send a state update of an item which doesn't exist: {}", itemName);
                }
            }
        }

        if (!payload.isEmpty()) {
            return eventBuilder.mediaType(MediaType.APPLICATION_JSON_TYPE).data(payload).build();
        }

        return null;
    }

    protected @Nullable String getDisplayState(Item item, Locale locale) {
        return ItemDisplayStateUtil.getDisplayState(item, locale, timeZoneProvider.getTimeZone());
    }

    // Taken from org.openhab.core.items.events.ItemEventFactory
    private static String getStateType(State state) {
        String stateClassName = state.getClass().getSimpleName();
        return stateClassName.substring(0, stateClassName.length() - "Type".length());
    }
}
