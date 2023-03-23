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
package org.openhab.core.thing.i18n;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.test.SyntheticBundleInstaller;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.openhab.core.thing.testutil.i18n.DefaultLocaleSetter;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.util.BundleResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

/**
 * OSGi tests for the {@link ThingStatusInfoI18nLocalizationServiceOSGiTest}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Henning Sudbrock - Migration from Groovy to Java
 */
@NonNullByDefault
public class ThingStatusInfoI18nLocalizationServiceOSGiTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "thingStatusInfoI18nTest.bundle";

    private @NonNullByDefault({}) ConfigurationAdmin configurationAdmin;
    private @NonNullByDefault({}) Locale defaultLocale;
    private @NonNullByDefault({}) ManagedThingProvider managedThingProvider;
    private @NonNullByDefault({}) Bundle testBundle;
    private @NonNullByDefault({}) Thing thing;
    private @NonNullByDefault({}) ThingStatusInfoI18nLocalizationService thingStatusInfoI18nLocalizationService;

    @BeforeEach
    public void setup() throws Exception {
        configurationAdmin = getService(ConfigurationAdmin.class);
        assertNotNull(configurationAdmin);

        LocaleProvider localeProvider = getService(LocaleProvider.class);
        assertThat(localeProvider, is(notNullValue()));
        defaultLocale = localeProvider.getLocale();

        new DefaultLocaleSetter(configurationAdmin).setDefaultLocale(Locale.ENGLISH);
        waitForAssert(() -> assertThat(localeProvider.getLocale(), is(Locale.ENGLISH)));

        registerVolatileStorageService();

        SimpleThingHandlerFactory simpleThingHandlerFactory = new SimpleThingHandlerFactory();
        ComponentContext componentContext = mock(ComponentContext.class);
        when(componentContext.getBundleContext()).thenReturn(bundleContext);
        simpleThingHandlerFactory.activate(componentContext);
        registerService(simpleThingHandlerFactory, ThingHandlerFactory.class.getName());

        registerThingType();

        thing = ThingBuilder.create(new ThingTypeUID("aaa:bbb"), "ccc").build();

        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));

        managedThingProvider.add(thing);

        waitForAssert(() -> assertThat(thing.getStatus(), is(ThingStatus.ONLINE)));

        testBundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(testBundle, is(notNullValue()));

        thingStatusInfoI18nLocalizationService = getService(ThingStatusInfoI18nLocalizationService.class);
        assertThat(thingStatusInfoI18nLocalizationService, is(notNullValue()));

        thingStatusInfoI18nLocalizationService.setBundleResolver(new BundleResolverImpl());
    }

    @AfterEach
    public void tearDown() throws IOException, BundleException {
        testBundle.uninstall();
        managedThingProvider.remove(thing.getUID());
        new DefaultLocaleSetter(configurationAdmin).setDefaultLocale(defaultLocale);
        waitForAssert(() -> assertThat(getService(LocaleProvider.class).getLocale(), is(defaultLocale)));
    }

    @Test
    public void thingStatusInfoNotChangedIfNoDescription() {
        ThingStatusInfo orgThingStatusInfo = thing.getStatusInfo();
        assertThat(orgThingStatusInfo.getDescription(), nullValue());
        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                null);
        assertThat(thingStatusInfo, is(orgThingStatusInfo));
    }

    @Test
    public void thingStatusInfoNotChangedForNonI18nConstantDescription() {
        ThingStatusInfo expected = new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, "The description.");
        setThingStatusInfo(thing, expected);

        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                null);
        assertThat(thingStatusInfo, is(expected));
    }

    @Test
    public void thingStatusInfoLocalizedForOnlineStatusWithoutArgument() {
        setThingStatusInfo(thing, new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, "@text/online"));

        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                null);
        assertThat(thingStatusInfo,
                is(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Thing is online.")));

        thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.GERMAN);
        assertThat(thingStatusInfo,
                is(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Thing ist online.")));

        thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.ENGLISH);
        assertThat(thingStatusInfo,
                is(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Thing is online.")));
    }

    @Test
    public void thingStatusInfoLocalizedForOfflineStatusWithoutArgument() {
        setThingStatusInfo(thing,
                new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "@text/offline.without-param"));

        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                null);
        assertThat(thingStatusInfo,
                is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Thing is offline.")));

        thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.GERMAN);
        assertThat(thingStatusInfo,
                is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Thing ist offline.")));

        thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.ENGLISH);
        assertThat(thingStatusInfo,
                is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Thing is offline.")));
    }

    @Test
    public void thingStatusInfoLocalizedForOfflineStatusWithOneArgument() {
        setThingStatusInfo(thing, new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "@text/offline.with-one-param [\"5\"]"));

        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                null);
        assertThat(thingStatusInfo, is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Thing is offline because of 5 failed logins.")));

        thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.GERMAN);
        assertThat(thingStatusInfo, is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Thing ist nach 5 fehlgeschlagenen Anmeldeversuchen offline.")));

        thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.ENGLISH);
        assertThat(thingStatusInfo, is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Thing is offline because of 5 failed logins.")));
    }

    @Test
    public void thingStatusInfoLocalizedForOfflineStatusWithTwoArguments() {
        setThingStatusInfo(thing, new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "@text/offline.with-two-params [\"@text/communication-problems\", \"120\"]"));

        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                null);
        assertThat(thingStatusInfo, is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Thing is offline because of communication problems. Pls try again after 120 seconds.")));

        thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.GERMAN);
        assertThat(thingStatusInfo, is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Thing ist wegen Kommunikationsprobleme offline. Bitte versuchen Sie es in 120 Sekunden erneut.")));

        thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.ENGLISH);
        assertThat(thingStatusInfo, is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Thing is offline because of communication problems. Pls try again after 120 seconds.")));
    }

    @Test
    public void thingStatusInfoLocalizedForMalformedArguments() {
        setThingStatusInfo(thing, new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "@text/offline.with-two-params   [   \"@text/communication-problems\", ,      \"120\"  ]"));

        ThingStatusInfo thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing,
                null);
        assertThat(thingStatusInfo, is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Thing is offline because of communication problems. Pls try again after 120 seconds.")));

        thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.GERMAN);
        assertThat(thingStatusInfo, is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Thing ist wegen Kommunikationsprobleme offline. Bitte versuchen Sie es in 120 Sekunden erneut.")));

        thingStatusInfo = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.ENGLISH);
        assertThat(thingStatusInfo, is(new ThingStatusInfo(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                "Thing is offline because of communication problems. Pls try again after 120 seconds.")));
    }

    @Test
    public void unhandledThingCanBeHandled() {
        ThingStatusInfo info = thingStatusInfoI18nLocalizationService
                .getLocalizedThingStatusInfo(ThingBuilder.create(new ThingTypeUID("xyz:abc"), "123").build(), null);
        assertThat(info, is(ThingStatusInfoBuilder.create(ThingStatus.UNINITIALIZED, ThingStatusDetail.NONE).build()));
    }

    @Test
    public void nullThingIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(null, null));
    }

    @Test
    public void translationsFromThingHandlerSuperclassBundleAreUsed() {
        setThingStatusInfo(thing, new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, "@text/testText"));
        ThingStatusInfo info = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.GERMAN);
        assertThat(info, is(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, "Some test text")));
    }

    @Test
    public void noTranslationIsUsedIfNotEvenFoundInThingHandlerSuperclassBundle() {
        setThingStatusInfo(thing,
                new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE, "@text/some-fake-label-for-this-test"));
        ThingStatusInfo info = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.GERMAN);
        assertThat(info, is(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE,
                "@text/some-fake-label-for-this-test")));
    }

    @Test
    public void argumentsForDescriptionAreAlsoTranslatedUsingThingHandlerSuperclassBundle() {
        setThingStatusInfo(thing, new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE,
                "@text/testText.withParams [@text/some.other.text, 60]"));
        ThingStatusInfo info = thingStatusInfoI18nLocalizationService.getLocalizedThingStatusInfo(thing, Locale.GERMAN);
        assertThat(info, is(new ThingStatusInfo(ThingStatus.ONLINE, ThingStatusDetail.NONE,
                "Some test text with params: some other text - 60")));
    }

    @SuppressWarnings("null")
    private void setThingStatusInfo(Thing thing, ThingStatusInfo thingStatusInfo) {
        ((SimpleThingHandler) thing.getHandler()).setThingStatusInfo(thingStatusInfo);
    }

    private class SimpleThingHandlerFactory extends BaseThingHandlerFactory {

        @Override
        public void activate(ComponentContext componentContext) {
            super.activate(componentContext);
        }

        @Override
        public boolean supportsThingType(ThingTypeUID thingTypeUID) {
            return true;
        }

        @Override
        protected @Nullable ThingHandler createHandler(Thing thing) {
            return new SimpleThingHandler(thing);
        }
    }

    private class SimpleThingHandler extends AbstractThingHandler {

        SimpleThingHandler(Thing thing) {
            super(thing);
        }

        @Override
        public void handleCommand(ChannelUID channelUID, Command command) {
            // do nothing
        }

        @Override
        public void initialize() {
            updateStatus(ThingStatus.ONLINE);
        }

        public void setThingStatusInfo(ThingStatusInfo thingStatusInfo) {
            updateStatus(thingStatusInfo.getStatus(), thingStatusInfo.getStatusDetail(),
                    thingStatusInfo.getDescription());
        }
    }

    private abstract class AbstractThingHandler extends BaseThingHandler {
        public AbstractThingHandler(Thing thing) {
            super(thing);
        }
    }

    /**
     * Use this for simulating that the {@link AbstractThingHandler} class does come from another bundle than this test
     * bundle.
     */
    private class BundleResolverImpl implements BundleResolver {
        @Override
        public Bundle resolveBundle(@Nullable Class<?> clazz) {
            if (clazz != null && clazz.equals(AbstractThingHandler.class)) {
                return testBundle;
            } else {
                return FrameworkUtil.getBundle(clazz);
            }
        }
    }

    private void registerThingType() {
        ThingType thingType = ThingTypeBuilder.instance(new ThingTypeUID("aaa:bbb"), "label").build();

        ThingTypeProvider thingTypeProvider = mock(ThingTypeProvider.class);
        ThingTypeRegistry thingTypeRegistry = mock(ThingTypeRegistry.class);

        when(thingTypeProvider.getThingType(eq(thingType.getUID()), nullable(Locale.class))).thenReturn(thingType);
        when(thingTypeRegistry.getThingType(eq(thingType.getUID()))).thenReturn(thingType);

        registerService(thingTypeProvider);
        registerService(thingTypeRegistry);
    }
}
