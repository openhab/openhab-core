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
package org.openhab.core.config.core;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link ConfigDescriptionParameterGroupBuilder) class.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ConfigDescriptionParameterGroupBuilderTest {

    private @NonNullByDefault({}) ConfigDescriptionParameterGroupBuilder builder;

    @Before
    public void setup() {
        builder = ConfigDescriptionParameterGroupBuilder.create("test") //
                .withContext("My Context") //
                .withAdvanced(Boolean.TRUE) //
                .withLabel("My Label") //
                .withDescription("My Description");
    }

    @Test
    public void testConfigDescriptionParameterGroupBuilder() {
        ConfigDescriptionParameterGroup group = builder.build();
        assertThat(group.getName(), is("test"));
        assertThat(group.getContext(), is("My Context"));
        assertThat(group.isAdvanced(), is(true));
        assertThat(group.getLabel(), is("My Label"));
        assertThat(group.getDescription(), is("My Description"));
    }

    @Test
    public void subsequentBuildsCreateIndependentConfigDescriptionParameterGroups() {
        ConfigDescriptionParameterGroup group = builder.build();
        ConfigDescriptionParameterGroup otherGroup = builder.withContext("My Second Context") //
                .withAdvanced(Boolean.FALSE) //
                .withLabel("My Second Label") //
                .withDescription("My Second Description") //
                .build();

        assertThat(otherGroup.getName(), is(group.getName()));
        assertThat(otherGroup.getContext(), is(not(group.getContext())));
        assertThat(otherGroup.isAdvanced(), is(not(group.isAdvanced())));
        assertThat(otherGroup.getLabel(), is(not(group.getLabel())));
        assertThat(otherGroup.getDescription(), is(not(group.getDescription())));
    }
}
