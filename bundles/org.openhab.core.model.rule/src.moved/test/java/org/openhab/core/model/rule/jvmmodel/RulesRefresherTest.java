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
package org.openhab.core.model.rule.jvmmodel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.engine.action.ActionService;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.StartLevelService;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.binding.ThingActions;

/**
 * Tests rule refresh requests while the initial model reload is running.
 *
 * @author Leo Siepel - Initial contribution
 */
@NonNullByDefault
class RulesRefresherTest {
    private final ModelRepository modelRepository = mock(ModelRepository.class);
    private final ReadyService readyService = mock(ReadyService.class);
    private final ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final ReadyMarker modelReadyMarker = new ReadyMarker(StartLevelService.STARTLEVEL_MARKER_TYPE,
            Integer.toString(StartLevelService.STARTLEVEL_MODEL));

    private @NonNullByDefault({}) RulesRefresher refresher;

    @BeforeEach
    void setUp() {
        when(scheduler.schedule(any(Runnable.class), anyLong(), eq(TimeUnit.SECONDS))).thenAnswer(invocation -> {
            tasks.add(invocation.getArgument(0));
            return mock(ScheduledFuture.class);
        });
        refresher = new RulesRefresher(modelRepository, mock(ItemRegistry.class), mock(ThingRegistry.class),
                readyService, scheduler);
    }

    @AfterEach
    void tearDown() {
        refresher.deactivate();
    }

    @Test
    void thingActionArrivingDuringInitialReloadRequestsAnotherReload() {
        assertRefreshDuringInitialReload(r -> r.addThingActions(mock(ThingActions.class)));
    }

    @Test
    void scriptActionArrivingDuringInitialReloadRequestsAnotherReload() {
        assertRefreshDuringInitialReload(r -> r.addActionService(mock(ActionService.class)));
    }

    @Test
    void thingActionRemovedDuringInitialReloadRequestsAnotherReload() {
        assertRefreshDuringInitialReload(r -> r.removeThingActions(mock(ThingActions.class)));
    }

    @Test
    void scriptActionRemovedDuringInitialReloadRequestsAnotherReload() {
        assertRefreshDuringInitialReload(r -> r.removeActionService(mock(ActionService.class)));
    }

    @Test
    void actionsBeforeModelReadinessDoNotStartReloadingRules() {
        refresher.addThingActions(mock(ThingActions.class));
        refresher.addActionService(mock(ActionService.class));
        assertTrue(tasks.isEmpty());
        verifyNoInteractions(scheduler);
    }

    private void assertRefreshDuringInitialReload(Consumer<RulesRefresher> changeActions) {
        doAnswer(invocation -> {
            verify(readyService, never()).markReady(any());
            changeActions.accept(refresher);
            return null;
        }).doNothing().when(modelRepository).reloadAllModelsOfType("rules");

        refresher.onReadyMarkerAdded(modelReadyMarker);
        tasks.remove().run();

        assertEquals(1, tasks.size(), "Action changes during the first reload must request a follow-up reload");
        tasks.remove().run();
        verify(modelRepository, times(2)).reloadAllModelsOfType("rules");
        verify(readyService)
                .markReady(new ReadyMarker(RulesRefresher.RULES_REFRESH_MARKER_TYPE, RulesRefresher.RULES_REFRESH));
    }
}
