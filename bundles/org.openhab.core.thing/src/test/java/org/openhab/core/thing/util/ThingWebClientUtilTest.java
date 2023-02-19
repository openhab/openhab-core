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
package org.openhab.core.thing.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.thing.ThingUID;

/**
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class ThingWebClientUtilTest {

    private ThingUID uid1 = new ThingUID("mycroft", "mycroft", "yy");
    private ThingUID uid2 = new ThingUID("mycroft", "mycroft", "myInstance");
    private ThingUID uid3 = new ThingUID("mycroft", "mycroft", "myPersonalInstance");
    private ThingUID uid4 = new ThingUID("amazonechocontrol", "account", "myAccount");

    @Test
    public void testBuildWebClientConsumerNameWhenThingUidSizeIsOk() {
        String name = ThingWebClientUtil.buildWebClientConsumerName(uid1, null);
        assertThat(name, is("mycroft-mycroft-yy"));
    }

    @Test
    public void testBuildWebClientConsumerNameWhenPrefixAndThingUidSizeIsOk() {
        String name = ThingWebClientUtil.buildWebClientConsumerName(uid1, "x-");
        assertThat(name, is("x-mycroft-mycroft-yy"));
    }

    @Test
    public void testBuildWebClientConsumerNameWhenPrefixIsTooBig() {
        String name = ThingWebClientUtil.buildWebClientConsumerName(uid1, "xxxx-");
        assertThat(name, is("xxxx-mycro-yy"));
    }

    @Test
    public void testBuildWebClientConsumerNameWhenThingUidIsTooBig() {
        String name = ThingWebClientUtil.buildWebClientConsumerName(uid2, null);
        assertThat(name, is("mycroft-myInstance"));
    }

    @Test
    public void testBuildWebClientConsumerNameWhenThingIdIsTooBig() {
        String name = ThingWebClientUtil.buildWebClientConsumerName(uid3, null);
        String hashCode = ThingWebClientUtil.buildHashCode(uid3, 12);
        assertThat(name, is("mycroft-" + hashCode));
    }

    @Test
    public void testBuildWebClientConsumerNameWhenBindingIdIsTooBig() {
        String name = ThingWebClientUtil.buildWebClientConsumerName(uid4, null);
        assertThat(name, is("amazonecho-myAccount"));
    }
}
