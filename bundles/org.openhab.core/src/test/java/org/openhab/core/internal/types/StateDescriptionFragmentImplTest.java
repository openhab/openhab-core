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
package org.openhab.core.internal.types;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateOption;

/**
 * Test the {@link StateDescriptionFragmentImpl}.
 *
 * @author Henning Treu - Initial contribution
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
        source.setOptions(Collections.emptyList());
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

        // fragment with empty options should inherit new options
        StateDescriptionFragmentImpl sourceWithOptions = new StateDescriptionFragmentImpl();
        sourceWithOptions.setOptions(Collections.singletonList(new StateOption("value1", "label1")));

        fragment = source.merge(sourceWithOptions);

        assertThat(fragment.getOptions(), is(sourceWithOptions.getOptions()));

        // fragment with options should NOT inherit new options
        sourceWithOptions.setOptions(Collections.singletonList(new StateOption("value2", "label2")));

        fragment = source.merge(sourceWithOptions);

        assertThat(fragment.getOptions(), is(not(sourceWithOptions.getOptions())));
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
