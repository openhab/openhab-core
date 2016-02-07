/**
 * Copyright (c) 2015-2016 Kai Kreuzer and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.core.compat1x.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openhab.core.library.types.DateTimeType;

public class TypeMapperTest {

    @Test
    public void testDateTypeType() {
        DateTimeType ohType1 = new DateTimeType();
        DateTimeType ohType2 = (DateTimeType) TypeMapper.mapToOpenHABType(TypeMapper.mapToESHType(ohType1));
        assertEquals(ohType1, ohType2);
    }

}
