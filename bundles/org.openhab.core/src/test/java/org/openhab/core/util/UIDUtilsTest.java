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
package org.openhab.core.util;

import java.util.function.Consumer;

import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.openhab.core.common.AbstractUID;

/**
 * Tests for {@link AbstractUID}.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class UIDUtilsTest {

    @Test
    public void encodeDecode() {
        Consumer<String> test = in -> {
            final String encoded = UIDUtils.encode(in);
            final String decoded = UIDUtils.decode(encoded);
            System.out.printf("in: %s%n encoded: %s%n decoded: %s%n equals: %b%n", in, encoded, decoded,
                    in.equals(decoded));
            Assert.assertThat(decoded, IsEqual.equalTo(in));
        };
        test.accept("test");
        test.accept("TEST");
        test.accept("test123TEST");
        test.accept("test_test-test%test");
        test.accept("äöø€");
    }

}
