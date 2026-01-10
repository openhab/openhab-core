/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.providersupport.shared;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingProvider;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.events.ThingStatusInfoEvent;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This {@link ThingProvider} keeps things provided by scripts during runtime.
 * This ensures that things are not kept on reboot, but have to be provided by the scripts again.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { ScriptedThingProvider.class, ThingProvider.class, EventSubscriber.class })
public class ScriptedThingProvider extends AbstractProvider<Thing>
        implements ThingProvider, ManagedProvider<Thing, ThingUID>, EventSubscriber {
    private final Logger logger = LoggerFactory.getLogger(ScriptedThingProvider.class);
    private final Map<ThingUID, Thing> things = new HashMap<>();

    @Override
    public Collection<Thing> getAll() {
        return things.values();
    }

    @Override
    public @Nullable Thing get(ThingUID uid) {
        return things.get(uid);
    }

    @Override
    public void add(Thing thing) {
        if (things.get(thing.getUID()) != null) {
            throw new IllegalArgumentException(
                    "Cannot add thing, because a thing with same UID (" + thing.getUID() + ") already exists.");
        }
        things.put(thing.getUID(), thing);

        notifyListenersAboutAddedElement(thing);
    }

    @Override
    public @Nullable Thing update(Thing thing) {
        Thing oldThing = things.get(thing.getUID());
        if (oldThing != null) {
            things.put(thing.getUID(), thing);
            notifyListenersAboutUpdatedElement(oldThing, thing);
        } else {
            logger.warn("Cannot update thing with UID '{}', because it does not exist.", thing.getUID());
        }
        return oldThing;
    }

    @Override
    public @Nullable Thing remove(ThingUID uid) {
        Thing thing = things.remove(uid);
        if (thing != null) {
            notifyListenersAboutRemovedElement(thing);
        }
        return thing;
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(ThingStatusInfoEvent.TYPE);
    }

    @Override
    public void receive(Event event) {
        if (event instanceof ThingStatusInfoEvent thingStatusInfoEvent) {
            if (thingStatusInfoEvent.getStatusInfo().getStatus() == ThingStatus.REMOVED) {
                remove(thingStatusInfoEvent.getThingUID());
            }
        }
    }
}
