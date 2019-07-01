/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.core.io.rest.publisher.internal;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.io.rest.OhJAXRSResource;
import org.eclipse.smarthome.io.rest.RESTResource;
import org.eclipse.smarthome.io.rest.sse.SseResource;
import org.glassfish.jersey.media.sse.SseFeature;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.openhab.core.thirdparty.com.eclipsesource.jaxrs.publisher.internal.JAXRSConnector;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * A registrator for JAX RS resources.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class ResourceRegistrator {

    private JAXRSConnector jaxRsConnector;

    @Activate
    public ResourceRegistrator(final @Reference JAXRSConnectorProvider provider) {
        jaxRsConnector = provider.getJAXRSConnector();
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
    public void addOhJaxRsResource(final ServiceReference<OhJAXRSResource> resource) {
        jaxRsConnector.addResource(resource);
    }

    public void removeOhJaxRsResource(final ServiceReference<OhJAXRSResource> resource) {
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

}
