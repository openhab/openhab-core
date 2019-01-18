/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.internal.service.StateDescriptionServiceImpl;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragment;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentProvider;
import org.eclipse.smarthome.core.types.StateDescriptionProvider;
import org.eclipse.smarthome.core.types.StateOption;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the StateDescriptionService implementation
 *
 * @author Lyubomir Papazov
 *
 */
@SuppressWarnings({ "deprecation", "null" })
public class StateDescriptionServiceImplTest {

    private static final String ITEM_NAME = "Item1";
    private static final int STATE_DESCRIPTION_PROVIDER_DEFAULT_SERVICE_RANKING = 0;
    private static final BigDecimal STATE_DESCRIPTION_PROVIDER_DEFAULT_MIN_VALUE = new BigDecimal("0");
    private static final BigDecimal STATE_DESCRIPTION_PROVIDER_DEFAULT_MAX_VALUE = new BigDecimal("0");
    private static final BigDecimal STATE_DESCRIPTION_PROVIDER_DEFAULT_STEP = new BigDecimal("0");
    private static final String STATE_DESCRIPTION_PROVIDER_DEFAULT_PATTERN = "pattern1";
    private static final boolean STATE_DESCRIPTION_PROVIDER_DEFAULT_IS_READONLY = false;
    private static final List<StateOption> STATE_DESCRIPTION_PROVIDER_DEFAULT_OPTIONS = Collections.emptyList();

    private StateDescriptionServiceImpl stateDescriptionService;
    private NumberItem item;

    @Before
    public void setup() {
        stateDescriptionService = new StateDescriptionServiceImpl();

        item = new NumberItem(ITEM_NAME);
        item.setStateDescriptionService(stateDescriptionService);
    }

    @Test
    public void legacy_testServiceWithOneStateDescriptionProvider() {
        StateDescriptionProvider stateDescriptionProviderDefault = mock(StateDescriptionProvider.class);
        when(stateDescriptionProviderDefault.getRank()).thenReturn(STATE_DESCRIPTION_PROVIDER_DEFAULT_SERVICE_RANKING);
        StateDescription stateDescription = new StateDescription(STATE_DESCRIPTION_PROVIDER_DEFAULT_MIN_VALUE,
                STATE_DESCRIPTION_PROVIDER_DEFAULT_MAX_VALUE, STATE_DESCRIPTION_PROVIDER_DEFAULT_STEP,
                STATE_DESCRIPTION_PROVIDER_DEFAULT_PATTERN, STATE_DESCRIPTION_PROVIDER_DEFAULT_IS_READONLY,
                STATE_DESCRIPTION_PROVIDER_DEFAULT_OPTIONS);
        when(stateDescriptionProviderDefault.getStateDescription(ITEM_NAME, null)).thenReturn(stateDescription);
        stateDescriptionService.addStateDescriptionProvider(stateDescriptionProviderDefault);

        StateDescription finalStateDescription = item.getStateDescription();

        assertThat(finalStateDescription.getMinimum(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_MIN_VALUE));
        assertThat(finalStateDescription.getMaximum(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_MAX_VALUE));
        assertThat(finalStateDescription.getStep(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_STEP));
        assertThat(finalStateDescription.getPattern(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_PATTERN));
        assertThat(finalStateDescription.isReadOnly(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_IS_READONLY));
        assertThat(finalStateDescription.getOptions(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_OPTIONS));
    }

    @Test
    public void legacy_testMinValueMaxValueStepAndPatternTwoDescriptionProviders() {
        StateDescription stateDescription1 = new StateDescription(new BigDecimal("-1"), new BigDecimal("-1"),
                new BigDecimal("-1"), "pattern1", false, null);
        StateDescription stateDescription2 = new StateDescription(new BigDecimal("-2"), new BigDecimal("-2"),
                new BigDecimal("-2"), "pattern2", false, null);

        registerStateDescriptionProvider(stateDescription1, -1);
        registerStateDescriptionProvider(stateDescription2, -2);
        StateDescription finalStateDescription = item.getStateDescription();

        assertThat(finalStateDescription.getMinimum(), is(stateDescription1.getMinimum()));
        assertThat(finalStateDescription.getMaximum(), is(stateDescription1.getMaximum()));
        assertThat(finalStateDescription.getStep(), is(stateDescription1.getStep()));
        assertThat(finalStateDescription.getPattern(), is(stateDescription1.getPattern()));
    }

    @Test
    public void legacy_testIsReadOnlyWhenTwoDescriptionProvidersHigherRankingIsNotReadOnly() {
        StateDescription stateDescription1 = new StateDescription(null, null, null, null, false, null);
        StateDescription stateDescription2 = new StateDescription(null, null, null, null, true, null);

        registerStateDescriptionProvider(stateDescription1, -1);
        registerStateDescriptionProvider(stateDescription2, -2);
        StateDescription finalStateDescription = item.getStateDescription();

        assertThat(finalStateDescription.isReadOnly(), is(stateDescription1.isReadOnly()));
    }

    @Test
    public void legacy_testIsReadOnlyWhenTwoDescriptionProvidersHigherRankingIsReadOnly() {
        StateDescription stateDescription1 = new StateDescription(null, null, null, null, true, null);
        StateDescription stateDescription2 = new StateDescription(null, null, null, null, false, null);

        registerStateDescriptionProvider(stateDescription1, -1);
        registerStateDescriptionProvider(stateDescription2, -2);
        StateDescription finalStateDescription = item.getStateDescription();

        assertThat(finalStateDescription.isReadOnly(), is(stateDescription1.isReadOnly()));
    }

    @Test
    public void legacy_testOptionsWhenTwoDescriptionProvidersHigherRankingProvidesOptions() {
        StateDescription stateDescription1 = new StateDescription(null, null, null, null, false,
                Arrays.asList(new StateOption("value", "label")));
        StateDescription stateDescription2 = new StateDescription(null, null, null, null, false,
                Collections.emptyList());

        registerStateDescriptionProvider(stateDescription1, -1);
        registerStateDescriptionProvider(stateDescription2, -2);
        StateDescription finalStateDescription = item.getStateDescription();

        assertThat(finalStateDescription.getOptions(), is(stateDescription1.getOptions()));
    }

    @Test
    public void legacy_testOptionsWhenTwoDescriptionProvidersHigherRankingDoesntProvideOptions() {
        StateDescription stateDescription1 = new StateDescription(null, null, null, null, false,
                Collections.emptyList());
        StateDescription stateDescription2 = new StateDescription(null, null, null, null, false,
                Arrays.asList(new StateOption("value", "label")));

        registerStateDescriptionProvider(stateDescription1, -1);
        registerStateDescriptionProvider(stateDescription2, -2);
        StateDescription finalStateDescription = item.getStateDescription();

        assertThat(finalStateDescription.getOptions(), is(stateDescription2.getOptions()));
    }

    @Test
    public void testLegacyProviderMergedBeforeFragmentProvider() {
        StateDescription stateDescription = new StateDescription(null, null, null, "pattern", false, null);
        registerStateDescriptionProvider(stateDescription, 1);

        List<@NonNull StateOption> options = Arrays.asList(new StateOption("value", "label"));
        StateDescriptionFragment stateDescriptionFragment = StateDescriptionFragmentBuilder.create()
                .withMinimum(BigDecimal.ZERO).withMaximum(BigDecimal.TEN).withOptions(options).build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment, 1);

        StateDescription finalStateDescription = item.getStateDescription();

        assertThat(finalStateDescription.getMinimum(), is(BigDecimal.ZERO));
        assertThat(finalStateDescription.getMaximum(), is(BigDecimal.TEN));
        assertThat(finalStateDescription.getPattern(), is("pattern"));
        assertThat(finalStateDescription.isReadOnly(), is(false));
        assertThat(finalStateDescription.getOptions(), is(options));
    }

    @Test
    public void testFragmentProviderOrder() {
        List<@NonNull StateOption> options = Arrays.asList(new StateOption("value", "label"));

        StateDescriptionFragment firstFragment = StateDescriptionFragmentBuilder.create()
                .withMinimum(BigDecimal.ZERO) //
                .withMaximum(BigDecimal.TEN) //
                .withPattern("pattern") //
                .withReadOnly(Boolean.TRUE) //
                .withOptions(options).build();
        registerStateDescriptionFragmentProvider(firstFragment, -1);

        StateDescriptionFragment secondFragment = StateDescriptionFragmentBuilder.create()
                .withMinimum(BigDecimal.ONE) //
                .withMaximum(BigDecimal.ONE) //
                .withStep(BigDecimal.ONE) //
                .withPattern("base_pattern") //
                .withOptions(options).build();
        registerStateDescriptionFragmentProvider(secondFragment, -2);

        StateDescription finalStateDescription = item.getStateDescription();

        assertThat(finalStateDescription.getMinimum(), is(BigDecimal.ZERO));
        assertThat(finalStateDescription.getMaximum(), is(BigDecimal.TEN));
        assertThat(finalStateDescription.getStep(), is(BigDecimal.ONE));
        assertThat(finalStateDescription.getPattern(), is("pattern"));
        assertThat(finalStateDescription.isReadOnly(), is(true));
        assertThat(finalStateDescription.getOptions(), is(options));
    }

    private void registerStateDescriptionProvider(StateDescription stateDescription, int serviceRanking) {
        StateDescriptionProvider stateDescriptionProvider = mock(StateDescriptionProvider.class);

        when(stateDescriptionProvider.getRank()).thenReturn(serviceRanking);
        when(stateDescriptionProvider.getStateDescription(ITEM_NAME, null)).thenReturn(stateDescription);
        stateDescriptionService.addStateDescriptionProvider(stateDescriptionProvider);
    }

    private void registerStateDescriptionFragmentProvider(StateDescriptionFragment stateDescription,
            int serviceRanking) {
        StateDescriptionFragmentProvider stateDescriptionProvider = mock(StateDescriptionFragmentProvider.class);

        when(stateDescriptionProvider.getRank()).thenReturn(serviceRanking);
        when(stateDescriptionProvider.getStateDescriptionFragment(ITEM_NAME, null)).thenReturn(stateDescription);
        stateDescriptionService.addStateDescriptionFragmentProvider(stateDescriptionProvider);
    }

}
