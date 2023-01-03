/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.config.discovery;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.framework.Bundle;

/**
 * Tests the {@link DiscoveryResultBuilder}.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class AbstractDiscoveryServiceTest implements DiscoveryListener {

    private static final String BINDING_ID = "bindingId";
    private static final ThingUID BRIDGE_UID = new ThingUID(new ThingTypeUID(BINDING_ID, "bridgeTypeId"), "bridgeId");
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID(BINDING_ID, "thingTypeId");
    private static final ThingUID THING_UID1 = new ThingUID(THING_TYPE_UID, BRIDGE_UID, "thingId1");
    private static final ThingUID THING_UID2 = new ThingUID(THING_TYPE_UID, "thingId2");
    private static final ThingUID THING_UID3 = new ThingUID(THING_TYPE_UID, BRIDGE_UID, "thingId3");
    private static final ThingUID THING_UID4 = new ThingUID(THING_TYPE_UID, "thingId4");
    private static final String KEY1 = "key1";
    private static final String KEY2 = "key2";
    private static final String VALUE1 = "value1";
    private static final String VALUE2 = "value2";
    private final Map<String, Object> properties = Map.of(KEY1, VALUE1, KEY2, VALUE2);
    private static final String DISCOVERY_THING2_INFERED_KEY = "discovery."
            + THING_UID2.getAsString().replaceAll(":", ".") + ".label";
    private static final String DISCOVERY_THING4_INFERED_KEY = "discovery."
            + THING_UID4.getAsString().replaceAll(":", ".") + ".label";
    private static final String DISCOVERY_LABEL = "Result Test";
    private static final String DISCOVERY_LABEL_KEY1 = "@text/test";
    private static final String DISCOVERY_LABEL_KEY2 = "@text/test2 [ \"50\", \"number\" ]";
    private static final String PROPERTY_LABEL1 = "Label from property (text key)";
    private static final String PROPERTY_LABEL2 = "Label from property (infered key)";
    private static final String PROPERTY_LABEL3 = "Label from property (parameters 50 and number)";

    private TranslationProvider i18nProvider = new TranslationProvider() {
        @Override
        public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
                @Nullable Locale locale, @Nullable Object @Nullable... arguments) {
            if (Locale.ENGLISH.equals(locale)) {
                if ("test".equals(key)) {
                    return PROPERTY_LABEL1;
                } else if ("test2".equals(key) && arguments != null && arguments.length == 2
                        && "50".equals(arguments[0]) && "number".equals(arguments[1])) {
                    return PROPERTY_LABEL3;
                } else if (DISCOVERY_THING2_INFERED_KEY.equals(key) || DISCOVERY_THING4_INFERED_KEY.equals(key)) {
                    return PROPERTY_LABEL2;
                }
            }
            return defaultText;
        }

        @Override
        public @Nullable String getText(@Nullable Bundle bundle, @Nullable String key, @Nullable String defaultText,
                @Nullable Locale locale) {
            return null;
        }
    };

    private LocaleProvider localeProvider = new LocaleProvider() {
        @Override
        public Locale getLocale() {
            return Locale.ENGLISH;
        }
    };

    class TestDiscoveryService extends AbstractDiscoveryService {

        public TestDiscoveryService(TranslationProvider i18nProvider, LocaleProvider localeProvider)
                throws IllegalArgumentException {
            super(Set.of(THING_TYPE_UID), 1, false);
            this.i18nProvider = i18nProvider;
            this.localeProvider = localeProvider;
        }

        @Override
        protected void startScan() {
            // Discovered thing 1 has a hard coded label and no key based on its thing UID defined in the properties
            // file => the hard coded label should be considered
            DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(THING_UID1).withThingType(THING_TYPE_UID)
                    .withProperties(properties).withRepresentationProperty(KEY1).withBridge(BRIDGE_UID)
                    .withLabel(DISCOVERY_LABEL).build();
            thingDiscovered(discoveryResult);

            // Discovered thing 2 has a hard coded label but with a key based on its thing UID defined in the properties
            // file => the value from the properties file should be considered
            discoveryResult = DiscoveryResultBuilder.create(THING_UID2).withThingType(THING_TYPE_UID)
                    .withProperties(properties).withRepresentationProperty(KEY1).withLabel(DISCOVERY_LABEL).build();
            thingDiscovered(discoveryResult);

            // Discovered thing 3 has a label referencing an entry in the properties file and no key based on its thing
            // UID defined in the properties file => the value from the properties file should be considered
            discoveryResult = DiscoveryResultBuilder.create(THING_UID3).withThingType(THING_TYPE_UID)
                    .withProperties(properties).withRepresentationProperty(KEY1).withBridge(BRIDGE_UID)
                    .withLabel(DISCOVERY_LABEL_KEY1).build();
            thingDiscovered(discoveryResult);

            // Discovered thing 4 has a label referencing an entry in the properties file and a key based on its thing
            // UID defined in the properties file => the value from the properties file (the one referenced by the
            // label) should be considered
            discoveryResult = DiscoveryResultBuilder.create(THING_UID4).withThingType(THING_TYPE_UID)
                    .withProperties(properties).withRepresentationProperty(KEY1).withLabel(DISCOVERY_LABEL_KEY2)
                    .build();
            thingDiscovered(discoveryResult);
        }
    };

    @Override
    public void thingDiscovered(DiscoveryService source, DiscoveryResult result) {
        assertThat(result.getThingTypeUID(), is(THING_TYPE_UID));
        assertThat(result.getBindingId(), is(BINDING_ID));
        assertThat(result.getProperties().size(), is(2));
        assertThat(result.getProperties(), hasEntry(KEY1, VALUE1));
        assertThat(result.getProperties(), hasEntry(KEY2, VALUE2));
        assertThat(result.getRepresentationProperty(), is(KEY1));
        assertThat(result.getTimeToLive(), is(DiscoveryResult.TTL_UNLIMITED));

        if (THING_UID1.equals(result.getThingUID())) {
            assertThat(result.getBridgeUID(), is(BRIDGE_UID));
            assertThat(result.getLabel(), is(DISCOVERY_LABEL));
        } else if (THING_UID2.equals(result.getThingUID())) {
            assertNull(result.getBridgeUID());
            assertThat(result.getLabel(), is(PROPERTY_LABEL2));
        } else if (THING_UID3.equals(result.getThingUID())) {
            assertThat(result.getBridgeUID(), is(BRIDGE_UID));
            assertThat(result.getLabel(), is(PROPERTY_LABEL1));
        } else if (THING_UID4.equals(result.getThingUID())) {
            assertNull(result.getBridgeUID());
            assertThat(result.getLabel(), is(PROPERTY_LABEL3));
        }
    }

    @Override
    public void thingRemoved(DiscoveryService source, ThingUID thingUID) {
    }

    @Override
    public @Nullable Collection<ThingUID> removeOlderResults(DiscoveryService source, long timestamp,
            @Nullable Collection<ThingTypeUID> thingTypeUIDs, @Nullable ThingUID bridgeUID) {
        return null;
    }

    @Test
    public void testDiscoveryResults() {
        TestDiscoveryService discoveryService = new TestDiscoveryService(i18nProvider, localeProvider);
        discoveryService.addDiscoveryListener(this);
        discoveryService.startScan();
    }
}
