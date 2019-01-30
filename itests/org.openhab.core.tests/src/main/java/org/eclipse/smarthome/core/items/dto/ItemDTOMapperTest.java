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
/**
 * @author Stefan Triller - initial contribution
 */
package org.eclipse.smarthome.core.items.dto;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.eclipse.smarthome.core.items.GroupFunction;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.types.ArithmeticGroupFunction;
import org.eclipse.smarthome.core.library.types.StringType;
import org.junit.Test;

public class ItemDTOMapperTest {

    @Test
    public void testMapFunctionWithNumberItemAndCountFunction() {

        // testing Group:Number:Count(".*hello.*")
        NumberItem item1 = new NumberItem("item1");

        GroupFunctionDTO gFuncDTO = new GroupFunctionDTO();
        gFuncDTO.name = "COUNT";
        gFuncDTO.params = new String[] { ".*hello.*" };

        GroupFunction gFunc = ItemDTOMapper.mapFunction(item1, gFuncDTO);

        assertThat(gFunc, instanceOf(ArithmeticGroupFunction.Count.class));
        assertThat(gFunc.getParameters().length, is(1));
        assertThat(gFunc.getParameters()[0], instanceOf(StringType.class));

    }

}
