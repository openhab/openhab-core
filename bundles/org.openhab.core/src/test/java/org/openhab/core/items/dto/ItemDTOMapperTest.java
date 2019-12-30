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
/**
 * @author Stefan Triller - Initial contribution
 */
package org.openhab.core.items.dto;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.openhab.core.items.GroupFunction;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.ArithmeticGroupFunction;
import org.openhab.core.library.types.StringType;

/**
 * @author Stefan Triller - Initial contribution
 */
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
