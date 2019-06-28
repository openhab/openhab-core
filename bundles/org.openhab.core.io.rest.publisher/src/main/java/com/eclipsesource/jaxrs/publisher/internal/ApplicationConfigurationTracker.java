/*******************************************************************************
 * Copyright (c) 2015 Holger Staudacher and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.eclipsesource.jaxrs.publisher.ApplicationConfiguration;

/**
 * <p>
 * Tracker for OSGi Services implementing the {@link ApplicationConfiguration} interface.
 * </p> 
 */
public class ApplicationConfigurationTracker extends ServiceTracker {

  private final JAXRSConnector connector;

  ApplicationConfigurationTracker( BundleContext context, JAXRSConnector connector ) {
    super( context, ApplicationConfiguration.class.getName(), null );
    this.connector = connector;
  }

  @Override
  public Object addingService( ServiceReference reference ) {
    return connector.addApplicationConfiguration( reference );
  }

  @Override
  public void removedService( ServiceReference reference, Object service ) {
    if( service instanceof ApplicationConfiguration ) {
      connector.removeApplicationConfiguration( reference, ( ApplicationConfiguration )service );
    }
  }
}
