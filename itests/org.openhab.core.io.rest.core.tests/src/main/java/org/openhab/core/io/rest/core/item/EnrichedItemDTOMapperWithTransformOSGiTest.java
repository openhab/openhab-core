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
package org.openhab.core.io.rest.core.item;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.service.StateDescriptionService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;

/**
 * @author Henning Treu - Initial contribution
 */
public class EnrichedItemDTOMapperWithTransformOSGiTest extends JavaOSGiTest {

    private static final String ITEM_NAME = "Item1";

    @Mock
    private StateDescriptionService stateDescriptionService;

    @Before
    public void setup() {
        initMocks(this);

        StateDescription stateDescription = new StateDescription(BigDecimal.ZERO, BigDecimal.valueOf(100),
                BigDecimal.TEN, "%d °C", true, Collections.singletonList(new StateOption("SOUND", "My great sound.")));
        when(stateDescriptionService.getStateDescription(ITEM_NAME, null)).thenReturn(stateDescription);
    }

    @Test
    public void shouldConsiderTraformationWhenPresent() {
        NumberItem item1 = new NumberItem("Item1");
        item1.setState(new DecimalType("12.34"));
        item1.setStateDescriptionService(stateDescriptionService);

        EnrichedItemDTO enrichedDTO = EnrichedItemDTOMapper.map(item1, false, null, null, null);
        assertThat(enrichedDTO, is(notNullValue()));
        assertThat(enrichedDTO.name, is("Item1"));
        assertThat(enrichedDTO.state, is("12.34"));

        StateDescription sd = enrichedDTO.stateDescription;
        assertThat(sd.getMinimum(), is(BigDecimal.valueOf(0)));
        assertThat(sd.getMaximum(), is(BigDecimal.valueOf(100)));
        assertThat(sd.getStep(), is(BigDecimal.valueOf(10)));
        assertThat(sd.getPattern(), is("%d °C"));
        assertThat(sd.getOptions().get(0).getValue(), is("SOUND"));
        assertThat(sd.getOptions().get(0).getLabel(), is("My great sound."));
    }
}
