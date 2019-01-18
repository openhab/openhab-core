/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
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
package org.eclipse.smarthome.core.thing.binding;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.core.validation.ConfigValidationException;
import org.eclipse.smarthome.core.common.ThreadPoolManager;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.eclipse.smarthome.core.thing.util.ThingHandlerHelper;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link BaseThingHandler} provides a base implementation for the {@link ThingHandler} interface.
 * <p>
 * The default behavior for {@link Thing} updates is to {@link #dispose()} this handler first, exchange the
 * {@link Thing} and {@link #initialize()} it again. Override the method {@link #thingUpdated(Thing)} to change the
 * default behavior.
 * <p>
 * It is recommended to extend this abstract base class, because it covers a lot of common logic.
 * <p>
 *
 * @author Dennis Nobel - Initial contribution
 * @author Michael Grammling - Added dynamic configuration update
 * @author Thomas Höfer - Added thing properties and config description validation
 * @author Stefan Bußweiler - Added new thing status handling, refactorings thing/bridge life cycle
 * @author Kai Kreuzer - Refactored isLinked method to not use deprecated functions anymore
 * @author Christoph Weitkamp - Moved OSGI ServiceTracker from BaseThingHandler to ThingHandlerCallback
 */
@NonNullByDefault
public abstract class BaseThingHandler implements ThingHandler {

    private static final String THING_HANDLER_THREADPOOL_NAME = "thingHandler";
    private final Logger logger = LoggerFactory.getLogger(BaseThingHandler.class);

    protected final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(THING_HANDLER_THREADPOOL_NAME);

    @Deprecated // this must not be used by bindings!
    @NonNullByDefault({})
    protected ThingRegistry thingRegistry;

    @Deprecated // this must not be used by bindings!
    @NonNullByDefault({})
    protected BundleContext bundleContext;

    protected Thing thing;

    @SuppressWarnings("rawtypes")
    @NonNullByDefault({})
    private ServiceTracker thingRegistryServiceTracker;

    private @Nullable ThingHandlerCallback callback;

    /**
     * Creates a new instance of this class for the {@link Thing}.
     *
     * @param thing the thing that should be handled, not null
     */
    public BaseThingHandler(Thing thing) {
        this.thing = thing;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        thingRegistryServiceTracker = new ServiceTracker(this.bundleContext, ThingRegistry.class.getName(), null) {
            @Override
            public Object addingService(final @Nullable ServiceReference reference) {
                thingRegistry = (ThingRegistry) bundleContext.getService(reference);
                return thingRegistry;
            }

            @Override
            public void removedService(final @Nullable ServiceReference reference, final @Nullable Object service) {
                synchronized (BaseThingHandler.this) {
                    thingRegistry = null;
                }
            }
        };
        thingRegistryServiceTracker.open();
    }

    public void unsetBundleContext(final BundleContext bundleContext) {
        thingRegistryServiceTracker.close();
        this.bundleContext = null;
    }

    @Override
    public void handleRemoval() {
        // can be overridden by subclasses
        updateStatus(ThingStatus.REMOVED);
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        if (!isModifyingCurrentConfig(configurationParameters)) {
            return;
        }

        validateConfigurationParameters(configurationParameters);

        // can be overridden by subclasses
        Configuration configuration = editConfiguration();
        for (Entry<String, Object> configurationParameter : configurationParameters.entrySet()) {
            configuration.put(configurationParameter.getKey(), configurationParameter.getValue());
        }

        if (isInitialized()) {
            // persist new configuration and reinitialize handler
            dispose();
            updateConfiguration(configuration);
            initialize();
        } else {
            // persist new configuration and notify Thing Manager
            updateConfiguration(configuration);
            if (this.callback != null) {
                this.callback.configurationUpdated(this.getThing());
            } else {
                logger.warn("Handler {} tried updating its configuration although the handler was already disposed.",
                        this.getClass().getSimpleName());
            }
        }
    }

    /**
     * Checks whether a given list of parameters would mean any change to the existing Thing configuration if applied to
     * it.
     * Note that the passed parameters might be a subset of the existing configuration.
     *
     * @param configurationParameters the parameters to check against the current configuration
     * @return true if the parameters would result in a modified configuration, false otherwise
     */
    protected boolean isModifyingCurrentConfig(Map<String, Object> configurationParameters) {
        Configuration currentConfig = getConfig();
        for (Entry<String, Object> entry : configurationParameters.entrySet()) {
            if (!Objects.equals(currentConfig.get(entry.getKey()), entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void dispose() {
        // can be overridden by subclasses
    }

    @Override
    public Thing getThing() {
        return this.thing;
    }

    @Override
    @Deprecated
    public void handleUpdate(ChannelUID channelUID, State newState) {
        // can be overridden by subclasses
    }

    @Override
    @Deprecated
    public void initialize() {
        // should be overridden by subclasses!
        updateStatus(ThingStatus.ONLINE);
        logger.warn(
                "BaseThingHandler.initialize() will be removed soon, ThingStatus can be set manually via updateStatus(ThingStatus.ONLINE)");
    }

    @Override
    public void thingUpdated(Thing thing) {
        dispose();
        this.thing = thing;
        initialize();
    }

    @Override
    public void setCallback(@Nullable ThingHandlerCallback thingHandlerCallback) {
        synchronized (this) {
            this.callback = thingHandlerCallback;
        }
    }

    /**
     * Get the {@link ThingHandlerCallback} instance.
     *
     * @return the {@link ThingHandlerCallback} instance. Only returns {@code null} while the handler is not
     *         initialized.
     */
    protected @Nullable ThingHandlerCallback getCallback() {
        return this.callback;
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        // can be overridden by subclasses
        // standard behavior is to refresh the linked channel,
        // so the newly linked items will receive a state update.
        handleCommand(channelUID, RefreshType.REFRESH);
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        // can be overridden by subclasses
    }

    /**
     * Validates the given configuration parameters against the configuration description.
     *
     * @param configurationParameters the configuration parameters to be validated
     * @throws ConfigValidationException if one or more of the given configuration parameters do not match
     *             their declarations in the configuration description
     */
    protected void validateConfigurationParameters(Map<String, Object> configurationParameters) {
        if (this.callback != null) {
            this.callback.validateConfigurationParameters(this.getThing(), configurationParameters);
        } else {
            logger.warn("Handler {} tried validating its configuration although the handler was already disposed.",
                    this.getClass().getSimpleName());
        }
    }

    /**
     * Returns the configuration of the thing.
     *
     * @return configuration of the thing
     */
    protected Configuration getConfig() {
        return getThing().getConfiguration();
    }

    /**
     * Returns the configuration of the thing and transforms it to the given
     * class.
     *
     * @param configurationClass configuration class
     * @return configuration of thing in form of the given class
     */
    protected <T> T getConfigAs(Class<T> configurationClass) {
        return getConfig().as(configurationClass);
    }

    /**
     *
     * Updates the state of the thing.
     *
     * @param channelUID unique id of the channel, which was updated
     * @param state new state
     */
    protected void updateState(ChannelUID channelUID, State state) {
        synchronized (this) {
            if (this.callback != null) {
                this.callback.stateUpdated(channelUID, state);
            } else {
                logger.warn(
                        "Handler {} of thing {} tried updating channel {} although the handler was already disposed.",
                        this.getClass().getSimpleName(), channelUID.getThingUID(), channelUID.getId());
            }
        }
    }

    /**
     *
     * Updates the state of the thing. Will use the thing UID to infer the
     * unique channel UID from the given ID.
     *
     * @param channel ID id of the channel, which was updated
     * @param state new state
     */
    protected void updateState(String channelID, State state) {
        ChannelUID channelUID = new ChannelUID(this.getThing().getUID(), channelID);
        updateState(channelUID, state);
    }

    /**
     * Emits an event for the given channel.
     *
     * @param channelUID UID of the channel over which the event will be emitted
     * @param event Event to emit
     */
    protected void triggerChannel(ChannelUID channelUID, String event) {
        synchronized (this) {
            if (this.callback != null) {
                this.callback.channelTriggered(this.getThing(), channelUID, event);
            } else {
                logger.warn(
                        "Handler {} of thing {} tried triggering channel {} although the handler was already disposed.",
                        this.getClass().getSimpleName(), channelUID.getThingUID(), channelUID.getId());
            }
        }
    }

    /**
     * Emits an event for the given channel. Will use the thing UID to infer the
     * unique channel UID from the given ID.
     *
     * @param channelID ID of the channel over which the event will be emitted
     * @param event Event to emit
     */
    protected void triggerChannel(String channelID, String event) {
        triggerChannel(new ChannelUID(this.getThing().getUID(), channelID), event);
    }

    /**
     * Emits an event for the given channel. Will use the thing UID to infer the
     * unique channel UID.
     *
     * @param channelUID UID of the channel over which the event will be emitted
     */
    protected void triggerChannel(String channelUID) {
        triggerChannel(new ChannelUID(this.getThing().getUID(), channelUID), "");
    }

    /**
     * Emits an event for the given channel. Will use the thing UID to infer the
     * unique channel UID.
     *
     * @param channelUID UID of the channel over which the event will be emitted
     */
    protected void triggerChannel(ChannelUID channelUID) {
        triggerChannel(channelUID, "");
    }

    /**
     * Sends a command for a channel of the thing.
     *
     * @param channelID id of the channel, which sends the command
     * @param command command
     */
    protected void postCommand(String channelID, Command command) {
        ChannelUID channelUID = new ChannelUID(this.getThing().getUID(), channelID);
        postCommand(channelUID, command);
    }

    /**
     * Sends a command for a channel of the thing.
     *
     * @param channelUID unique id of the channel, which sends the command
     * @param command command
     */
    protected void postCommand(ChannelUID channelUID, Command command) {
        synchronized (this) {
            if (this.callback != null) {
                this.callback.postCommand(channelUID, command);
            } else {
                logger.warn(
                        "Handler {} of thing {} tried posting a command to channel {} although the handler was already disposed.",
                        this.getClass().getSimpleName(), channelUID.getThingUID(), channelUID.getId());
            }
        }
    }

    /**
     * Updates the status of the thing.
     *
     * @param status the status
     * @param statusDetail the detail of the status
     * @param description the description of the status
     */
    protected void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        synchronized (this) {
            if (this.callback != null) {
                ThingStatusInfoBuilder statusBuilder = ThingStatusInfoBuilder.create(status, statusDetail);
                ThingStatusInfo statusInfo = statusBuilder.withDescription(description).build();
                this.callback.statusUpdated(this.thing, statusInfo);
            } else {
                logger.warn("Handler {} tried updating the thing status although the handler was already disposed.",
                        this.getClass().getSimpleName());
            }
        }
    }

    /**
     * Updates the status of the thing.
     *
     * @param status the status
     * @param statusDetail the detail of the status
     */
    protected void updateStatus(ThingStatus status, ThingStatusDetail statusDetail) {
        updateStatus(status, statusDetail, null);
    }

    /**
     * Updates the status of the thing. The detail of the status will be 'NONE'.
     *
     * @param status the status
     */
    protected void updateStatus(ThingStatus status) {
        updateStatus(status, ThingStatusDetail.NONE, null);
    }

    /**
     * Creates a thing builder, which allows to modify the thing. The method
     * {@link BaseThingHandler#updateThing(Thing)} must be called to persist the changes.
     *
     * @return {@link ThingBuilder} which builds an exact copy of the thing (not null)
     */
    protected ThingBuilder editThing() {
        return ThingBuilder.create(this.thing.getThingTypeUID(), this.thing.getUID())
                .withBridge(this.thing.getBridgeUID()).withChannels(this.thing.getChannels())
                .withConfiguration(this.thing.getConfiguration()).withLabel(this.thing.getLabel())
                .withLocation(this.thing.getLocation()).withProperties(this.thing.getProperties());
    }

    /**
     * Informs the framework, that a thing was updated. This method must be called after the configuration or channels
     * was changed.
     *
     * @param thing thing, that was updated and should be persisted
     */
    protected void updateThing(Thing thing) {
        if (thing == this.thing) {
            throw new IllegalArgumentException(
                    "Changes must not be done on the current thing - create a copy, e.g. via editThing()");
        }
        synchronized (this) {
            if (this.callback != null) {
                this.thing = thing;
                this.callback.thingUpdated(thing);
            } else {
                logger.warn("Handler {} tried updating thing {} although the handler was already disposed.",
                        this.getClass().getSimpleName(), thing.getUID());
            }
        }
    }

    /**
     * Returns a copy of the configuration, that can be modified. The method
     * {@link BaseThingHandler#updateConfiguration(Configuration)} must be called to persist the configuration.
     *
     * @return copy of the thing configuration (not null)
     */
    protected Configuration editConfiguration() {
        Map<String, Object> properties = this.thing.getConfiguration().getProperties();
        return new Configuration(new HashMap<>(properties));
    }

    /**
     * Updates the configuration of the thing and informs the framework about it.
     *
     * @param configuration configuration, that was updated and should be persisted
     */
    protected void updateConfiguration(Configuration configuration) {
        Map<String, Object> old = this.thing.getConfiguration().getProperties();
        try {
            this.thing.getConfiguration().setProperties(configuration.getProperties());
            synchronized (this) {
                if (this.callback != null) {
                    this.callback.thingUpdated(thing);
                } else {
                    logger.warn(
                            "Handler {} tried updating its configuration although the handler was already disposed.",
                            this.getClass().getSimpleName());
                }
            }
        } catch (RuntimeException e) {
            logger.warn(
                    "Error while applying configuration changes: '{}: {}' - reverting configuration changes on thing '{}'.",
                    e.getClass().getSimpleName(), e.getMessage(), this.thing.getUID().getAsString());
            this.thing.getConfiguration().setProperties(old);
            throw e;
        }
    }

    /**
     * Returns a copy of the properties map, that can be modified. The method {@link
     * BaseThingHandler#updateProperties(Map<String, String> properties)} must be called to persist the properties.
     *
     * @return copy of the thing properties (not null)
     */
    protected Map<String, String> editProperties() {
        Map<String, String> properties = this.thing.getProperties();
        return new HashMap<>(properties);
    }

    /**
     * Informs the framework, that the given properties map of the thing was updated. This method performs a check, if
     * the properties were updated. If the properties did not change, the framework is not informed about changes.
     *
     * @param properties properties map, that was updated and should be persisted
     */
    protected void updateProperties(Map<String, String> properties) {
        boolean propertiesUpdated = false;
        for (Entry<String, String> property : properties.entrySet()) {
            String propertyName = property.getKey();
            String propertyValue = property.getValue();
            String existingPropertyValue = thing.getProperties().get(propertyName);
            if (existingPropertyValue == null || !existingPropertyValue.equals(propertyValue)) {
                this.thing.setProperty(propertyName, propertyValue);
                propertiesUpdated = true;
            }
        }
        if (propertiesUpdated) {
            synchronized (this) {
                if (this.callback != null) {
                    this.callback.thingUpdated(thing);
                } else {
                    logger.warn(
                            "Handler {} tried updating its thing's properties although the handler was already disposed.",
                            this.getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * <p>
     * Updates the given property value for the thing that is handled by this thing handler instance. The value is only
     * set for the given property name if there has not been set any value yet or if the value has been changed. If the
     * value of the property to be set is null then the property is removed.
     *
     * This method also informs the framework about the updated thing, which in fact will persists the changes. So, if
     * multiple properties should be changed at the same time, the {@link BaseThingHandler#editProperties()} method
     * should be used.
     *
     * @param name the name of the property to be set
     * @param value the value of the property
     */
    protected void updateProperty(String name, String value) {
        String existingPropertyValue = thing.getProperties().get(name);
        if (existingPropertyValue == null || !existingPropertyValue.equals(value)) {
            thing.setProperty(name, value);
            synchronized (this) {
                if (this.callback != null) {
                    this.callback.thingUpdated(thing);
                } else {
                    logger.warn(
                            "Handler {} tried updating its thing's properties although the handler was already disposed.",
                            this.getClass().getSimpleName());
                }
            }
        }
    }

    /**
     * Returns the bridge of the thing.
     *
     * @return returns the bridge of the thing or null if the thing has no
     *         bridge
     */
    protected @Nullable Bridge getBridge() {
        ThingUID bridgeUID = thing.getBridgeUID();
        synchronized (this) {
            if (bridgeUID != null && thingRegistry != null) {
                return (Bridge) thingRegistry.get(bridgeUID);
            } else {
                return null;
            }
        }
    }

    /**
     * Returns whether at least one item is linked for the given channel ID.
     *
     * @param channelId channel ID (must not be null)
     * @return true if at least one item is linked, false otherwise
     */
    protected boolean isLinked(String channelId) {
        ChannelUID channelUID = new ChannelUID(this.getThing().getUID(), channelId);
        return isLinked(channelUID);
    }

    /**
     * Returns whether at least one item is linked for the given UID of the channel.
     *
     * @param channelUID UID of the channel (must not be null)
     * @return true if at least one item is linked, false otherwise
     */
    protected boolean isLinked(ChannelUID channelUID) {
        if (callback != null) {
            return callback.isChannelLinked(channelUID);
        } else {
            logger.warn(
                    "Handler {} of thing {} tried checking if channel {} is linked although the handler was already disposed.",
                    this.getClass().getSimpleName(), channelUID.getThingUID(), channelUID.getId());
            return false;
        }
    }

    /**
     * Returns whether the handler has already been initialized.
     *
     * @return true if handler is initialized, false otherwise
     */
    protected boolean isInitialized() {
        return ThingHandlerHelper.isHandlerInitialized(this);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE
                && getThing().getStatusInfo().getStatusDetail() == ThingStatusDetail.BRIDGE_OFFLINE) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.NONE);
        } else if (bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
    }

    protected void changeThingType(ThingTypeUID thingTypeUID, Configuration configuration) {
        if (this.callback != null) {
            this.callback.migrateThingType(getThing(), thingTypeUID, configuration);
        } else {
            logger.warn("Handler {} tried migrating the thing type although the handler was already disposed.",
                    this.getClass().getSimpleName());
        }
    }

}
