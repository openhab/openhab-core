/*******************************************************************************
 * Copyright (c) 2012,2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation
 *    ProSyst Software GmbH. - compatibility with OSGi specification 4.2 APIs
 *    Ivan Iliev - added ServletConfigurationTracker
 ******************************************************************************/
package org.openhab.core.thirdparty.com.eclipsesource.jaxrs.publisher.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.ws.rs.Path;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.eclipse.smarthome.io.rest.RESTResource;
import org.eclipse.smarthome.io.rest.sse.SseResource;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.openhab.core.io.rest.publisher.internal.ResourceFilterImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(immediate = true)
public class Activator {

    private ServiceRegistration<JAXRSConnector> connectorRegistration;
    private JAXRSConnector jaxRsConnector;
    private HttpTracker httpTracker;
    private ServletConfigurationTracker servletConfigurationTracker;
    private ApplicationConfigurationTracker applicationConfigurationTracker;
    private ServiceRegistration<ManagedService> configRegistration;

    private ResourceFilterImpl ohResourceFilter;
    private ResourceTracker ohTracker;

    @Activate
    public Activator(final BundleContext context) {
        ohResourceFilter = new ResourceFilterImpl();
        jaxRsConnector = new JAXRSConnector(context);
    }

    @Activate
    public void start(BundleContext context) throws Exception {
        System.setProperty("javax.ws.rs.ext.RuntimeDelegate",
                "org.glassfish.jersey.server.internal.RuntimeDelegateImpl");
        startJerseyServer();
        registerConfiguration(context);
        connectorRegistration = context.registerService(JAXRSConnector.class, jaxRsConnector, null);
        openHttpServiceTracker(context);
        openServletConfigurationTracker(context);
        openApplicationConfigurationTracker(context);
        openOhServiceTracker(context);
    }

    @Deactivate
    public void stop(BundleContext context) throws Exception {
        httpTracker.close();
        servletConfigurationTracker.close();
        applicationConfigurationTracker.close();
        ohTracker.close();
        connectorRegistration.unregister();
        configRegistration.unregister();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addJaxRsMessageBodyReader(final ServiceReference<MessageBodyReader<?>> resource) {
        jaxRsConnector.addResource(resource);
    }

    public void removeJaxRsMessageBodyReader(final ServiceReference<MessageBodyReader<?>> resource) {
        jaxRsConnector.removeResource(resource);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addJaxRsMessageBodyWriter(final ServiceReference<MessageBodyWriter<?>> resource) {
        jaxRsConnector.addResource(resource);
    }

    public void removeJaxRsMessageBodyWriter(final ServiceReference<MessageBodyWriter<?>> resource) {
        jaxRsConnector.removeResource(resource);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addOhRESTResource(final ServiceReference<RESTResource> resource) {
        jaxRsConnector.addResource(resource);
    }

    public void removeOhRESTResource(final ServiceReference<RESTResource> resource) {
        jaxRsConnector.removeResource(resource);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addOhSSEResource(final ServiceReference<SseResource> resource) {
        jaxRsConnector.addResource(resource);
    }

    public void removeOhSSEResource(final ServiceReference<SseResource> resource) {
        jaxRsConnector.removeResource(resource);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addJerseySseFeature(final ServiceReference<SseFeature> resource) {
        jaxRsConnector.addResource(resource);
    }

    public void removeJerseySseFeature(final ServiceReference<SseFeature> resource) {
        jaxRsConnector.removeResource(resource);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addJerseyApplicationEventListener(final ServiceReference<ApplicationEventListener> resource) {
        jaxRsConnector.addResource(resource);
    }

    public void removeJerseyApplicationEventListener(final ServiceReference<ApplicationEventListener> resource) {
        jaxRsConnector.removeResource(resource);
    }

    private void registerConfiguration(BundleContext context) {
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, Configuration.CONFIG_SERVICE_PID);
        configRegistration = context.registerService(ManagedService.class, new Configuration(jaxRsConnector),
                properties);
    }

    private void startJerseyServer() throws BundleException {
        Bundle bundle = getJerseyAPIBundle();
        if (bundle.getState() != Bundle.ACTIVE) {
            bundle.start();
        }
    }

    private void openHttpServiceTracker(BundleContext context) {
        httpTracker = new HttpTracker(context, jaxRsConnector);
        httpTracker.open();
    }

    private void openServletConfigurationTracker(BundleContext context) {
        servletConfigurationTracker = new ServletConfigurationTracker(context, jaxRsConnector);
        servletConfigurationTracker.open();
    }

    private void openApplicationConfigurationTracker(BundleContext context) {
        applicationConfigurationTracker = new ApplicationConfigurationTracker(context, jaxRsConnector);
        applicationConfigurationTracker.open();
    }

    private void openOhServiceTracker(BundleContext context) throws InvalidSyntaxException {
        ohTracker = new ResourceTracker(context, ohResourceFilter.getFilter(), jaxRsConnector);
        ohTracker.open();
    }

    // For testing purpose
    Bundle getJerseyAPIBundle() {
        return FrameworkUtil.getBundle(Path.class);
    }
}
