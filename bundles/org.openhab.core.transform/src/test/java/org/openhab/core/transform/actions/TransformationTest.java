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
package org.openhab.core.transform.actions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openhab.core.transform.TransformationException;

/**
 * @author Stefan Triller - Initial contribution
 */
public class TransformationTest {

    @Test
    public void testTransform() {
        String result = Transformation.transform("UnknownTransformation", "function", "test");
        assertEquals("test", result);
    }

    @Test
    public void testTransformRaw() {
        try {
            Transformation.transformRaw("UnknownTransformation", "function", "test");
        } catch (TransformationException e) {
            assertEquals("No transformation service 'UnknownTransformation' could be found.", e.getMessage());
        }
    }
}
