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
package org.eclipse.smarthome.core.transform.actions;

import static org.junit.Assert.assertEquals;

import org.eclipse.smarthome.core.transform.TransformationException;
import org.junit.Test;

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
