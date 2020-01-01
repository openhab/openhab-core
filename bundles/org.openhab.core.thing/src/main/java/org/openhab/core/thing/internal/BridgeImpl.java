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
package org.openhab.core.thing.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Denis Nobel - Initial contribution
 * @author Christoph Weitkamp - Added method `getThing(ThingUID)`
 */
@NonNullByDefault
public class BridgeImpl extends ThingImpl implements Bridge {

    private final transient Logger logger = LoggerFactory.getLogger(BridgeImpl.class);

    /*
     * !!! DO NOT CHANGE - We are not allowed to change the members of the BridgeImpl implementation as the storage for
     * things uses this implementation itself to store and restore the data.
     */
    private transient List<Thing> things = new CopyOnWriteArrayList<>();

    /**
     * Package protected default constructor to allow reflective instantiation.
     *
     * !!! DO NOT REMOVE - Gson needs it !!!
     */
    BridgeImpl() {
    }

    public BridgeImpl(ThingTypeUID thingTypeUID, String bridgeId) {
        super(thingTypeUID, bridgeId);
    }

    /**
     * @param thingTypeUID
     * @param thingUID
     * @throws IllegalArgumentException
     */
    public BridgeImpl(ThingTypeUID thingTypeUID, ThingUID thingUID) throws IllegalArgumentException {
        super(thingTypeUID, thingUID);
    }

    public void addThing(Thing thing) {
        things.add(thing);
    }

    public void removeThing(Thing thing) {
        things.remove(thing);
    }

    @Override
    public @Nullable Thing getThing(ThingUID thingUID) {
        for (Thing thing : things) {
            if (thing.getUID().equals(thingUID)) {
                return thing;
            }
        }
        return null;
    }

    @Override
    public List<Thing> getThings() {
        return Collections.unmodifiableList(new ArrayList<>(things));
    }

    @Override
    public @Nullable BridgeHandler getHandler() {
        BridgeHandler bridgeHandler = null;
        ThingHandler thingHandler = super.getHandler();
        if (thingHandler instanceof BridgeHandler) {
            bridgeHandler = (BridgeHandler) thingHandler;
        } else if (thingHandler != null) {
            logger.warn("Handler of bridge '{}' must implement BridgeHandler interface.", getUID());
        }
        return bridgeHandler;
    }

}
