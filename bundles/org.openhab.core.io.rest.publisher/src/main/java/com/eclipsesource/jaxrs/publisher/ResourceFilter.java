/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher;

import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;


/**
 * <p>
 * The JAX-RS publisher uses a {@link ServiceTracker} to track all services. If one has an @Path, @Provider etc. 
 * annotation it will be published. In rare cases it's necessary to modify the filter for the tracker e.g. to only
 * publish resource starting with "com.foo.*".
 * </p>
 * <p>
 * To accomplish this you can register a {@link ResourceFilter} as an OSGi service. The JAX-RS publisher will prefer 
 * use this service to construct it's {@link ServiceTracker}.
 * </p>
 * 
 * <p>
 * <b>Please Note:</b> Right now the {@link ResourceFilter} service must be registered before the publisher bundle 
 * is started. Dynamic behavior may follow in future versions.
 * </p>
 * 
 * @see Filter
 * 
 * @since 4.0
 */
public interface ResourceFilter {
  
  /**
   * <p>
   * The OSGi filter to use for tracking the Resources. Must not return <code>null</code>.
   * </p>
   */
  Filter getFilter();
  
}
