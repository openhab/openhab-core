/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.thing.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.common.SafeCaller;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.validation.ConfigDescriptionValidator;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.i18n.ThingStatusInfoI18nLocalizationService;
import org.openhab.core.thing.internal.ThingTracker.ThingTrackerEvent;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.ChannelGroupTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;

/**
 * @author Simon Kaufmann - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@NonNullByDefault
public class ThingManagerImplTest {

    private @Mock @NonNullByDefault({}) Bundle bundleMock;
    private @Mock @NonNullByDefault({}) BundleResolver bundleResolverMock;
    private @Mock @NonNullByDefault({}) ChannelGroupTypeRegistry channelGroupTypeRegistryMock;
    private @Mock @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistryMock;
    private @Mock @NonNullByDefault({}) CommunicationManager communicationManagerMock;
    private @Mock @NonNullByDefault({}) EventPublisher eventPublisherMock;
    private @Mock @NonNullByDefault({}) ConfigDescriptionRegistry configDescriptionRegistryMock;
    private @Mock @NonNullByDefault({}) ConfigDescriptionValidator configDescriptionValidatorMock;
    private @Mock @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistryMock;
    private @Mock @NonNullByDefault({}) ThingTypeRegistry thingTypeRegistryMock;
    private @Mock @NonNullByDefault({}) ReadyService readyServiceMock;
    private @Mock @NonNullByDefault({}) SafeCaller safeCallerMock;
    private @Mock @NonNullByDefault({}) Storage<Object> storageMock;
    private @Mock @NonNullByDefault({}) StorageService storageServiceMock;
    private @Mock @NonNullByDefault({}) Thing thingMock;
    private @Mock @NonNullByDefault({}) ThingRegistryImpl thingRegistryMock;

    // This class is final so it cannot be mocked
    private final ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService = new ThingStatusInfoI18nLocalizationService();

    @BeforeEach
    public void setup() {
        when(bundleMock.getSymbolicName()).thenReturn("test");
        when(bundleResolverMock.resolveBundle(any())).thenReturn(bundleMock);
        when(thingMock.getUID()).thenReturn(new ThingUID("test", "thing"));
    }

    private ThingManagerImpl createThingManager() {
        return new ThingManagerImpl(bundleResolverMock, channelGroupTypeRegistryMock, channelTypeRegistryMock,
                communicationManagerMock, configDescriptionRegistryMock, configDescriptionValidatorMock,
                eventPublisherMock, itemChannelLinkRegistryMock, readyServiceMock, safeCallerMock, storageServiceMock,
                thingRegistryMock, thingStatusInfoI18nLocalizationService, thingTypeRegistryMock);
    }

    @Test
    public void thingHandlerFactoryLifecycle() {
        ThingHandlerFactory mockFactory1 = mock(ThingHandlerFactory.class);
        ThingHandlerFactory mockFactory2 = mock(ThingHandlerFactory.class);

        ThingManagerImpl thingManager = createThingManager();

        thingManager.thingAdded(thingMock, ThingTrackerEvent.THING_ADDED);

        thingManager.addThingHandlerFactory(mockFactory1);
        verify(mockFactory1, atLeastOnce()).supportsThingType(any());
        thingManager.removeThingHandlerFactory(mockFactory1);

        thingManager.addThingHandlerFactory(mockFactory2);
        verify(mockFactory2, atLeastOnce()).supportsThingType(any());
        thingManager.removeThingHandlerFactory(mockFactory2);
    }

    @Test
    public void setEnabledWithUnknownThingUID() throws Exception {
        ThingUID unknownUID = new ThingUID("someBundle", "someType", "someID");

        when(storageServiceMock.getStorage(eq("thing_status_storage"), any(ClassLoader.class))).thenReturn(storageMock);

        ThingManagerImpl thingManager = createThingManager();

        thingManager.setEnabled(unknownUID, true);
        verify(storageMock).remove(eq(unknownUID.getAsString()));

        thingManager.setEnabled(unknownUID, false);
        verify(storageMock).put(eq(unknownUID.getAsString()), eq(""));
    }

    @Test
    public void isEnabledWithUnknownThingUIDAndNullStorage() throws Exception {
        ThingUID unknownUID = new ThingUID("someBundle", "someType", "someID");

        when(storageServiceMock.getStorage(eq("thing_status_storage"), any(ClassLoader.class))).thenReturn(storageMock);

        ThingManagerImpl thingManager = createThingManager();

        assertTrue(thingManager.isEnabled(unknownUID));
    }

    @Test
    public void isEnabledWithUnknownThingUIDAndNonNullStorage() throws Exception {
        ThingUID unknownUID = new ThingUID("someBundle", "someType", "someID");

        when(storageMock.containsKey(unknownUID.getAsString())).thenReturn(false);
        when(storageServiceMock.getStorage(eq("thing_status_storage"), any(ClassLoader.class))).thenReturn(storageMock);

        ThingManagerImpl thingManager = createThingManager();

        assertTrue(thingManager.isEnabled(unknownUID));

        when(storageMock.containsKey(unknownUID.getAsString())).thenReturn(true);
        when(storageServiceMock.getStorage(eq("thing_status_storage"), any(ClassLoader.class))).thenReturn(storageMock);

        assertFalse(thingManager.isEnabled(unknownUID));
    }
}
