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
package org.openhab.core.rulestest.manipulator;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.test.java.JavaTest;

/**
 * Helper functions to manipulate Switch items.
 *
 * @author Florian Schmidt - Initial contribution
 *
 */
@NonNullByDefault
public class TestSwitchItem extends JavaTest {
    private final SwitchItem switchItem;

    private TestSwitchItem(SwitchItem item) {
        switchItem = item;
    }

    public static void send(SwitchItem item, OnOffType newState) {
        new TestSwitchItem(item).send(newState);
    }

    private void send(OnOffType newState) {
        switchItem.send(newState);
        waitFor(() -> switchItem.getState().equals(newState));
    }
}
