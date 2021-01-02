/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.config.core.status;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.openhab.core.config.core.status.ConfigStatusMessage.Type;

/**
 * Testing the {@link ConfigStatusInfo}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class ConfigStatusInfoTest {

    private static final String PARAM1 = "param1";
    private static final String PARAM2 = "param2";
    private static final String PARAM3 = "param3";
    private static final String PARAM4 = "param4";
    private static final String PARAM5 = "param5";
    private static final String PARAM6 = "param6";

    private static final String INFO1 = "info1";
    private static final String INFO2 = "info2";
    private static final String WARNING1 = "warning1";
    private static final String WARNING2 = "warning2";
    private static final String ERROR1 = "error1";
    private static final String ERROR2 = "error2";

    private static final ConfigStatusMessage MSG1 = ConfigStatusMessage.Builder.information(PARAM1)
            .withMessageKeySuffix(INFO1).build();
    private static final ConfigStatusMessage MSG2 = ConfigStatusMessage.Builder.information(PARAM2)
            .withMessageKeySuffix(INFO2).withStatusCode(1).build();
    private static final ConfigStatusMessage MSG3 = ConfigStatusMessage.Builder.warning(PARAM3)
            .withMessageKeySuffix(WARNING1).build();
    private static final ConfigStatusMessage MSG4 = ConfigStatusMessage.Builder.warning(PARAM4)
            .withMessageKeySuffix(WARNING2).withStatusCode(1).build();
    private static final ConfigStatusMessage MSG5 = ConfigStatusMessage.Builder.error(PARAM5)
            .withMessageKeySuffix(ERROR1).build();
    private static final ConfigStatusMessage MSG6 = ConfigStatusMessage.Builder.pending(PARAM6)
            .withMessageKeySuffix(ERROR2).withStatusCode(1).build();

    private static final List<ConfigStatusMessage> ALL = Stream.of(MSG1, MSG2, MSG3, MSG4, MSG5, MSG6)
            .collect(toList());

    @Test
    public void assertCorrectConfigErrorHandlingForEmptyResultObject() {
        ConfigStatusInfo info = new ConfigStatusInfo();
        assertThat(info.getConfigStatusMessages().size(), is(0));
    }

    @Test
    public void assertCorrectConfigStatusInfoHandlingUusingConstructor() {
        assertConfigStatusInfo(new ConfigStatusInfo(ALL));
    }

    @Test
    public void assertCorrectConfigErrorHandlingUsingAddConfigErrors() {
        ConfigStatusInfo info = new ConfigStatusInfo();
        info.add(ALL);
        assertConfigStatusInfo(info);
    }

    @Test
    public void assertCorrectConfigErrorHandlingUsingAddConfigError() {
        ConfigStatusInfo info = new ConfigStatusInfo();
        for (ConfigStatusMessage configStatusMessage : ALL) {
            info.add(configStatusMessage);
        }
        assertConfigStatusInfo(info);
    }

    @Test
    public void assertNPEisThrownIfTypesAreNull() {
        ConfigStatusInfo info = new ConfigStatusInfo();
        assertThrows(NullPointerException.class, () -> info.getConfigStatusMessages(null, emptySet()));
    }

    @Test
    public void assertNPEisThrownIfParameterNamesAreNull() {
        ConfigStatusInfo info = new ConfigStatusInfo();
        assertThrows(NullPointerException.class, () -> info.getConfigStatusMessages(emptySet(), null));
    }

    private void assertConfigStatusInfo(ConfigStatusInfo info) {
        assertThat(info.getConfigStatusMessages().size(), is(ALL.size()));
        assertThat(info.getConfigStatusMessages(), hasItems(MSG1, MSG2, MSG3, MSG4, MSG5, MSG6));

        assertThat(info.getConfigStatusMessages(Type.INFORMATION).size(), is(2));
        assertThat(info.getConfigStatusMessages(Type.INFORMATION), hasItems(MSG1, MSG2));

        assertThat(info.getConfigStatusMessages(Type.WARNING).size(), is(2));
        assertThat(info.getConfigStatusMessages(Type.WARNING), hasItems(MSG3, MSG4));

        assertThat(info.getConfigStatusMessages(Type.ERROR).size(), is(1));
        assertThat(info.getConfigStatusMessages(Type.ERROR), hasItems(MSG5));

        assertThat(info.getConfigStatusMessages(Type.PENDING).size(), is(1));
        assertThat(info.getConfigStatusMessages(Type.PENDING), hasItems(MSG6));

        assertThat(info.getConfigStatusMessages(Type.INFORMATION, Type.WARNING).size(), is(4));
        assertThat(info.getConfigStatusMessages(Type.INFORMATION, Type.WARNING), hasItems(MSG1, MSG2, MSG3, MSG4));

        assertThat(info.getConfigStatusMessages(PARAM1).size(), is(1));
        assertThat(info.getConfigStatusMessages(PARAM1), hasItem(MSG1));

        assertThat(info.getConfigStatusMessages(PARAM2).size(), is(1));
        assertThat(info.getConfigStatusMessages(PARAM2), hasItem(MSG2));

        assertThat(info.getConfigStatusMessages(PARAM3, PARAM4).size(), is(2));
        assertThat(info.getConfigStatusMessages(PARAM3, PARAM4), hasItems(MSG3, MSG4));

        assertThat(info.getConfigStatusMessages(Set.of(Type.INFORMATION, Type.WARNING), Set.of(PARAM1, PARAM6)).size(),
                is(5));
        assertThat(info.getConfigStatusMessages(Set.of(Type.INFORMATION, Type.WARNING), Set.of(PARAM1, PARAM6)),
                hasItems(MSG1, MSG2, MSG3, MSG4, MSG6));

        assertThat(info.getConfigStatusMessages("unknown").size(), is(0));
    }
}
