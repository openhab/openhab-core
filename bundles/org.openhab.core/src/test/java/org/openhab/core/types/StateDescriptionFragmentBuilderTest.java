/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.types;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link StateDescriptionFragmentBuilder}.
 *
 * @author Henning Treu - Initial contribution
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
    public void builderWithEmptyOptions() {
        List<StateOption> options = Collections.emptyList();
        assertThat(builder.withOptions(options).build().getOptions(), is(options));
    }

    @Test
    public void builderWithOption() {
        StateOption option = new StateOption("value", "label");
        assertThat(builder.withOption(option).build().getOptions(), is(Arrays.asList(option)));
    }

    @Test
    public void builderWithOptions() {
        List<StateOption> options = Arrays.asList(new StateOption("value1", "label1"),
                new StateOption("value2", "label2"));
        assertThat(builder.withOptions(options).build().getOptions(), is(options));
    }

    @Test
    public void builderWithStateDescription() {
        StateDescription source = new StateDescription(BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ONE, "pattern", true,
                Collections.singletonList(new StateOption("value", "label")));
        StateDescriptionFragmentBuilder builder = StateDescriptionFragmentBuilder.create(source);
        StateDescriptionFragment fragment = builder.build();

        assertThat(fragment.getMinimum(), is(source.getMinimum()));
        assertThat(fragment.getMaximum(), is(source.getMaximum()));
        assertThat(fragment.getStep(), is(source.getStep()));
        assertThat(fragment.getPattern(), is(source.getPattern()));
        assertThat(fragment.isReadOnly(), is(source.isReadOnly()));
        assertThat(fragment.getOptions(), is(source.getOptions()));

        builder.withOption(new StateOption("NEW value", "NEW label"));
    }

    @Test
    public void subsequentBuildsCreateIndependentFragments() {
        StateDescriptionFragment fragment1 = builder.withMinimum(BigDecimal.ZERO).withMaximum(BigDecimal.TEN)
                .withStep(BigDecimal.ONE).withPattern("pattern").withReadOnly(Boolean.FALSE)
                .withOptions(Collections.singletonList(new StateOption("value", "label"))).build();
        StateDescriptionFragment fragment2 = builder.withMinimum(BigDecimal.ONE).withMaximum(BigDecimal.ONE)
                .withStep(BigDecimal.ZERO).withPattern("pattern_new").withReadOnly(Boolean.TRUE)
                .withOptions(Collections.emptyList()).build();

        assertThat(fragment1.getMinimum(), is(not(fragment2.getMinimum())));
        assertThat(fragment1.getMaximum(), is(not(fragment2.getMaximum())));
        assertThat(fragment1.getStep(), is(not(fragment2.getStep())));
        assertThat(fragment1.getPattern(), is(not(fragment2.getPattern())));
        assertThat(fragment1.isReadOnly(), is(not(fragment2.isReadOnly())));
        assertThat(fragment1.getOptions(), is(not(fragment2.getOptions())));
    }

}
