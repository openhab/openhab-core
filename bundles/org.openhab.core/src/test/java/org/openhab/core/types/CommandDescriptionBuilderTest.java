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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the {@link CommandDescriptionBuilder}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class CommandDescriptionBuilderTest {

    private CommandDescriptionBuilder builder;

    @Before
    public void setup() {
        builder = CommandDescriptionBuilder.create();
    }

    @Test
    public void builderWithEmptyOptions() {
        List<CommandOption> options = Collections.emptyList();
        assertThat(builder.withCommandOptions(options).build().getCommandOptions(), is(options));
    }

    @Test
    public void builderWithOption() {
        CommandOption option = new CommandOption("command", "label");
        assertThat(builder.withCommandOption(option).build().getCommandOptions(), is(Arrays.asList(option)));
    }

    @Test
    public void builderWithOptions() {
        List<CommandOption> options = Arrays.asList(new CommandOption("command1", "label1"),
                new CommandOption("command2", "label2"));
        assertThat(builder.withCommandOptions(options).build().getCommandOptions(), is(options));
    }

    @Test
    public void subsequentBuildsCreateIndependentFragments() {
        CommandDescription commandDescription1 = builder
                .withCommandOptions(Collections.singletonList(new CommandOption("command", "label"))).build();
        CommandDescription commandDescription2 = builder.withCommandOptions(Collections.emptyList()).build();

        assertThat(commandDescription1.getCommandOptions(), is(not(commandDescription2.getCommandOptions())));
    }

}
