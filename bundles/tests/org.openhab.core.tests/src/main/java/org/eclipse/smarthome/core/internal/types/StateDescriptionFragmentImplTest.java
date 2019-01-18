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
package org.eclipse.smarthome.core.internal.types;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragment;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link StateDescriptionFragmentImpl}.
 *
 * @author Henning Treu - initial contribution
 *
 */
public class StateDescriptionFragmentImplTest {

    private StateDescriptionFragmentImpl source;

    @Before
    public void setup() {
        source = new StateDescriptionFragmentImpl();
        source.setMinimum(BigDecimal.ZERO);
        source.setMaximum(BigDecimal.TEN);
        source.setStep(BigDecimal.ONE);
        source.setPattern("pattern");
        source.setReadOnly(Boolean.TRUE);
        source.setOptions(new ArrayList<>(0));
    }

    @Test
    public void mergeFragment() {
        StateDescriptionFragment fragment = new StateDescriptionFragmentImpl().merge(source);

        assertThat(fragment.getMinimum(), is(source.getMinimum()));
        assertThat(fragment.getMaximum(), is(source.getMaximum()));
        assertThat(fragment.getStep(), is(source.getStep()));
        assertThat(fragment.getPattern(), is(source.getPattern()));
        assertThat(fragment.isReadOnly(), is(source.isReadOnly()));
        assertThat(fragment.getOptions(), is(source.getOptions()));
    }

    @Test
    @SuppressWarnings("null")
    public void toStateDescription() {
        StateDescription stateDescription = source.toStateDescription();

        assertThat(stateDescription.getMinimum(), is(source.getMinimum()));
        assertThat(stateDescription.getMaximum(), is(source.getMaximum()));
        assertThat(stateDescription.getStep(), is(source.getStep()));
        assertThat(stateDescription.getPattern(), is(source.getPattern()));
        assertThat(stateDescription.isReadOnly(), is(source.isReadOnly()));
        assertThat(stateDescription.getOptions(), is(source.getOptions()));
    }
}
