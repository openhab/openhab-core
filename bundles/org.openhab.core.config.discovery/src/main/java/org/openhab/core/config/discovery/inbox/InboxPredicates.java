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
package org.openhab.core.config.discovery.inbox;

import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultFlag;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * Implements static factory methods for {@link Predicate}s to filter in streams of {@link DiscoveryResult}s.
 *
 * @author Andre Fuechsel - Initial contribution
 */
@NonNullByDefault
public class InboxPredicates {

    public static Predicate<DiscoveryResult> forBinding(@Nullable String bindingId) {
        return r -> bindingId != null && bindingId.equals(r.getBindingId());
    }

    public static Predicate<DiscoveryResult> forThingTypeUID(@Nullable ThingTypeUID uid) {
        return r -> uid != null && uid.equals(r.getThingTypeUID());
    }

    public static Predicate<DiscoveryResult> forThingUID(@Nullable ThingUID thingUID) {
        return r -> thingUID != null && thingUID.equals(r.getThingUID());
    }

    public static Predicate<DiscoveryResult> withFlag(DiscoveryResultFlag flag) {
        return r -> flag == r.getFlag();
    }

    public static Predicate<DiscoveryResult> withProperty(@Nullable String propertyName, String propertyValue) {
        return r -> r.getProperties().containsKey(propertyName)
                && r.getProperties().get(propertyName).equals(propertyValue);
    }

    public static Predicate<DiscoveryResult> withRepresentationProperty(@Nullable String propertyName) {
        return r -> propertyName != null && propertyName.equals(r.getRepresentationProperty());
    }

    public static Predicate<DiscoveryResult> withRepresentationPropertyValue(@Nullable String propertyValue) {
        return r -> propertyValue != null && propertyValue.equals(r.getProperties().get(r.getRepresentationProperty()));
    }
}
