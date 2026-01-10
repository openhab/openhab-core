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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.module.script.providersupport.internal.ProviderRegistry;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;

/**
 * The {@link ProviderThingRegistryDelegate} is wrapping a {@link ThingRegistry} to provide a comfortable way to provide
 * Things from scripts without worrying about the need to remove Things again when the script is unloaded.
 * Nonetheless, using the {@link #addPermanent(Thing)} method it is still possible to add Things permanently.
 * <p>
 * Use a new instance of this class for each {@link javax.script.ScriptEngine}.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
public class ProviderThingRegistryDelegate implements ThingRegistry, ProviderRegistry {
    private final ThingRegistry thingRegistry;

    private final Set<ThingUID> things = new HashSet<>();

    private final ScriptedThingProvider scriptedProvider;

    public ProviderThingRegistryDelegate(ThingRegistry thingRegistry, ScriptedThingProvider scriptedProvider) {
        this.thingRegistry = thingRegistry;
        this.scriptedProvider = scriptedProvider;
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<Thing> listener) {
        thingRegistry.addRegistryChangeListener(listener);
    }

    @Override
    public Collection<Thing> getAll() {
        return thingRegistry.getAll();
    }

    @Override
    public Stream<Thing> stream() {
        return thingRegistry.stream();
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<Thing> listener) {
        thingRegistry.removeRegistryChangeListener(listener);
    }

    @Override
    public Thing add(Thing element) {
        ThingUID thingUID = element.getUID();
        // Check for Thing already existing here because the Thing might exist in a different provider, so we need to
        // check the registry and not only the provider itself
        if (get(thingUID) != null) {
            throw new IllegalArgumentException(
                    "Cannot add Thing, because a Thing with same UID (" + thingUID + ") already exists.");
        }

        scriptedProvider.add(element);
        things.add(thingUID);

        return element;
    }

    /**
     * Add a Thing permanently to the registry.
     * This Thing will be kept in the registry even if the script is unloaded.
     *
     * @param element the Thing to be added (must not be null)
     * @return the added Thing
     */
    public Thing addPermanent(Thing element) {
        return thingRegistry.add(element);
    }

    @Override
    public @Nullable Thing update(Thing element) {
        if (things.contains(element.getUID())) {
            return scriptedProvider.update(element);
        }

        return thingRegistry.update(element);
    }

    @Override
    public @Nullable Thing get(ThingUID uid) {
        return thingRegistry.get(uid);
    }

    @Override
    public @Nullable Channel getChannel(ChannelUID channelUID) {
        return thingRegistry.getChannel(channelUID);
    }

    @Override
    public void updateConfiguration(ThingUID thingUID, Map<String, Object> configurationParameters) {
        thingRegistry.updateConfiguration(thingUID, configurationParameters);
    }

    @Override
    public @Nullable Thing remove(ThingUID thingUID) {
        // Give the ThingHandler the chance to perform any removal operations instead of forcefully removing from
        // ScriptedThingProvider
        // If the Thing was provided by ScriptedThingProvider, it will be removed from there by listening to
        // ThingStatusEvent for ThingStatus.REMOVED in ScriptedThingProvider
        return thingRegistry.remove(thingUID);
    }

    @Override
    public void removeAllAddedByScript() {
        for (ThingUID thing : things) {
            scriptedProvider.remove(thing);
        }
        things.clear();
    }

    @Override
    public @Nullable Thing forceRemove(ThingUID thingUID) {
        if (things.remove(thingUID)) {
            return scriptedProvider.remove(thingUID);
        }

        return thingRegistry.forceRemove(thingUID);
    }

    @Override
    public @Nullable Thing createThingOfType(ThingTypeUID thingTypeUID, @Nullable ThingUID thingUID,
            @Nullable ThingUID bridgeUID, @Nullable String label, Configuration configuration) {
        return thingRegistry.createThingOfType(thingTypeUID, thingUID, bridgeUID, label, configuration);
    }
}
