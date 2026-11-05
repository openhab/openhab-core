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
package org.openhab.core.karaf.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.kar.KarService;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.events.EventPublisher;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Tests for the add-on installation resilience of {@link FeatureInstaller}: a single add-on that cannot be
 * downloaded/resolved must not block the other add-ons, and a persistently failing add-on must not be retried
 * forever.
 *
 * @author openHAB contributors - Initial contribution
 */
@NonNullByDefault
public class FeatureInstallerTest {

    private static final String ADDON_A = "openhab-binding-a";
    private static final String ADDON_B = "openhab-binding-b";
    private static final String ADDON_BAD = "openhab-binding-bad";

    private @NonNullByDefault({}) FeaturesService featuresService;
    private @NonNullByDefault({}) EventPublisher eventPublisher;
    private @NonNullByDefault({}) FeatureInstaller featureInstaller;

    /** The names of the features the mocked {@link FeaturesService} considers currently installed. */
    private final Set<String> installedFeatures = new HashSet<>();

    @BeforeEach
    public void setUp() {
        ConfigurationAdmin configurationAdmin = mock(ConfigurationAdmin.class);
        featuresService = mock(FeaturesService.class);
        KarService karService = mock(KarService.class);
        eventPublisher = mock(EventPublisher.class);

        featureInstaller = new FeatureInstaller(configurationAdmin, featuresService, karService, eventPublisher,
                Map.of());
        // Stop the background scheduler and discard the interactions triggered by the constructor so that the
        // tests can drive installFeatures(...) deterministically.
        featureInstaller.deactivate();
        reset(featuresService, eventPublisher);
    }

    /**
     * A failing group installation (as thrown by the transactional Karaf {@code installFeatures} when one add-on
     * cannot be downloaded) must fall back to installing the add-ons individually, so that the healthy add-ons are
     * still installed.
     */
    @Test
    public void groupFailureFallsBackToIndividualInstalls() throws Exception {
        when(featuresService.listInstalledFeatures()).thenAnswer(invocation -> featuresFor(installedFeatures));
        installEverythingButBrokenAddon();

        featureInstaller.installFeatures(Set.of(ADDON_A, ADDON_B, ADDON_BAD));

        // The two healthy add-ons are installed despite the broken one, which is not.
        assertThat(installedFeatures, hasItems(ADDON_A, ADDON_B));
        assertThat(installedFeatures, not(hasItem(ADDON_BAD)));

        // The healthy add-ons were retried individually after the group install failed.
        verify(featuresService).installFeatures(eq(Set.of(ADDON_A)), any());
        verify(featuresService).installFeatures(eq(Set.of(ADDON_B)), any());
    }

    /**
     * A persistently failing add-on must be retried only a bounded number of times. Up to the limit the
     * configuration cache is cleared so that the periodic sync job retries; once the limit is reached the cache is
     * left intact so that the retry storm stops.
     */
    @Test
    public void persistentFailureStopsRetryingAfterMaxAttempts() throws Exception {
        when(featuresService.listInstalledFeatures()).thenAnswer(invocation -> featuresFor(installedFeatures));
        installEverythingButBrokenAddon();

        Map<String, Object> sentinelConfig = new HashMap<>();
        sentinelConfig.put("key", "value");

        // Attempts before the limit clear the config cache so that the sync job retries the installation.
        for (int attempt = 1; attempt < 5; attempt++) {
            setConfigMapCache(sentinelConfig);
            featureInstaller.installFeatures(Set.of(ADDON_BAD));
            assertThat("attempt " + attempt + " should schedule a retry", getConfigMapCache(), is(nullValue()));
        }

        // The final attempt gives up and keeps the cache so that the periodic sync job stops retrying.
        setConfigMapCache(sentinelConfig);
        featureInstaller.installFeatures(Set.of(ADDON_BAD));
        assertThat("the retry storm should stop after the maximum number of attempts", getConfigMapCache(),
                is(sameInstance(sentinelConfig)));
    }

    /**
     * Makes the mocked {@link FeaturesService} fail any installation request that contains the broken add-on and
     * succeed for all other requests, tracking the successfully installed features.
     */
    @SuppressWarnings("unchecked")
    private void installEverythingButBrokenAddon() throws Exception {
        doAnswer(invocation -> {
            Set<String> requested = invocation.getArgument(0, Set.class);
            if (requested.contains(ADDON_BAD)) {
                throw new Exception("simulated download failure of " + ADDON_BAD);
            }
            installedFeatures.addAll(requested);
            return null;
        }).when(featuresService).installFeatures(any(Set.class), any(EnumSet.class));
    }

    private Feature[] featuresFor(Set<String> names) {
        return names.stream().map(name -> {
            Feature feature = mock(Feature.class);
            when(feature.getName()).thenReturn(name);
            return feature;
        }).toArray(Feature[]::new);
    }

    private void setConfigMapCache(@Nullable Map<String, Object> value) throws Exception {
        Field field = FeatureInstaller.class.getDeclaredField("configMapCache");
        field.setAccessible(true);
        field.set(featureInstaller, value);
    }

    private @Nullable Object getConfigMapCache() throws Exception {
        Field field = FeatureInstaller.class.getDeclaredField("configMapCache");
        field.setAccessible(true);
        return field.get(featureInstaller);
    }
}
