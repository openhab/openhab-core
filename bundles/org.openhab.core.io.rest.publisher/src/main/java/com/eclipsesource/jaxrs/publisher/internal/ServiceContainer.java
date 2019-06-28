/*******************************************************************************
 * Copyright (c) 2011,2012 Frank Appel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Frank Appel - initial API and implementation
 *    Holger Staudacher - ongoing development
 *    ProSyst Software GmbH. - compatibility with OSGi specification 4.2 APIs
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


public class ServiceContainer {

  private final Set<ServiceHolder> services;
  private final BundleContext bundleContext;

  ServiceContainer( BundleContext bundleContext ) {
    this.bundleContext = bundleContext;
    this.services = new HashSet<ServiceHolder>();
  }

  ServiceHolder add( ServiceReference reference ) {
    return add( bundleContext.getService( reference ), reference );
  }

  void remove( Object service ) {
    services.remove( find( service ) );
  }

  ServiceHolder[] getServices() {
    Set<ServiceHolder> result = new HashSet<ServiceHolder>();
    Iterator<ServiceHolder> iterator = services.iterator();
    while( iterator.hasNext() ) {
      result.add( iterator.next() );
    }
    return result.toArray( new ServiceHolder[ result.size() ]);
  }

  ServiceHolder find( Object service ) {
    Finder finder = new Finder();
    return finder.findServiceHolder( service, services );
  }

  void clear() {
    services.clear();
  }

  int size() {
    return services.size();
  }

  private ServiceHolder add( Object service, ServiceReference reference ) {
    ServiceHolder result = find( service );
    if( notFound( result ) ) {
      result = new ServiceHolder( service, reference );
      services.add( result );
    } else if( referenceIsMissing( reference, result ) ) {
      result.setServiceReference( reference );
    }
    return result;
  }

  private boolean notFound( ServiceHolder result ) {
    return result == null;
  }

  private boolean referenceIsMissing( ServiceReference reference, ServiceHolder result ) {
    return reference != null && result.getReference() == null;
  }

  static class ServiceHolder {
  
    private ServiceReference serviceReference;
    private final Object service;
  
     ServiceHolder( Object service, ServiceReference serviceReference ) {
      this.service = service;
      this.serviceReference = serviceReference;
    }
  
    Object getService() {
      return service;
    }
  
    ServiceReference getReference() {
      return serviceReference;
    }
  
    void setServiceReference( ServiceReference serviceReference ) {
      this.serviceReference = serviceReference;
    }
  }

  static class Finder {
  
    ServiceHolder findServiceHolder( Object service, Set<ServiceHolder> collection ) {
      Iterator<ServiceHolder> iterator = collection.iterator();
      ServiceHolder result = null;
      while( iterator.hasNext() ) {
        ServiceHolder serviceHolder = iterator.next();
        Object found = serviceHolder.getService();
        if( service.equals( found ) ) {
          result = serviceHolder;
        }
      }
      return result;
    }
  }
}
