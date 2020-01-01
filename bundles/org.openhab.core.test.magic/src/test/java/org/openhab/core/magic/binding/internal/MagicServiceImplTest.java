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
package org.openhab.core.magic.binding.internal;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.magic.binding.MagicService;

/**
 * Tests cases for {@link MagicService}.
 *
 * @author Henning Treu - Initial contribution
 */
public class MagicServiceImplTest {

    private static final String PARAMETER_NAME = "select_decimal_limit";

    private MagicService magicService;

    @Before
    public void setup() {
        magicService = new MagicServiceImpl();
    }

    @Test
    public void shouldProvideConfigOptionsForURIAndParameterName() {
        Collection<ParameterOption> parameterOptions = magicService.getParameterOptions(MagicService.CONFIG_URI,
                PARAMETER_NAME, null, null);

        assertThat(parameterOptions, hasSize(3));
    }

    @Test
    public void shouldProvidemtpyListForInvalidURI() {
        Collection<ParameterOption> parameterOptions = magicService.getParameterOptions(URI.create("system.audio"),
                PARAMETER_NAME, null, null);

        assertNull(parameterOptions);
    }

    @Test
    public void shouldProvidemtpyListForInvalidParameterName() {
        Collection<ParameterOption> parameterOptions = magicService.getParameterOptions(MagicService.CONFIG_URI,
                "some_param_name", null, null);

        assertNull(parameterOptions);
    }
}
