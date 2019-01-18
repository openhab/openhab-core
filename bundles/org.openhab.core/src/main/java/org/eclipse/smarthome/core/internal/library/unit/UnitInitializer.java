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
package org.eclipse.smarthome.core.internal.library.unit;

import org.eclipse.smarthome.core.library.unit.ImperialUnits;
import org.eclipse.smarthome.core.library.unit.SIUnits;

/**
 * Make sure static blocks from {@link SIUnits} & {@link ImperialUnits} are executed to initialize the unit parser.
 *
 * @author Henning Treu - initial contribution
 *
 */
public class UnitInitializer {

    static {
        SIUnits.getInstance();
        ImperialUnits.getInstance();
    }

    public static void init() {
        // make sure the static block gets executed.
    }
}
