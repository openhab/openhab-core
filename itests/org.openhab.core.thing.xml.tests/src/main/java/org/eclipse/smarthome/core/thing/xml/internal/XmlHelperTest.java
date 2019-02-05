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
package org.eclipse.smarthome.core.thing.xml.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * @author Simon Kaufmann - Initial contribution and API
 *
 */
public class XmlHelperTest {

    @Test
    public void whenUIDContainsDot_shouldBeconvcertedToColon() {
        assertThat(XmlHelper.getSystemUID("system.test"), is("system:test"));
    }

    @Test
    public void whenNoPrefixIsGiven_shouldPrependSystemPrefix() {
        assertThat(XmlHelper.getSystemUID("test"), is("system:test"));
    }
}
