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
package org.eclipse.smarthome.core.thing.xml.test;

import java.io.File;
import java.net.URL;

import org.eclipse.smarthome.core.thing.xml.internal.ThingDescriptionList;
import org.eclipse.smarthome.core.thing.xml.internal.ThingDescriptionReader;
import org.junit.Test;

/**
 * The {@link Example} test case is a usage example how the according {@code ThingType} parser
 * can be used. This example can also be used for manual tests when the schema is extended or
 * changed.
 *
 * @author Michael Grammling - Initial Contribution.
 */
public class Example {

    @SuppressWarnings("unchecked")
    @Test
    public void test() throws Exception {
        File file = new File("./example/example.xml");
        URL channelsURL = file.toURI().toURL();

        ThingDescriptionReader reader = new ThingDescriptionReader();
        ThingDescriptionList thingList = (ThingDescriptionList) reader.readFromXML(channelsURL);
    }
}
