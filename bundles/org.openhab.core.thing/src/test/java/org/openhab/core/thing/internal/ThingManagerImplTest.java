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
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.service.ReadyService;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.i18n.ThingStatusInfoI18nLocalizationService;
import org.openhab.core.thing.internal.ThingTracker.ThingTrackerEvent;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.ChannelGroupTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.BundleContext;

/**
 * @author Simon Kaufmann - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ThingManagerImplTest extends JavaTest {

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
    private @Mock @NonNullByDefault({}) BundleResolver bundleResolverMock;
    private @Mock @NonNullByDefault({}) TranslationProvider translationProviderMock;
    private @Mock @NonNullByDefault({}) BundleContext bundleContextMock;
    private @Mock @NonNullByDefault({}) ThingType thingTypeMock;

    // This class is final so it cannot be mocked
    private final ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService = new ThingStatusInfoI18nLocalizationService();

    @BeforeEach
    public void setup() {
        when(thingMock.getUID()).thenReturn(new ThingUID("test", "thing"));
        when(thingMock.getStatusInfo())
                .thenReturn(new ThingStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE, null));
        when(thingTypeMock.getConfigDescriptionURI()).thenReturn(null);
        when(thingTypeRegistryMock.getThingType(any())).thenReturn(thingTypeMock);
    }

    private ThingManagerImpl createThingManager() {
        return new ThingManagerImpl(channelGroupTypeRegistryMock, channelTypeRegistryMock, communicationManagerMock,
                configDescriptionRegistryMock, configDescriptionValidatorMock, eventPublisherMock,
                itemChannelLinkRegistryMock, readyServiceMock, safeCallerMock, storageServiceMock, thingRegistryMock,
                thingStatusInfoI18nLocalizationService, thingTypeRegistryMock, bundleResolverMock,
                translationProviderMock, bundleContextMock);
    }

    @Test
    public void thingHandlerFactoryLifecycle() {
        ThingHandlerFactory mockFactory1 = mock(ThingHandlerFactory.class);
        ThingHandlerFactory mockFactory2 = mock(ThingHandlerFactory.class);
        when(storageServiceMock.getStorage(any(), any())).thenReturn(storageMock);
        when(storageMock.get(any())).thenReturn(null);

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

    @Test
    public void removingNonExistingThingLogsError() {
        setupInterceptedLogger(ThingManagerImpl.class, LogLevel.DEBUG);

        ThingManagerImpl thingManager = createThingManager();

        thingManager.thingRemoved(thingMock, ThingTrackerEvent.THING_REMOVED);

        stopInterceptedLogger(ThingManagerImpl.class);
        assertLogMessage(ThingManagerImpl.class, LogLevel.ERROR,
                "Trying to remove thing 'test::thing', but is not tracked by ThingManager. This is a bug.");
    }

    @Test
    public void addingExistingThingLogsError() {
        when(storageServiceMock.getStorage(any(), any())).thenReturn(storageMock);
        when(storageMock.get(any())).thenReturn(null);

        setupInterceptedLogger(ThingManagerImpl.class, LogLevel.DEBUG);

        ThingManagerImpl thingManager = createThingManager();

        thingManager.thingAdded(thingMock, ThingTrackerEvent.THING_ADDED);
        thingManager.thingAdded(thingMock, ThingTrackerEvent.THING_ADDED);

        stopInterceptedLogger(ThingManagerImpl.class);
        assertLogMessage(ThingManagerImpl.class, LogLevel.ERROR,
                "A thing with UID 'test::thing' is already tracked by ThingManager. This is a bug.");
    }

    @Test
    public void replacingThingWithWrongUIDLogsError() {
        when(storageServiceMock.getStorage(any(), any())).thenReturn(storageMock);
        when(storageMock.get(any())).thenReturn(null);

        Thing thingMock2 = mock(Thing.class);

        when(thingMock2.getUID()).thenReturn(new ThingUID("test", "thing2"));
        when(thingMock2.getStatusInfo())
                .thenReturn(new ThingStatusInfo(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE, null));

        setupInterceptedLogger(ThingManagerImpl.class, LogLevel.DEBUG);

        ThingManagerImpl thingManager = createThingManager();

        thingManager.thingAdded(thingMock, ThingTrackerEvent.THING_ADDED);
        thingManager.thingUpdated(thingMock2, thingMock, ThingTrackerEvent.THING_ADDED);

        stopInterceptedLogger(ThingManagerImpl.class);
        assertLogMessage(ThingManagerImpl.class, LogLevel.ERROR,
                "Thing 'test::thing2' is different from thing tracked by ThingManager. This is a bug.");
    }
}
