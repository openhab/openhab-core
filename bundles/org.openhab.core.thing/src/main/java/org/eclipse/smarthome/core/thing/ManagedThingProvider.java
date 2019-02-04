/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
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
package org.eclipse.smarthome.core.thing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.DefaultAbstractManagedProvider;
import org.eclipse.smarthome.core.service.ReadyMarker;
import org.eclipse.smarthome.core.service.ReadyMarkerFilter;
import org.eclipse.smarthome.core.service.ReadyService;
import org.eclipse.smarthome.core.storage.StorageService;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.util.BundleResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * {@link ManagedThingProvider} is an OSGi service, that allows to add or remove
 * things at runtime by calling {@link ManagedThingProvider#addThing(Thing)} or
 * {@link ManagedThingProvider#removeThing(Thing)}. An added thing is
 * automatically exposed to the {@link ThingRegistry}.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Dennis Nobel - Integrated Storage
 * @author Michael Grammling - Added dynamic configuration update
 * @author Jan N. Klug - Added dynamic channel updates
 */
@Component(immediate = true, service = { ThingProvider.class, ManagedThingProvider.class })
public class ManagedThingProvider extends DefaultAbstractManagedProvider<Thing, ThingUID>
        implements ThingProvider, ReadyService.ReadyTracker {
    private static final String XML_THING_TYPE = "esh.xmlThingTypes";

    private Set<String> loadedXmlThingTypes = new CopyOnWriteArraySet<>();
    private List<ThingHandlerFactory> thingHandlerFactories = new CopyOnWriteArrayList<ThingHandlerFactory>();

    private BundleResolver bundleResolver;

    private Collection<Thing> thingsList = new ArrayList<>();

    @Override
    protected String getStorageName() {
        return Thing.class.getName();
    }

    @Override
    protected String keyToString(ThingUID key) {
        return key.toString();
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    @Override
    protected void setStorageService(StorageService storageService) {
        super.setStorageService(storageService);
    }

    @Override
    protected void unsetStorageService(StorageService storageService) {
        super.unsetStorageService(storageService);
    }

    @Reference
    protected void setBundleResolver(BundleResolver bundleResolver) {
        this.bundleResolver = bundleResolver;
    }

    protected void unsetBundleResolver(BundleResolver bundleResolver) {
        this.bundleResolver = null;
    }

    @Reference
    protected void setReadyService(ReadyService readyService) {
        readyService.registerTracker(this, new ReadyMarkerFilter().withType(XML_THING_TYPE));
        logger.info("set {}", readyService);
    }

    protected void unsetReadyService(ReadyService readyService) {
        readyService.unregisterTracker(this);
        logger.info("unset {}", readyService);
    }

    @Override
    public void onReadyMarkerAdded(ReadyMarker readyMarker) {
        String bsn = readyMarker.getIdentifier();
        loadedXmlThingTypes.add(bsn);
        handleXmlThingTypesLoaded(bsn);
        logger.info("marker added {}", readyMarker);
    }

    private void handleXmlThingTypesLoaded(String bsn) {
        thingHandlerFactories.stream().filter(thingHandlerFactory -> getBundleName(thingHandlerFactory).equals(bsn))
                .forEach(thingHandlerFactory -> thingHandlerFactoryAdded(thingHandlerFactory));
    }

    @Override
    public void onReadyMarkerRemoved(ReadyMarker readyMarker) {
        String bsn = readyMarker.getIdentifier();
        loadedXmlThingTypes.remove(bsn);
        logger.info("marker removed {}", readyMarker);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        logger.debug("ThingHandlerFactory added {}", thingHandlerFactory);
        thingHandlerFactories.add(thingHandlerFactory);
        thingHandlerFactoryAdded(thingHandlerFactory);
    }

    protected void removeThingHandlerFactory(ThingHandlerFactory thingHandlerFactory) {
        thingHandlerFactories.remove(thingHandlerFactory);
        thingHandlerFactoryRemoved();
    }

    private void thingHandlerFactoryRemoved() {
        // Don't do anything, Things should not be deleted
    }

    private void thingHandlerFactoryAdded(ThingHandlerFactory thingHandlerFactory) {
        Collection<Thing> storageList = super.getAll();
        storageList.stream().forEach(thing -> createThingFromStorageForThingHandlerFactory(thing, thingHandlerFactory));
    }

    @Override
    public Collection<Thing> getAll() {
        return thingsList;
    }

    private boolean compareChannels(Channel a, Channel b) {
        if (!a.getUID().equals(b.getUID())) {
            return false;
        }

        ChannelTypeUID aChannelTypeUID = a.getChannelTypeUID();
        ChannelTypeUID bChannelTypeUID = b.getChannelTypeUID();
        if (aChannelTypeUID != null) {
            if (!aChannelTypeUID.equals(bChannelTypeUID)) {
                return false;
            }
        } else if (bChannelTypeUID != null) {
            return false;
        }

        String aAcceptedItemType = a.getAcceptedItemType();
        String bAcceptedItemType = b.getAcceptedItemType();

        if (aAcceptedItemType != null) {
            if (!aAcceptedItemType.equals(bAcceptedItemType)) {
                return false;
            }
        } else if (bAcceptedItemType != null) {
            return false;
        }

        return true;
    }

    private void createThingFromStorageForThingHandlerFactory(Thing storedThing, ThingHandlerFactory factory) {
        if (!loadedXmlThingTypes.contains(getBundleName(factory))) {
            return;
        }

        if (factory.supportsThingType(storedThing.getThingTypeUID())) {
            ThingBuilder thingBuilder = ThingBuilder.create(storedThing.getThingTypeUID(), storedThing.getUID())
                    .withBridge(storedThing.getBridgeUID()).withChannels(storedThing.getChannels())
                    .withConfiguration(storedThing.getConfiguration()).withLabel(storedThing.getLabel())
                    .withLocation(storedThing.getLocation()).withProperties(storedThing.getProperties());

            Configuration storedConfiguration = storedThing.getConfiguration();
            Thing factoryThing = factory.createThing(storedThing.getThingTypeUID(), storedConfiguration,
                    storedThing.getUID(), storedThing.getBridgeUID());

            if (factoryThing != null) {
                Map<ChannelUID, Channel> factoryChannels = factoryThing.getChannels().stream()
                        .collect(Collectors.toMap(Channel::getUID, channel -> channel));
                Map<ChannelUID, Channel> storedChannels = storedThing.getChannels().stream()
                        .collect(Collectors.toMap(Channel::getUID, channel -> channel));

                // check if we have new or updated channels
                factoryChannels.forEach((factoryChannelUID, factoryChannel) -> {
                    if (!storedChannels.containsKey(factoryChannelUID)) {
                        thingBuilder.withChannel(factoryChannel);
                        logger.trace("added channel {} to thing {}", factoryChannel, storedThing.getUID());
                    } else {
                        if (!compareChannels(factoryChannel, storedThing.getChannel(factoryChannelUID.getId()))) {
                            thingBuilder.withoutChannel(factoryChannelUID).withChannel(factoryChannel);
                            logger.trace("updated channel {} in thing {}", factoryChannel, storedThing.getUID());
                        }
                    }
                });

                // check if we have removed channels
                storedChannels.forEach((storedChannelUID, storedChannel) -> {
                    if (!factoryChannels.containsKey(storedChannelUID)) {
                        logger.trace("channel {} in thing {} no longer present in XML", storedChannel,
                                storedThing.getUID());
                    }
                });
            }
            Thing newThing = thingBuilder.build();

            if (thingsList.contains(storedThing)) {
                notifyListenersAboutUpdatedElement(storedThing, newThing);
                logger.debug("updated {}", newThing.getUID());
            } else {
                thingsList.add(newThing);
                notifyListenersAboutAddedElement(newThing);
                logger.debug("added {}", newThing.getUID());
            }
        }
    }

    private String getBundleName(ThingHandlerFactory thingHandlerFactory) {
        return bundleResolver.resolveBundle(thingHandlerFactory.getClass()).getSymbolicName();
    }

}
