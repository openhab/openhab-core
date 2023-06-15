/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.persistence.filter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.library.items.StringItem;

/**
 * The {@link PersistenceTimeFilterTest} contains tests for {@link PersistenceTimeFilter}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class PersistenceTimeFilterTest {

    @Test
    public void testTimeFilter() throws InterruptedException {
        PersistenceFilter filter = new PersistenceTimeFilter("test", 1, "s");

        StringItem item = new StringItem("testItem");
        assertThat(filter.apply(item), is(true));
        filter.persisted(item);

        // immediate store returns false
        assertThat(filter.apply(item), is(false));

        // after interval returns true
        Thread.sleep(1500);
        assertThat(filter.apply(item), is(true));
    }
}
