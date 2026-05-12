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
package org.openhab.core.thing.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.openhab.core.config.core.ConfigUtil;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;

/**
 * Tests for {@link BaseThingHandler}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock(value = "org.openhab.core.config.core.ConfigUtil", mode = ResourceAccessMode.READ_WRITE)
@NonNullByDefault
class BaseThingHandlerTest {

    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("test:type");
    private static final ThingUID THING_UID = new ThingUID(THING_TYPE_UID, "thing");

    private @NonNullByDefault({}) ThingHandlerCallback callback;
    private @NonNullByDefault({}) TestThingHandler handler;

    private static class TestThingHandler extends BaseThingHandler {
        public @Nullable Configuration configInInitialize = null;

        public TestThingHandler(Thing thing) {
            super(thing);
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
        }

        @Override
        public void initialize() {
            this.configInInitialize = getConfig();
        }

        @Override
        public Configuration getConfig() {
            return super.getConfig();
        }

        @Override
        public void updateConfiguration(Configuration configuration) {
            super.updateConfiguration(configuration);
        }

        @Override
        public void updateThing(Thing thing) {
            super.updateThing(thing);
        }
    }

    private static class ConfigUtilAccessor extends ConfigUtil {
        static void setEnv(Map<String, String> values) {
            setEnvProvider(values::get);
        }

        static void resetEnv() {
            setEnvProvider(System::getenv);
        }
    }

    @BeforeEach
    public void setup() throws Exception {
        callback = mock(ThingHandlerCallback.class);
        Thing thing = ThingBuilder.create(THING_TYPE_UID, THING_UID.getId())
                .withConfiguration(new Configuration(Map.of("p1", "${ENV:VAR}", "p2", "${ENV:FOO}"))).build();
        handler = new TestThingHandler(thing);
        handler.setCallback(callback);

        ConfigUtilAccessor.setEnv(Map.of("VAR", "resolved-var", "FOO", "resolved-foo", "BAR", "resolved-bar"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        ConfigUtilAccessor.resetEnv();
    }

    @Test
    public void testGetConfig() {
        Configuration config = handler.getConfig();
        assertEquals("resolved-var", config.get("p1"));
        assertEquals("resolved-foo", config.get("p2"));
    }

    public static class ConfigClass {
        public String p1 = "";
        public String p2 = "";
    }

    @Test
    public void testGetConfigAs() {
        ConfigClass config = handler.getConfig().as(ConfigClass.class);
        assertEquals("resolved-var", config.p1);
        assertEquals("resolved-foo", config.p2);
    }

    @Test
    public void testResolvedConfigAfterThingUpdate() {
        // Notify the handler: Thing updated with new configuration
        Thing thing = handler.editThing()
                .withConfiguration(new Configuration(Map.of("p1", "${ENV:FOO}", "p2", "${ENV:BAR}"))).build();
        handler.thingUpdated(thing);

        assertEquals("resolved-foo", handler.getConfig().get("p1"));
        assertEquals("resolved-bar", handler.getConfig().get("p2"));
    }

    @Test
    public void testResolvedConfigAfterUpdateThing() {
        // Action: Update the Thing with new configuration
        Thing newThing = ThingBuilder.create(THING_TYPE_UID, THING_UID.getId())
                .withConfiguration(new Configuration(Map.of("p1", "${ENV:FOO}", "p2", "${ENV:BAR}"))).build();
        handler.updateThing(newThing);

        assertEquals("resolved-foo", handler.getConfig().get("p1"));
        assertEquals("resolved-bar", handler.getConfig().get("p2"));
    }

    @Test
    public void testResolvedConfigAfterConfigurationUpdate() {
        // Action: Update the configuration
        Configuration newConfig = new Configuration(Map.of("p1", "${ENV:FOO}", "p2", "${ENV:BAR}"));
        handler.updateConfiguration(newConfig);

        assertEquals("resolved-foo", handler.getConfig().get("p1"));
        assertEquals("resolved-bar", handler.getConfig().get("p2"));
    }

    @Test
    public void testHandleConfigurationUpdate() {
        handler.handleConfigurationUpdate(Map.of("p1", "${ENV:VAR}_suffix"));

        assertEquals("resolved-var_suffix", handler.getConfig().get("p1"));
        assertEquals("resolved-foo", handler.getConfig().get("p2"));
    }
}
