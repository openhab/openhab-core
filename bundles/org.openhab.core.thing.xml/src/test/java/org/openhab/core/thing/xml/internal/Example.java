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
package org.openhab.core.thing.xml.internal;

import java.net.URL;

import org.junit.Test;

/**
 * The {@link Example} test case is a usage example how the according {@code ThingType} parser
 * can be used. This example can also be used for manual tests when the schema is extended or
 * changed.
 *
 * @author Michael Grammling - Initial contribution
 */
public class Example {

    @SuppressWarnings("unchecked")
    @Test
    public void test() throws Exception {
        URL channelsURL = Example.class.getClassLoader().getResource("/example/example.xml");

        ThingDescriptionReader reader = new ThingDescriptionReader();
        ThingDescriptionList thingList = (ThingDescriptionList) reader.readFromXML(channelsURL);
    }
}
