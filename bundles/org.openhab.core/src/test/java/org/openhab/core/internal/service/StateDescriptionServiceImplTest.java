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
package org.openhab.core.internal.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateDescriptionFragmentProvider;
import org.openhab.core.types.StateOption;

/**
 * Tests for the {@link StateDescriptionServiceImpl}.
 *
 * @author Lyubomir Papazov - Initial contribution
 */
@NonNullByDefault
public class StateDescriptionServiceImplTest {

    private static final String ITEM_NAME = "Item1";
    private static final int STATE_DESCRIPTION_PROVIDER_DEFAULT_SERVICE_RANKING = 0;
    private static final BigDecimal STATE_DESCRIPTION_PROVIDER_DEFAULT_MIN_VALUE = BigDecimal.ZERO;
    private static final BigDecimal STATE_DESCRIPTION_PROVIDER_DEFAULT_MAX_VALUE = BigDecimal.ZERO;
    private static final BigDecimal STATE_DESCRIPTION_PROVIDER_DEFAULT_STEP = BigDecimal.ZERO;
    private static final String STATE_DESCRIPTION_PROVIDER_DEFAULT_PATTERN = "pattern1";
    private static final Boolean STATE_DESCRIPTION_PROVIDER_DEFAULT_IS_READONLY = Boolean.FALSE;
    private static final List<StateOption> STATE_DESCRIPTION_PROVIDER_DEFAULT_OPTIONS = Collections.emptyList();

    private @NonNullByDefault({}) StateDescriptionServiceImpl stateDescriptionService;
    private @NonNullByDefault({}) NumberItem item;

    @BeforeEach
    public void setup() {
        stateDescriptionService = new StateDescriptionServiceImpl();

        item = new NumberItem(ITEM_NAME);
        item.setStateDescriptionService(stateDescriptionService);
    }

    @Test
    public void testServiceWithOneStateDescriptionProvider() {
        StateDescriptionFragment stateDescriptionFragment = StateDescriptionFragmentBuilder.create()
                .withMinimum(STATE_DESCRIPTION_PROVIDER_DEFAULT_MIN_VALUE)
                .withMaximum(STATE_DESCRIPTION_PROVIDER_DEFAULT_MAX_VALUE)
                .withStep(STATE_DESCRIPTION_PROVIDER_DEFAULT_STEP)
                .withPattern(STATE_DESCRIPTION_PROVIDER_DEFAULT_PATTERN)
                .withReadOnly(STATE_DESCRIPTION_PROVIDER_DEFAULT_IS_READONLY)
                .withOptions(STATE_DESCRIPTION_PROVIDER_DEFAULT_OPTIONS).build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment,
                STATE_DESCRIPTION_PROVIDER_DEFAULT_SERVICE_RANKING);

        StateDescription stateDescription = Objects.requireNonNull(item.getStateDescription());
        assertThat(stateDescription.getMinimum(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_MIN_VALUE));
        assertThat(stateDescription.getMaximum(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_MAX_VALUE));
        assertThat(stateDescription.getStep(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_STEP));
        assertThat(stateDescription.getPattern(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_PATTERN));
        assertThat(stateDescription.isReadOnly(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_IS_READONLY));
        assertThat(stateDescription.getOptions(), is(STATE_DESCRIPTION_PROVIDER_DEFAULT_OPTIONS));
    }

    @Test
    public void testMinValueMaxValueStepAndPatternTwoDescriptionProviders() {
        StateDescriptionFragment stateDescriptionFragment1 = StateDescriptionFragmentBuilder.create()
                .withMinimum(new BigDecimal(-1)) //
                .withMaximum(new BigDecimal(-1)) //
                .withStep(new BigDecimal(-1)) //
                .withPattern("pattern1").build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment1, -1);

        StateDescriptionFragment stateDescriptionFragment2 = StateDescriptionFragmentBuilder.create()
                .withMinimum(new BigDecimal(-2)) //
                .withMaximum(new BigDecimal(-2)) //
                .withStep(new BigDecimal(-2)) //
                .withPattern("pattern2").build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment2, -2);

        StateDescription stateDescription = Objects.requireNonNull(item.getStateDescription());

        assertThat(stateDescription.getMinimum(), is(stateDescriptionFragment1.getMinimum()));
        assertThat(stateDescription.getMaximum(), is(stateDescriptionFragment1.getMaximum()));
        assertThat(stateDescription.getStep(), is(stateDescriptionFragment1.getStep()));
        assertThat(stateDescription.getPattern(), is(stateDescriptionFragment1.getPattern()));
    }

    @Test
    public void testIsReadOnlyWhenTwoDescriptionProvidersHigherRankingIsNotReadOnly() {
        StateDescriptionFragment stateDescriptionFragment1 = StateDescriptionFragmentBuilder.create().build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment1, -1);

        StateDescriptionFragment stateDescriptionFragment2 = StateDescriptionFragmentBuilder.create()
                .withReadOnly(Boolean.TRUE).build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment2, -2);

        StateDescription stateDescription = Objects.requireNonNull(item.getStateDescription());

        assertThat(stateDescription.isReadOnly(), is(stateDescriptionFragment2.isReadOnly()));
    }

    @Test
    public void testIsReadOnlyWhenTwoDescriptionProvidersHigherRankingIsReadOnly() {
        StateDescriptionFragment stateDescriptionFragment1 = StateDescriptionFragmentBuilder.create()
                .withReadOnly(Boolean.TRUE).build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment1, -1);

        StateDescriptionFragment stateDescriptionFragment2 = StateDescriptionFragmentBuilder.create().build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment2, -2);

        StateDescription stateDescription = Objects.requireNonNull(item.getStateDescription());

        assertThat(stateDescription.isReadOnly(), is(stateDescriptionFragment1.isReadOnly()));
    }

    @Test
    public void testOptionsWhenTwoDescriptionProvidersHigherRankingProvidesOptions() {
        StateDescriptionFragment stateDescriptionFragment1 = StateDescriptionFragmentBuilder.create()
                .withOption(new StateOption("value", "label")).build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment1, -1);

        StateDescriptionFragment stateDescriptionFragment2 = StateDescriptionFragmentBuilder.create().build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment2, -2);

        StateDescription stateDescription = Objects.requireNonNull(item.getStateDescription());

        assertThat(stateDescription.getOptions(), is(stateDescriptionFragment1.getOptions()));
    }

    @Test
    public void testOptionsWhenTwoDescriptionProvidersHigherRankingDoesntProvideOptions() {
        StateDescriptionFragment stateDescriptionFragment1 = StateDescriptionFragmentBuilder.create().build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment1, -1);

        StateDescriptionFragment stateDescriptionFragment2 = StateDescriptionFragmentBuilder.create()
                .withOption(new StateOption("value", "label")).build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment2, -2);

        StateDescription stateDescription = Objects.requireNonNull(item.getStateDescription());

        assertThat(stateDescription.getOptions(), is(stateDescriptionFragment2.getOptions()));
    }

    @Test
    public void testFragmentsAreMergedInProviderOrder() {
        final List<StateOption> options = List.of(new StateOption("value", "label"));

        StateDescriptionFragment stateDescriptionFragment1 = StateDescriptionFragmentBuilder.create()
                .withMinimum(BigDecimal.ZERO) //
                .withMaximum(BigDecimal.TEN) //
                .withPattern("pattern") //
                .withReadOnly(Boolean.TRUE).build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment1, -1);

        StateDescriptionFragment stateDescriptionFragment2 = StateDescriptionFragmentBuilder.create()
                .withMinimum(BigDecimal.ONE) //
                .withMaximum(BigDecimal.ONE) //
                .withStep(BigDecimal.ONE) //
                .withPattern("base_pattern") //
                .withOptions(options).build();
        registerStateDescriptionFragmentProvider(stateDescriptionFragment2, -2);

        StateDescription stateDescription = Objects.requireNonNull(item.getStateDescription());

        assertThat(stateDescription.getMinimum(), is(BigDecimal.ZERO));
        assertThat(stateDescription.getMaximum(), is(BigDecimal.TEN));
        assertThat(stateDescription.getStep(), is(BigDecimal.ONE));
        assertThat(stateDescription.getPattern(), is("pattern"));
        assertThat(stateDescription.isReadOnly(), is(true));
        assertThat(stateDescription.getOptions(), is(options));
        // check that fragement2 is not merged into fragement1
        List<StateOption> fragment1Options = Objects.requireNonNull(stateDescriptionFragment1.getOptions());
        assertThat(fragment1Options.size(), is(0));
    }

    private void registerStateDescriptionFragmentProvider(StateDescriptionFragment stateDescriptionFragment,
            int serviceRanking) {
        StateDescriptionFragmentProvider stateDescriptionProvider = mock(StateDescriptionFragmentProvider.class);
        when(stateDescriptionProvider.getRank()).thenReturn(serviceRanking);
        when(stateDescriptionProvider.getStateDescriptionFragment(ITEM_NAME, null))
                .thenReturn(stateDescriptionFragment);
        stateDescriptionService.addStateDescriptionFragmentProvider(stateDescriptionProvider);
    }
}
