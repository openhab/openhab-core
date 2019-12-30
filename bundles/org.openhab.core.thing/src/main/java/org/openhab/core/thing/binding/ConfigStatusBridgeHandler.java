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
package org.openhab.core.thing.binding;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.status.ConfigStatusCallback;
import org.openhab.core.config.core.status.ConfigStatusProvider;
import org.openhab.core.thing.Bridge;

/**
 * The {@link ConfigStatusBridgeHandler} is an extension of {@link BaseBridgeHandler} that implements the
 * {@link ConfigStatusProvider} interface. It provides default implementations for
 * <ul>
 * <li>{@link ConfigStatusProvider#supportsEntity(String)}</li>
 * <li>{@link ConfigStatusProvider#setConfigStatusCallback(ConfigStatusCallback)}</li>
 * </ul>
 * Furthermore it overwrites {@link ThingHandler#handleConfigurationUpdate(Map)} and
 * {@link BaseBridgeHandler#updateConfiguration(Configuration)} to initiate a propagation of a new
 * configuration status. So sub classes need only to provide the current configuration status by implementing
 * {@link ConfigStatusProvider#getConfigStatus()}.
 *
 * @author Thomas Höfer - Initial contribution
 */
@NonNullByDefault
public abstract class ConfigStatusBridgeHandler extends BaseBridgeHandler implements ConfigStatusProvider {

    private @Nullable ConfigStatusCallback configStatusCallback;

    /**
     * Creates a new instance of this class for the given {@link Bridge}.
     *
     * @param bridge the bridge for this handler
     */
    public ConfigStatusBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public boolean supportsEntity(String entityId) {
        return getThing().getUID().getAsString().equals(entityId);
    }

    @Override
    public void setConfigStatusCallback(@Nullable ConfigStatusCallback configStatusCallback) {
        this.configStatusCallback = configStatusCallback;
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        super.handleConfigurationUpdate(configurationParameters);
        if (configStatusCallback != null) {
            configStatusCallback.configUpdated(new ThingConfigStatusSource(getThing().getUID().getAsString()));
        }
    }

    @Override
    protected void updateConfiguration(Configuration configuration) {
        super.updateConfiguration(configuration);
        if (configStatusCallback != null) {
            configStatusCallback.configUpdated(new ThingConfigStatusSource(getThing().getUID().getAsString()));
        }
    }

}
