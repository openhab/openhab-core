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
package org.openhab.core.config.discovery;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;

/**
 * The {@link AbstractThingHandlerDiscoveryService} extends the {@link AbstractDiscoveryService} for thing-based
 * discovery services.
 *
 * It handles the injection of the {@link ThingHandler}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractThingHandlerDiscoveryService<T extends ThingHandler> extends AbstractDiscoveryService
        implements ThingHandlerService {

    private final Class<T> thingClazz;
    protected @Nullable T thingHandler;

    public AbstractThingHandlerDiscoveryService(Class<T> thingClazz, @Nullable Set<ThingTypeUID> supportedThingTypes,
            int timeout, boolean backgroundDiscoveryEnabledByDefault) throws IllegalArgumentException {
        super(supportedThingTypes, timeout, backgroundDiscoveryEnabledByDefault);
        this.thingClazz = thingClazz;
    }

    public AbstractThingHandlerDiscoveryService(Class<T> thingClazz, @Nullable Set<ThingTypeUID> supportedThingTypes,
            int timeout) throws IllegalArgumentException {
        super(supportedThingTypes, timeout);
        this.thingClazz = thingClazz;
    }

    public AbstractThingHandlerDiscoveryService(Class<T> thingClazz, int timeout) throws IllegalArgumentException {
        super(timeout);
        this.thingClazz = thingClazz;
    }

    @Override
    protected abstract void startScan();

    @Override
    @SuppressWarnings("unchecked")
    public void setThingHandler(ThingHandler handler) {
        if (thingClazz.isAssignableFrom(handler.getClass())) {
            this.thingHandler = (T) handler;
        } else {
            throw new IllegalArgumentException(
                    "Expected class is " + thingClazz + " but the parameter has class " + handler.getClass());
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return thingHandler;
    }

    @Override
    public void activate(@Nullable Map<String, Object> config) {
        super.activate(config);
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }
}
