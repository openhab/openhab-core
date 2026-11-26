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
package org.openhab.core.automation.internal.module.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;

/**
 * Exercises the conversion of configured values and rule inputs into item state events.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class ItemStateUpdateActionHandlerTest {

    private static final String ITEM = "TestSwitch";

    private final EventPublisher publisher = mock(EventPublisher.class);
    private final ItemRegistry registry = mock(ItemRegistry.class);

    @BeforeEach
    public void setUp() throws ItemNotFoundException {
        when(registry.getItem(ITEM)).thenReturn(new SwitchItem(ITEM));
    }

    @Test
    public void postsTheConfiguredStateInPreferenceToTheRuleInput() {
        handler(Map.of("itemName", ITEM, "state", "OFF")).execute(Map.of("state", OnOffType.ON));

        assertPostedState(OnOffType.OFF);
    }

    @Test
    public void postsACompatibleStateFromTheRuleInput() {
        handler(Map.of("itemName", ITEM)).execute(Map.of("state", OnOffType.ON));

        assertPostedState(OnOffType.ON);
    }

    @Test
    public void invalidConfiguredStateDoesNotFallBackToTheRuleInput() {
        handler(Map.of("itemName", ITEM, "state", "OPEN")).execute(Map.of("state", OnOffType.ON));

        verifyNoInteractions(publisher);
    }

    @Test
    public void incompatibleRuleInputDoesNotPublishAStateUpdate() {
        handler(Map.of("itemName", ITEM)).execute(Map.of("state", new StringType("ON")));

        verifyNoInteractions(publisher);
    }

    private ItemStateUpdateActionHandler handler(Map<String, Object> configuration) {
        Action action = ModuleBuilder.createAction().withId("update").withTypeUID("core.ItemStateUpdateAction")
                .withConfiguration(new Configuration(configuration)).build();
        return new ItemStateUpdateActionHandler(action, publisher, registry);
    }

    private void assertPostedState(OnOffType state) {
        ArgumentCaptor<ItemStateEvent> event = ArgumentCaptor.forClass(ItemStateEvent.class);
        verify(publisher).post(event.capture());
        assertEquals(ITEM, event.getValue().getItemName());
        assertEquals(state, event.getValue().getItemState());
    }
}
