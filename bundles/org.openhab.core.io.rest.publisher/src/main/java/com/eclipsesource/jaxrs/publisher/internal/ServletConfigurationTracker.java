/*******************************************************************************
 * Copyright (c) 2015 Ivan Iliev and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Ivan Iliev - initial API and implementation
 *    Holger Staudacher  - ongoing development
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.eclipsesource.jaxrs.publisher.ServletConfiguration;

/**
 * <p>
 * Tracker for OSGi Services implementing the {@link ServletConfiguration} interface.
 * </p> 
 */
public class ServletConfigurationTracker extends ServiceTracker {

  private final JAXRSConnector connector;

  ServletConfigurationTracker( BundleContext context, JAXRSConnector connector ) {
    super( context, ServletConfiguration.class.getName(), null );
    this.connector = connector;
  }

  @Override
  public Object addingService( ServiceReference reference ) {
    return connector.setServletConfiguration( reference );
  }

  @Override
  public void removedService( ServiceReference reference, Object service ) {
    if( service instanceof ServletConfiguration ) {
      connector.unsetServletConfiguration( reference, ( ServletConfiguration )service );
    }
  }
}
