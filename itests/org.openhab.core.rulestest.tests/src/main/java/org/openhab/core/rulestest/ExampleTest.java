/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.rulestest;

import static org.hamcrest.CoreMatchers.is;

import java.util.Arrays;
import java.util.List;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.junit.Ignore;
import org.junit.Test;

/**
 * An example test which shows how a user-generated test could look like for openHAB automation rules.
 *
 * @author Florian Schmidt - Initial contribution
 *
 */
public class ExampleTest extends RuleTest {
    private static final List<String> requestedItems = Arrays.asList("Temperature_Setpoint");

    @Test
    @Ignore
    public void initializesTeamperature() throws Exception {
        assertThatItemState(getItem("Temperature_Setpoint"), is(new DecimalType(22)));
    }

    @Override
    protected List<String> requestedItems() {
        return requestedItems;
    }
}
