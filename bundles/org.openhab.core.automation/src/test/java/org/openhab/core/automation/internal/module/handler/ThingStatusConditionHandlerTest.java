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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.util.ConditionBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.events.ThingAddedEvent;
import org.openhab.core.thing.events.ThingEventFactory;
import org.openhab.core.thing.events.ThingRemovedEvent;
import org.osgi.framework.BundleContext;

/**
 * Basic unit tests for {@link ThingStatusConditionHandler}.
 *
 * @author Jörg Sautter - Initial contribution based on ItemStateConditionHandlerTest
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ThingStatusConditionHandlerTest extends JavaTest {

    public static class ParameterSet {
        public final Thing thing;
        public final String comparisonStatus;
        public final ThingStatus thingStatus;
        public final boolean expectedResult;

        public ParameterSet(String comparisonStatus, ThingStatus thingStatus, boolean expectedResult) {
            thing = ThingBuilder.create(new ThingTypeUID(BINDING_UID, THING_TYPE_UID), THING_UID).build();
            thing.setStatusInfo(new ThingStatusInfo(thingStatus, ThingStatusDetail.NONE, null));
            this.comparisonStatus = comparisonStatus;
            this.thingStatus = thingStatus;
            this.expectedResult = expectedResult;
        }
    }

    public static Collection<Object[]> equalsParameters() {
        return List.of(new Object[][] { //
                { new ParameterSet("UNINITIALIZED", ThingStatus.UNINITIALIZED, true) }, //
                { new ParameterSet("INITIALIZING", ThingStatus.UNINITIALIZED, false) }, //
                { new ParameterSet("OFFLINE", ThingStatus.UNKNOWN, false) }, //
                { new ParameterSet("OFFLINE", ThingStatus.ONLINE, false) }, //
                { new ParameterSet("OFFLINE", ThingStatus.OFFLINE, true) }, //
                { new ParameterSet("ONLINE", ThingStatus.ONLINE, true) }, //
                { new ParameterSet("ONLINE", ThingStatus.OFFLINE, false) } });
    }

    private static final String BINDING_UID = "binding";
    private static final String THING_TYPE_UID = "type";
    private static final String THING_UID = "myThing";

    private @NonNullByDefault({}) Thing thing;

    private @NonNullByDefault({}) @Mock ThingRegistry mockThingRegistry;
    private @NonNullByDefault({}) @Mock BundleContext mockBundleContext;

    @BeforeEach
    public void setup() {
        when(mockThingRegistry.get(new ThingUID(BINDING_UID, THING_TYPE_UID, THING_UID))).thenAnswer(i -> thing);
    }

    @ParameterizedTest
    @MethodSource("equalsParameters")
    public void testEqualsCondition(ParameterSet parameterSet) {
        thing = parameterSet.thing;
        ThingStatusConditionHandler handler = initThingStatusConditionHandler("=", parameterSet.comparisonStatus);

        if (parameterSet.expectedResult) {
            assertTrue(handler.isSatisfied(Map.of()),
                    parameterSet.thing + ", comparisonStatus=" + parameterSet.comparisonStatus);
        } else {
            assertFalse(handler.isSatisfied(Map.of()),
                    parameterSet.thing + ", comparisonStatus=" + parameterSet.comparisonStatus);
        }
    }

    @ParameterizedTest
    @MethodSource("equalsParameters")
    public void testNotEqualsCondition(ParameterSet parameterSet) {
        thing = parameterSet.thing;
        ThingStatusConditionHandler handler = initThingStatusConditionHandler("!=", parameterSet.comparisonStatus);

        if (parameterSet.expectedResult) {
            assertFalse(handler.isSatisfied(Map.of()));
        } else {
            assertTrue(handler.isSatisfied(Map.of()));
        }
    }

    private ThingStatusConditionHandler initThingStatusConditionHandler(String operator, String state) {
        Configuration configuration = new Configuration();
        configuration.put(ThingStatusConditionHandler.CFG_THING_UID,
                BINDING_UID + ":" + THING_TYPE_UID + ":" + THING_UID);
        configuration.put(ThingStatusConditionHandler.CFG_OPERATOR, operator);
        configuration.put(ThingStatusConditionHandler.CFG_STATUS, state);
        ConditionBuilder builder = ConditionBuilder.create() //
                .withId("conditionId") //
                .withTypeUID(ThingStatusConditionHandler.THING_STATUS_CONDITION) //
                .withConfiguration(configuration);
        return new ThingStatusConditionHandler(builder.build(), "", mockBundleContext, mockThingRegistry);
    }

    @Test
    public void thingMessagesAreLogged() {
        Configuration configuration = new Configuration();
        configuration.put(ThingStatusConditionHandler.CFG_THING_UID,
                BINDING_UID + ":" + THING_TYPE_UID + ":" + THING_UID);
        configuration.put(ThingStatusConditionHandler.CFG_OPERATOR, "=");
        Condition condition = ConditionBuilder.create() //
                .withId("conditionId") //
                .withTypeUID(ThingStatusConditionHandler.THING_STATUS_CONDITION) //
                .withConfiguration(configuration) //
                .build();

        setupInterceptedLogger(ThingStatusConditionHandler.class, LogLevel.INFO);

        // missing on creation
        when(mockThingRegistry.get(new ThingUID(BINDING_UID, THING_TYPE_UID, THING_UID))).thenReturn(null);
        ThingStatusConditionHandler handler = new ThingStatusConditionHandler(condition, "foo", mockBundleContext,
                mockThingRegistry);
        assertLogMessage(ThingStatusConditionHandler.class, LogLevel.WARN,
                "Thing 'binding:type:myThing' needed for rule 'foo' is missing. Condition 'conditionId' will not work.");

        thing = ThingBuilder.create(new ThingTypeUID(BINDING_UID, THING_TYPE_UID), THING_UID).build();

        // added later
        ThingAddedEvent addedEvent = ThingEventFactory.createAddedEvent(thing);
        assertTrue(handler.getEventFilter().apply(addedEvent));
        handler.receive(addedEvent);
        assertLogMessage(ThingStatusConditionHandler.class, LogLevel.INFO,
                "Thing 'binding:type:myThing' needed for rule 'foo' added. Condition 'conditionId' will now work.");

        // removed later
        ThingRemovedEvent removedEvent = ThingEventFactory.createRemovedEvent(thing);
        assertTrue(handler.getEventFilter().apply(removedEvent));
        handler.receive(removedEvent);
        assertLogMessage(ThingStatusConditionHandler.class, LogLevel.WARN,
                "Thing 'binding:type:myThing' needed for rule 'foo' removed. Condition 'conditionId' will no longer work.");
    }
}
