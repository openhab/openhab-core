/*******************************************************************************
 * Copyright (c) 2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation, ongoing development
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import java.util.HashMap;
import java.util.Map;

import org.glassfish.jersey.server.ServerProperties;

import com.eclipsesource.jaxrs.publisher.ApplicationConfiguration;


public class DefaultApplicationConfiguration implements ApplicationConfiguration {

  @Override
  public Map<String, Object> getProperties() {
    Map<String, Object> properties = new HashMap<>();
    // don't look for implementations described by META-INF/services/*
    properties.put( ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE, false );
    // disable auto discovery on server, as it's handled via OSGI
    properties.put( ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true );
    return properties;
  }

}
