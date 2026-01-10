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
package org.openhab.core.thing.binding;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.status.ConfigStatusProvider;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateHandler;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BaseThingHandlerFactory} provides a base implementation for the {@link ThingHandlerFactory} interface.
 * <p>
 * It is recommended to extend this abstract base class, because it covers a lot of common logic.
 * <p>
 *
 * @author Dennis Nobel - Initial contribution
 * @author Benedikt Niehues - fix for Bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=445137 considering
 *         default values
 * @author Thomas Höfer - added config status provider and firmware update handler service registration
 * @author Stefan Bußweiler - API changes due to bridge/thing life cycle refactoring, removed OSGi service registration
 *         for thing handlers
 * @author Connor Petty - added osgi service registration for thing handler services.
 */
@NonNullByDefault
public abstract class BaseThingHandlerFactory implements ThingHandlerFactory {

    private static final String THING_HANDLER_SERVICE_CANONICAL_NAME = ThingHandlerService.class.getCanonicalName();

    protected @NonNullByDefault({}) BundleContext bundleContext;

    private final Logger logger = LoggerFactory.getLogger(BaseThingHandlerFactory.class);

    private final Map<String, ServiceRegistration<ConfigStatusProvider>> configStatusProviders = new ConcurrentHashMap<>();
    private final Map<String, ServiceRegistration<FirmwareUpdateHandler>> firmwareUpdateHandlers = new ConcurrentHashMap<>();

    private final Map<ThingUID, Set<RegisteredThingHandlerService<?>>> thingHandlerServices = new ConcurrentHashMap<>();

    private @NonNullByDefault({}) ServiceTracker<ThingTypeRegistry, ThingTypeRegistry> thingTypeRegistryServiceTracker;
    private @NonNullByDefault({}) ServiceTracker<ConfigDescriptionRegistry, ConfigDescriptionRegistry> configDescriptionRegistryServiceTracker;

    /**
     * Initializes the {@link BaseThingHandlerFactory}. If this method is overridden by a sub class, the implementing
     * method must call <code>super.activate(componentContext)</code> first.
     *
     * @param componentContext component context (must not be null)
     */
    protected void activate(ComponentContext componentContext) {
        bundleContext = componentContext.getBundleContext();
        thingTypeRegistryServiceTracker = new ServiceTracker<>(bundleContext, ThingTypeRegistry.class.getName(), null);
        thingTypeRegistryServiceTracker.open();
        configDescriptionRegistryServiceTracker = new ServiceTracker<>(bundleContext,
                ConfigDescriptionRegistry.class.getName(), null);
        configDescriptionRegistryServiceTracker.open();
    }

    /**
     * Disposes the {@link BaseThingHandlerFactory}. If this method is overridden by a sub class, the implementing
     * method must call <code>super.deactivate(componentContext)</code> first.
     *
     * @param componentContext component context (must not be null)
     */
    protected void deactivate(ComponentContext componentContext) {
        for (ServiceRegistration<ConfigStatusProvider> serviceRegistration : configStatusProviders.values()) {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
        for (ServiceRegistration<FirmwareUpdateHandler> serviceRegistration : firmwareUpdateHandlers.values()) {
            if (serviceRegistration != null) {
                serviceRegistration.unregister();
            }
        }
        thingTypeRegistryServiceTracker.close();
        configDescriptionRegistryServiceTracker.close();
        configStatusProviders.clear();
        firmwareUpdateHandlers.clear();
        thingHandlerServices.clear();
        bundleContext = null;
    }

    /**
     * Get the bundle context.
     *
     * @return the bundle context
     * @throws IllegalArgumentException if the bundle thing handler is not active
     */
    protected BundleContext getBundleContext() {
        final BundleContext bundleContext = this.bundleContext;
        if (bundleContext != null) {
            return bundleContext;
        } else {
            throw new IllegalStateException(
                    "The bundle context is missing (it seems your thing handler factory is used but not active).");
        }
    }

    @Override
    public ThingHandler registerHandler(Thing thing) {
        ThingHandler thingHandler = createHandler(thing);
        if (thingHandler == null) {
            throw new IllegalStateException(this.getClass().getSimpleName()
                    + " could not create a handler for the thing '" + thing.getUID() + "'.");
        }
        if ((thing instanceof Bridge) && !(thingHandler instanceof BridgeHandler)) {
            throw new IllegalStateException(
                    "Created handler of bridge '" + thing.getUID() + "' must implement the BridgeHandler interface.");
        }
        registerConfigStatusProvider(thing, thingHandler);
        registerFirmwareUpdateHandler(thing, thingHandler);
        registerServices(thing, thingHandler);
        return thingHandler;
    }

    private void registerServices(Thing thing, ThingHandler thingHandler) {
        ThingUID thingUID = thing.getUID();
        for (Class<? extends ThingHandlerService> c : thingHandler.getServices()) {
            if (!ThingHandlerService.class.isAssignableFrom(c)) {
                logger.warn(
                        "Should register service={} for thingUID={}, but it does not implement the interface ThingHandlerService.",
                        c.getCanonicalName(), thingUID);
                continue;
            }
            registerThingHandlerService(thingUID, thingHandler, c);
        }
    }

    /**
     * Registers a dynamic service for the given thing handler. The service must implement ThingHandlerService.
     * 
     * @param thingHandler The thing handler requesting a service to be registered.
     * @param klass The service class to register.
     */
    public void registerService(ThingHandler thingHandler, Class<? extends ThingHandlerService> klass) {
        ThingUID thingUID = thingHandler.getThing().getUID();
        if (!ThingHandlerService.class.isAssignableFrom(klass)) {
            logger.warn(
                    "Should register service={} for thingUID={}, but it does not implement the interface ThingHandlerService.",
                    klass.getCanonicalName(), thingUID);
            return;
        }
        registerThingHandlerService(thingUID, thingHandler, klass);
    }

    private <T extends ThingHandlerService> void registerThingHandlerService(ThingUID thingUID,
            ThingHandler thingHandler, Class<T> c) {
        RegisteredThingHandlerService<T> registeredService;

        Component component = c.getAnnotation(Component.class);
        if (component != null && component.enabled()) {
            if (component.scope() != ServiceScope.PROTOTYPE) {
                // then we cannot use it.
                logger.warn("Could not register service for class={}. Service must have a prototype scope",
                        c.getCanonicalName());
                return;
            }
            if (component.service().length != 1 || component.service()[0] != c) {
                logger.warn(
                        "Could not register service for class={}. ThingHandlerService with @Component must only label itself as a service.",
                        c.getCanonicalName());
                return;
            }
        }

        ServiceReference<T> serviceRef = bundleContext.getServiceReference(c);
        if (serviceRef != null) {
            ServiceObjects<T> serviceObjs = bundleContext.getServiceObjects(serviceRef);
            registeredService = new RegisteredThingHandlerService<>(serviceObjs);
        } else {
            try {
                T serviceInstance = c.getConstructor().newInstance();
                registeredService = new RegisteredThingHandlerService<>(serviceInstance);
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                    | InvocationTargetException e) {
                logger.warn("Could not register service for class={}", c.getCanonicalName(), e);
                return;
            }
        }

        String[] serviceNames = getAllInterfaces(c).stream()//
                .map(Class::getCanonicalName)
                // we only add specific ThingHandlerServices, i.e. those that derive from the
                // ThingHandlerService
                // interface, NOT the ThingHandlerService itself. We do this to register them as specific OSGi
                // services later, rather than as a generic ThingHandlerService.
                .filter(className -> className != null && !className.equals(THING_HANDLER_SERVICE_CANONICAL_NAME))
                .toArray(String[]::new);

        registeredService.initializeService(thingHandler, serviceNames);

        Objects.requireNonNull(thingHandlerServices.computeIfAbsent(thingUID, uid -> new HashSet<>()))
                .add(registeredService);
    }

    private void unregisterServices(Thing thing) {
        ThingUID thingUID = thing.getUID();
        Set<RegisteredThingHandlerService<?>> serviceRegs = thingHandlerServices.remove(thingUID);
        if (serviceRegs != null) {
            serviceRegs.forEach(RegisteredThingHandlerService::disposeService);
        }
    }

    /**
     * Returns all interfaces of the given class as well as all super classes.
     *
     * @param clazz The class
     * @return A {@link List} of interfaces
     */
    private static Set<Class<?>> getAllInterfaces(Class<?> clazz) {
        Set<Class<?>> interfaces = new HashSet<>();
        for (Class<?> superclazz = clazz; superclazz != null; superclazz = superclazz.getSuperclass()) {
            interfaces.addAll(Arrays.asList(superclazz.getInterfaces()));
        }
        return interfaces;
    }

    /**
     * Creates a {@link ThingHandler} for the given thing.
     *
     * @param thing the thing
     * @return thing the created handler
     */
    protected abstract @Nullable ThingHandler createHandler(Thing thing);

    private void registerConfigStatusProvider(Thing thing, ThingHandler thingHandler) {
        if (thingHandler instanceof ConfigStatusProvider) {
            ServiceRegistration<ConfigStatusProvider> serviceRegistration = registerAsService(thingHandler,
                    ConfigStatusProvider.class);
            configStatusProviders.put(thing.getUID().getAsString(), serviceRegistration);
        }
    }

    private void registerFirmwareUpdateHandler(Thing thing, ThingHandler thingHandler) {
        if (thingHandler instanceof FirmwareUpdateHandler) {
            ServiceRegistration<FirmwareUpdateHandler> serviceRegistration = registerAsService(thingHandler,
                    FirmwareUpdateHandler.class);
            firmwareUpdateHandlers.put(thing.getUID().getAsString(), serviceRegistration);
        }
    }

    private <T> ServiceRegistration<T> registerAsService(ThingHandler thingHandler, Class<T> type) {
        @SuppressWarnings("unchecked")
        ServiceRegistration<T> serviceRegistration = (ServiceRegistration<T>) bundleContext
                .registerService(type.getName(), thingHandler, null);
        return serviceRegistration;
    }

    @Override
    public void unregisterHandler(Thing thing) {
        ThingHandler thingHandler = thing.getHandler();
        if (thingHandler != null) {
            removeHandler(thingHandler);
        }
        unregisterConfigStatusProvider(thing);
        unregisterFirmwareUpdateHandler(thing);
        unregisterServices(thing);
    }

    /**
     * This method is called when a thing handler should be removed. The
     * implementing caller can override this method to release specific
     * resources.
     *
     * @param thingHandler thing handler to be removed
     */
    protected void removeHandler(ThingHandler thingHandler) {
        // can be overridden
    }

    private void unregisterConfigStatusProvider(Thing thing) {
        ServiceRegistration<ConfigStatusProvider> serviceRegistration = configStatusProviders
                .remove(thing.getUID().getAsString());
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    private void unregisterFirmwareUpdateHandler(Thing thing) {
        ServiceRegistration<FirmwareUpdateHandler> serviceRegistration = firmwareUpdateHandlers
                .remove(thing.getUID().getAsString());
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    @Override
    public void removeThing(ThingUID thingUID) {
        // can be overridden
    }

    /**
     * Returns the {@link ThingType} which is represented by the given {@link ThingTypeUID}.
     *
     * @param thingTypeUID the unique id of the thing type
     * @return the thing type represented by the given unique id
     */
    protected @Nullable ThingType getThingTypeByUID(ThingTypeUID thingTypeUID) {
        if (thingTypeRegistryServiceTracker == null) {
            throw new IllegalStateException(
                    "Base thing handler factory has not been properly initialized. Did you forget to call super.activate()?");
        }
        ThingTypeRegistry thingTypeRegistry = thingTypeRegistryServiceTracker.getService();
        if (thingTypeRegistry != null) {
            return thingTypeRegistry.getThingType(thingTypeUID);
        }
        return null;
    }

    /**
     * Creates a thing based on given thing type uid.
     *
     * @param thingTypeUID thing type uid (can not be null)
     * @param configuration (can not be null)
     * @param thingUID thingUID (can not be null)
     * @return thing (can be null, if thing type is unknown)
     */
    protected @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID) {
        return createThing(thingTypeUID, configuration, thingUID, null);
    }

    /**
     * Creates a thing based on given thing type uid.
     *
     * @param thingTypeUID thing type uid (must not be null)
     * @param thingUID thingUID (can be null)
     * @param configuration (must not be null)
     * @param bridgeUID (can be null)
     * @return thing (can be null, if thing type is unknown)
     */
    @Override
    public @Nullable Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration,
            @Nullable ThingUID thingUID, @Nullable ThingUID bridgeUID) {
        ThingUID effectiveUID = thingUID != null ? thingUID : ThingFactory.generateRandomThingUID(thingTypeUID);
        ThingType thingType = getThingTypeByUID(thingTypeUID);
        if (thingType != null) {
            return ThingFactory.createThing(thingType, effectiveUID, configuration, bridgeUID,
                    getConfigDescriptionRegistry());
        } else {
            return null;
        }
    }

    protected @Nullable ConfigDescriptionRegistry getConfigDescriptionRegistry() {
        if (configDescriptionRegistryServiceTracker == null) {
            throw new IllegalStateException(
                    "Config Description Registry has not been properly initialized. Did you forget to call super.activate()?");
        }
        return configDescriptionRegistryServiceTracker.getService();
    }

    private class RegisteredThingHandlerService<T extends ThingHandlerService> {

        private final T serviceInstance;

        private @Nullable ServiceObjects<T> serviceObjects;

        private @Nullable ServiceRegistration<?> serviceRegistration;

        public RegisteredThingHandlerService(T serviceInstance) {
            this.serviceInstance = serviceInstance;
        }

        public RegisteredThingHandlerService(ServiceObjects<T> serviceObjs) {
            this.serviceInstance = serviceObjs.getService();
            this.serviceObjects = serviceObjs;
        }

        public void initializeService(ThingHandler handler, String[] serviceNames) {
            serviceInstance.setThingHandler(handler);
            if (serviceNames.length > 0) {
                ServiceRegistration<?> serviceReg = bundleContext.registerService(serviceNames, serviceInstance, null);
                if (serviceReg != null) {
                    serviceRegistration = serviceReg;
                }
            }
            serviceInstance.initialize();
        }

        public void disposeService() {
            serviceInstance.dispose();

            ServiceRegistration<?> serviceReg = this.serviceRegistration;
            if (serviceReg != null) {
                serviceReg.unregister();
            }

            ServiceObjects<T> serviceObjs = this.serviceObjects;
            if (serviceObjs != null) {
                serviceObjs.ungetService(serviceInstance);
            }
        }
    }
}
