/*******************************************************************************
 * Copyright (c) 2013 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation, ongoing development
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher;

public class ServiceProperties {
  
  /**
   * <p>
   * When registering a @Path or @Provider annotated object as an OSGi service the connector does publish
   * this resource automatically. Anyway, in some scenarios it's not wanted to publish those services. If you
   * want a resource not publish set this property as a service property with the value <code>false</code>.
   * </p>
   */
  public static String PUBLISH = "com.eclipsesource.jaxrs.publish";
  
  private ServiceProperties() {
    // prevent instantiation
  }
}
