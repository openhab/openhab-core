/*******************************************************************************
 * Copyright (c) 2012,2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation, ongoing development
 *    Dirk Lecluse - added tracking of Provider classes
 *    Frank Appel - specified Filter to exclude resources from publishing
 *    Ivan Iliev - Performance Optimizations
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import javax.ws.rs.Path;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.Provider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;


public class ResourceTracker extends ServiceTracker {

  private final BundleContext context;
  private final JAXRSConnector connector;

  public ResourceTracker( BundleContext context, Filter filter, JAXRSConnector connector ) {
    super( context, filter, null );
    this.context = context;
    this.connector = connector;
  }

  @Override
  public Object addingService( ServiceReference reference ) {
    Object service = context.getService( reference );
    return delegateAddService( reference, service );
  }

  private Object delegateAddService( ServiceReference reference, Object service ) {
    Object result;
    if( isResource( service ) ) {
      result = connector.addResource( reference );
    } else {
      context.ungetService( reference );
      result = null;
    }
    return result;
  }

  @Override
  public void removedService( ServiceReference reference, Object service ) {
    connector.removeResource( service );
    context.ungetService( reference );
  }

  @Override
  public void modifiedService( ServiceReference reference, Object service ) {
    connector.removeResource( service );
    delegateAddService( reference, service );
  }

  private boolean isResource( Object service ) {
    return service != null && ( hasRegisterableAnnotation( service ) || service instanceof Feature );
  }

  private boolean hasRegisterableAnnotation( Object service ) {
    boolean result = isRegisterableAnnotationPresent( service.getClass() );
    if( !result ) {
      Class<?>[] interfaces = service.getClass().getInterfaces();
      for( Class<?> type : interfaces ) {
        result = result || isRegisterableAnnotationPresent( type );
      }
    }
    return result;
  }

  private boolean isRegisterableAnnotationPresent( Class<?> type ) {
    return type.isAnnotationPresent( Path.class ) || type.isAnnotationPresent( Provider.class );
  }
}
