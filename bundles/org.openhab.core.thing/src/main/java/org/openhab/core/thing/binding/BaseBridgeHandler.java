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
package org.openhab.core.thing.binding;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.BridgeBuilder;

/**
 * The {@link BaseBridgeHandler} implements the {@link BridgeHandler} interface and adds some convenience methods for
 * bridges to the {@link BaseThingHandler}.
 * <p>
 * It is recommended to extend this abstract base class.
 * <p>
 *
 * @author Dennis Nobel - Initial contribution
 * @author Stefan Bußweiler - Added implementation of BridgeHandler interface
 */
@NonNullByDefault
public abstract class BaseBridgeHandler extends BaseThingHandler implements BridgeHandler {

    /**
     * @see BaseThingHandler
     */
    public BaseBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public Bridge getThing() {
        return (Bridge) super.getThing();
    }

    /**
     * Creates a bridge builder, which allows to modify the bridge. The method
     * {@link BaseThingHandler#updateThing(Thing)} must be called to persist the changes.
     *
     * @return {@link BridgeBuilder} which builds an exact copy of the bridge
     */
    @Override
    protected BridgeBuilder editThing() {
        return BridgeBuilder.create(thing.getThingTypeUID(), thing.getUID()).withBridge(thing.getBridgeUID())
                .withChannels(thing.getChannels()).withConfiguration(thing.getConfiguration())
                .withLabel(thing.getLabel()).withLocation(thing.getLocation()).withProperties(thing.getProperties());
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        // do nothing by default, can be overridden by subclasses
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        // do nothing by default, can be overridden by subclasses
    }
}
