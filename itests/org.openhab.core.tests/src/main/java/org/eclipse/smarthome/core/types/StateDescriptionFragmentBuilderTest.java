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
package org.eclipse.smarthome.core.types;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link StateDescriptionFragmentBuilder}.
 *
 * @author Henning Treu - initial contribution
 *
 */
public class StateDescriptionFragmentBuilderTest {

    private StateDescriptionFragmentBuilder builder;

    @Before
    public void setup() {
        builder = StateDescriptionFragmentBuilder.create();
    }

    @Test
    public void builderWithMinimum() {
        assertThat(builder.withMinimum(BigDecimal.TEN).build().getMinimum(), is(BigDecimal.TEN));
    }

    @Test
    public void builderWithMaximum() {
        assertThat(builder.withMaximum(BigDecimal.TEN).build().getMaximum(), is(BigDecimal.TEN));
    }

    @Test
    public void builderWithStep() {
        assertThat(builder.withStep(BigDecimal.TEN).build().getStep(), is(BigDecimal.TEN));
    }

    @Test
    public void builderWithPattern() {
        assertThat(builder.withPattern("pattern").build().getPattern(), is("pattern"));
    }

    @Test
    public void builderWithReadOnly() {
        assertThat(builder.withReadOnly(Boolean.TRUE).build().isReadOnly(), is(Boolean.TRUE));
    }

    @Test
    public void builderWithIOptions() {
        List<@NonNull StateOption> options = new ArrayList<>(0);
        assertThat(builder.withOptions(options).build().getOptions(), is(options));
    }

    @Test
    public void builderWithStateDescription() {
        StateDescription source = new StateDescription(BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE, "pattern", true,
                Arrays.asList(new StateOption[] { new StateOption("value", "label") }));
        StateDescriptionFragment fragment = StateDescriptionFragmentBuilder.create(source).build();

        assertThat(fragment.getMinimum(), is(source.getMinimum()));
        assertThat(fragment.getMaximum(), is(source.getMaximum()));
        assertThat(fragment.getStep(), is(source.getStep()));
        assertThat(fragment.getPattern(), is(source.getPattern()));
        assertThat(fragment.isReadOnly(), is(source.isReadOnly()));
        assertThat(fragment.getOptions(), is(source.getOptions()));
    }

    @Test
    public void subsequentBuildsCreateIndependentFragments() {
        StateDescriptionFragment fragment1 = builder.withMinimum(BigDecimal.ZERO).withMaximum(BigDecimal.TEN)
                .withPattern("pattern").build();
        StateDescriptionFragment fragment2 = builder.withMinimum(BigDecimal.ONE).withMaximum(BigDecimal.ONE)
                .withPattern("pattern_new").build();

        assertThat(fragment1.getMinimum(), is(not(fragment2.getMinimum())));
        assertThat(fragment1.getMaximum(), is(not(fragment2.getMaximum())));
        assertThat(fragment1.getPattern(), is(not(fragment2.getPattern())));
    }

}
