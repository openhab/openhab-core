/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.transform.TransformationException;

/**
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class TransformationTest {

    @Test
    public void testTransform() {
        String result = Transformation.transform("UnknownTransformation", "function", "test");
        assertThat(result, is("test"));
    }

    @Test
    public void testTransformRaw() {
        TransformationException e = assertThrows(TransformationException.class,
                () -> Transformation.transformRaw("UnknownTransformation", "function", "test"));
        assertThat(e.getMessage(), is("No transformation service 'UnknownTransformation' could be found."));
    }
}
