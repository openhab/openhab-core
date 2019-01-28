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
package org.eclipse.smarthome.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.test.internal.java.MissingServiceAnalyzer;
import org.eclipse.smarthome.test.storage.VolatileStorageService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import groovy.lang.Closure;

/**
 * {@link OSGiTest} is an abstract base class for OSGi based tests. It provides
 * convenience methods to register and unregister mocks as OSGi services. All services, which
 * are registered through the {@link OSGiTest#registerService} methods, are unregistered
 * automatically in the tear down of the test.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Tanya Georgieva - Refactor the groovy file to java
 */
@Deprecated
public abstract class OSGiTest {

    private final Map<String, List<ServiceRegistration<?>>> registeredServices = new HashMap<>();
    protected BundleContext bundleContext;

    protected static final int TIMEOUT = 10000;
    protected static final int SLEEPTIME = 50;

    @Before
    public void bindBundleContext() {
        bundleContext = getBundleContext();
        assertThat(bundleContext, is(notNullValue()));
    }

    /**
     * Returns the {@link BundleContext}, which is used for registration and unregistration of OSGi
     * services. By default it uses the bundle context of the test class itself. This method can be overridden
     * by concrete implementations to provide another bundle context.
     *
     * @return bundle context
     */
    protected BundleContext getBundleContext() {
        final Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (bundle != null) {
            return bundle.getBundleContext();
        } else {
            return null;
        }
    }

    private <T> T unrefService(final ServiceReference<T> serviceReference) {
        if (serviceReference == null) {
            return null;
        } else {
            return bundleContext.getService(serviceReference);
        }
    }

    /**
     * Get an OSGi service for the given class.
     *
     * @param clazz class under which the OSGi service is registered
     * @return OSGi service or null if no service can be found for the given class
     */
    protected <T> T getService(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        final ServiceReference<T> serviceReference = (ServiceReference<T>) bundleContext
                .getServiceReference(clazz.getName());

        if (serviceReference == null) {
            new MissingServiceAnalyzer(System.out, bundleContext).printMissingServiceDetails(clazz);
            return null;
        }

        return unrefService(serviceReference);
    }

    /**
     * Get an OSGi service for the given class and the given filter.
     *
     * @param clazz class under which the OSGi service is registered
     * @param filter
     * @return OSGi service or null if no service can be found for the given class
     */
    protected <T> T getService(Class<T> clazz, Predicate<ServiceReference<T>> filter) {
        final ServiceReference<T> serviceReferences[] = getServices(clazz);

        if (serviceReferences == null) {
            return null;
        }
        final List<T> filteredServiceReferences = new ArrayList<>(serviceReferences.length);
        for (final ServiceReference<T> serviceReference : serviceReferences) {
            if (filter.test(serviceReference)) {
                filteredServiceReferences.add(unrefService(serviceReference));
            }
        }

        if (filteredServiceReferences.size() > 1) {
            Assert.fail("More than 1 service matching the filter is registered.");
        }
        if (filteredServiceReferences.isEmpty()) {
            return null;
        } else {
            return filteredServiceReferences.get(0);
        }
    }

    private <T> ServiceReference<T>[] getServices(final Class<T> clazz) {
        try {
            @SuppressWarnings("unchecked")
            ServiceReference<T> serviceReferences[] = (ServiceReference<T>[]) bundleContext
                    .getServiceReferences(clazz.getName(), null);
            return serviceReferences;
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid exception for a null filter");
        }
    }

    /**
     * Get an OSGi service for the given class and the given filter.
     *
     * @param clazz class under which the OSGi service is registered
     * @param implementationClass the implementation class
     * @return OSGi service or null if no service can be found for the given class
     */
    protected <T, I extends T> I getService(Class<T> clazz, Class<I> implementationClass) {
        @SuppressWarnings("unchecked")
        final I service = (I) getService(clazz, srvRef -> implementationClass.isInstance(unrefService(srvRef)));
        return service;
    }

    /**
     * Register the given object as OSGi service.
     *
     * <p>
     * The first interface is used as OSGi service interface name.
     *
     * @param service service to be registered
     * @return service registration object
     */
    protected ServiceRegistration<?> registerService(final Object service) {
        return registerService(service, getInterfaceName(service), null);
    }

    /**
     * Register the given object as OSGi service. The first interface is used as OSGi service interface name.
     *
     * @param service service to be registered
     * @param properties OSGi service properties
     * @return service registration object
     */
    protected ServiceRegistration<?> registerService(final Object service, final Dictionary<String, ?> properties) {
        return registerService(service, getInterfaceName(service), properties);
    }

    /**
     * Register the given object as OSGi service.
     *
     * <p>
     * The given interface name is used as OSGi service interface name.
     *
     * @param service service to be registered
     * @param interfaceName interface name of the OSGi service
     * @return service registration object
     */
    protected ServiceRegistration<?> registerService(final Object service, final String interfaceName) {
        return registerService(service, interfaceName, null);
    }

    /**
     * Register the given object as OSGi service.
     *
     * <p>
     * The given interface name is used as OSGi service interface name.
     *
     * @param service service to be registered
     * @param interfaceName interface name of the OSGi service
     * @param properties OSGi service properties
     * @return service registration object
     */
    protected ServiceRegistration<?> registerService(final Object service, final String interfaceName,
            final Dictionary<String, ?> properties) {
        assertThat(interfaceName, is(notNullValue()));
        final ServiceRegistration<?> srvReg = bundleContext.registerService(interfaceName, service, properties);
        saveServiceRegistration(interfaceName, srvReg);
        return srvReg;
    }

    private void saveServiceRegistration(final String interfaceName, final ServiceRegistration<?> srvReg) {
        List<ServiceRegistration<?>> regs = registeredServices.get(interfaceName);
        if (regs == null) {
            regs = new ArrayList<>();
            registeredServices.put(interfaceName, regs);
        }
        regs.add(srvReg);
    }

    /**
     * Register the given object as OSGi service.
     *
     * <p>
     * The given interface names are used as OSGi service interface name.
     *
     * @param service service to be registered
     * @param interfaceName interface name of the OSGi service
     * @param properties OSGi service properties
     * @return service registration object
     */
    protected ServiceRegistration<?> registerService(final Object service, final String[] interfaceNames,
            final Dictionary<String, ?> properties) {
        assertThat(interfaceNames, is(notNullValue()));

        final ServiceRegistration<?> srvReg = bundleContext.registerService(interfaceNames, service, properties);

        for (final String interfaceName : interfaceNames) {
            saveServiceRegistration(interfaceName, srvReg);
        }

        return srvReg;
    }

    /**
     * Unregister an OSGi service by the given object, that was registered before.
     *
     * <p>
     * The interface name is taken from the first interface of the service object.
     *
     * @param service the service
     * @return the service registration that was unregistered or null if no service could be found
     */
    protected ServiceRegistration<?> unregisterService(final Object service) {
        return unregisterService(getInterfaceName(service));
    }

    /**
     * Unregister an OSGi service by the given object, that was registered before.
     *
     * @param interfaceName the interface name of the service
     * @return the first service registration that was unregistered or null if no service could be found
     */
    protected ServiceRegistration<?> unregisterService(final String interfaceName) {
        ServiceRegistration<?> reg = null;
        List<ServiceRegistration<?>> regList = registeredServices.remove(interfaceName);
        if (regList != null) {
            reg = regList.get(0);
            regList.forEach(r -> r.unregister());
        }
        return reg;
    }

    /**
     * Returns the interface name for a given service object by choosing the first interface.
     *
     * @param service service object
     * @return name of the first interface or null if the object has no interfaces
     */
    protected String getInterfaceName(final Object service) {
        Class<?>[] classes = service.getClass().getInterfaces();
        if (classes.length >= 1) {
            return classes[0].getName();
        } else {
            return null;
        }
    }

    /**
     * Registers a volatile storage service.
     */
    protected void registerVolatileStorageService() {
        registerService(new VolatileStorageService());
    }

    @After
    public void unregisterMocks() {
        registeredServices.forEach((interfaceName, services) -> services.forEach(service -> service.unregister()));
        registeredServices.clear();
    }

    /**
     * When this method is called it waits until the condition is fulfilled or the timeout is reached.
     * The condition is specified by a closure, that must return a boolean object. When the condition is
     * not fulfilled Thread.sleep is called at the current Thread for a specified time. After this time
     * the condition is checked again. By a default the specified sleep time is 50 ms. The default timeout
     * is 10000 ms.
     *
     * @param condition closure that must not have an argument and must return a boolean value
     * @param timeout timeout, default is 10000ms
     * @param sleepTime interval for checking the condition, default is 50ms
     * @throws Exception
     */
    protected void waitFor(Closure<?> condition, int timeout, int sleepTime) throws Exception {
        int waitingTime = 0;
        while (condition != null && !Boolean.TRUE.equals(condition.call()) && waitingTime < timeout) {
            waitingTime += sleepTime;
            Thread.sleep(sleepTime);
        }
    }

    protected void waitFor(Closure<?> condition, int timeout) throws Exception {
        waitFor(condition, timeout, SLEEPTIME);
    }

    protected void waitFor(Closure<?> condition) throws Exception {
        waitFor(condition, TIMEOUT);
    }

    /**
     * When this method is called it waits until the assertion is fulfilled or the timeout is reached.
     * The assertion is specified by a closure, that must throw an Exception, if the assertion is not fulfilled.
     * When the assertion is not fulfilled Thread.sleep is called at the current Thread for a specified time.
     * After this time the condition is checked again. By a default the specified sleep time is 50 ms.
     * The default timeout is 10000 ms.
     *
     * @param assertion closure that must not have an argument
     * @param timeout timeout, default is 10000ms
     * @param sleepTime interval for checking the condition, default is 50ms
     * @throws Exception
     */
    protected void waitForAssert(Closure<?> assertion, int timeout, int sleepTime) throws Exception {
        waitForAssert(assertion, null, timeout, sleepTime);
    }

    /**
     * When this method is called it waits until the assertion is fulfilled or the timeout is reached.
     * The assertion is specified by a closure, that must throw an Exception, if the assertion is not fulfilled.
     * When the assertion is not fulfilled Thread.sleep is called at the current Thread for a specified time.
     * After this time the condition is checked again. By a default the specified sleep time is 50 ms.
     * The default timeout is 10000 ms.
     *
     * @param assertion closure that must not have an argument
     * @param beforeLastCall close that must not have an arugment and should be executed in front of the last call to
     *            ${code assertion}.
     * @param timeout timeout, default is 10000ms
     * @param sleepTime interval for checking the condition, default is 50ms
     * @throws Exception
     */
    protected void waitForAssert(Closure<?> assertion, Closure<?> beforeLastCall, long timeout, int sleepTime)
            throws Exception {
        final long timeoutNs = TimeUnit.MILLISECONDS.toNanos(timeout);
        final long startingTime = System.nanoTime();

        while ((System.nanoTime() - startingTime) < timeoutNs) {
            try {
                assertion.call();
                return;
            } catch (Error | NullPointerException error) {
                Thread.sleep(sleepTime);
            }
        }
        if (beforeLastCall != null) {
            beforeLastCall.call();
        }
        assertion.call();
    }

    protected void waitForAssert(Closure<?> assertion, Closure<?> beforeLastCall) throws Exception {
        waitForAssert(assertion, beforeLastCall, TIMEOUT, SLEEPTIME);
    }

    protected void waitForAssert(Closure<?> assertion, Closure<?> beforeLastCall, long timeout) throws Exception {
        waitForAssert(assertion, beforeLastCall, timeout, SLEEPTIME);
    }

    protected void waitForAssert(Closure<?> assertion, int timeout) throws Exception {
        waitForAssert(assertion, timeout, SLEEPTIME);
    }

    protected void waitForAssert(Closure<?> assertion) throws Exception {
        waitForAssert(assertion, TIMEOUT);
    }

    protected void setDefaultLocale(Locale locale) throws Exception {
        assertThat(locale, is(notNullValue()));

        ConfigurationAdmin configAdmin = (ConfigurationAdmin) getService(
                Class.forName("org.osgi.service.cm.ConfigurationAdmin"));
        assertThat(configAdmin, is(notNullValue()));

        LocaleProvider localeProvider = (LocaleProvider) getService(
                Class.forName("org.eclipse.smarthome.core.i18n.LocaleProvider"));
        assertThat(localeProvider, is(notNullValue()));

        Configuration config = configAdmin.getConfiguration("org.eclipse.smarthome.core.i18nprovider", null);
        assertThat(config, is(notNullValue()));

        Dictionary<String, Object> properties = config.getProperties();
        if (properties == null) {
            properties = new Hashtable<>();
        }

        properties.put("language", locale.getLanguage());
        properties.put("script", locale.getScript());
        properties.put("region", locale.getCountry());
        properties.put("variant", locale.getVariant());

        config.update(properties);

        waitForAssert(new Closure<Object>(null) {
            private static final long serialVersionUID = -5083904877474902686L;

            public Object doCall() {
                assertThat(localeProvider.getLocale(), is(locale));
                return null;
            }
        });
    }

}
