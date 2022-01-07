/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import static org.eclipse.jdt.annotation.Checks.requireNonNull;

import java.net.URL;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * The {@link Example} test case is a usage example how the according {@code ThingType} parser
 * can be used. This example can also be used for manual tests when the schema is extended or
 * changed.
 *
 * @author Michael Grammling - Initial contribution
 */
@NonNullByDefault
public class Example {

    @Test
    public void test() throws Exception {
        ClassLoader classLoader = requireNonNull(Example.class.getClassLoader());
        URL channelsURL = requireNonNull(classLoader.getResource("/example/example.xml"));

        ThingDescriptionReader reader = new ThingDescriptionReader();
        reader.readFromXML(channelsURL);
    }
}
