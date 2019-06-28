/*******************************************************************************
 * Copyright (c) 2012,2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation
 *    Dragos Dascalita  - added properties
 *    Ivan Iliev - Performance Optimizations
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;


public class RootApplication extends Application {

  private final Map<String, Object> properties;
  private final List<Object> resources;
  private final Object lock = new Object();
  private volatile boolean dirty;

  public RootApplication() {
    resources = new LinkedList<Object>();
    properties = new HashMap<String, Object>();
  }

  void addResource( Object resource ) {
    synchronized( lock ) {
      resources.add( resource );
      dirty = true;
    }
  }

  void removeResource( Object resource ) {
    synchronized( lock ) {
      resources.remove( resource );
      dirty = true;
    }
  }

  boolean hasResources() {
    return !resources.isEmpty();
  }

  @Override
  public Set<Object> getSingletons() {
    synchronized( lock ) {
      Set<Object> currentResources = getResources();
      // when this method is called jersey has obtained our resources as they are now, we mark the
      // application as not dirty, next time a resource is added it will mark it as dirty again.
      dirty = false;
      return currentResources;
    }
  }

  public Set<Object> getResources() {
    Set<Object> singletons = new HashSet<Object>( super.getSingletons() );
    singletons.addAll( resources );
    return singletons;
  }

  @Override
  public Map<String, Object> getProperties() {
    return properties;
  }

  public void addProperty( String key, Object value ) {
    Object oldValue = properties.get( key );
    properties.put( key, value );
    // if application is not dirty but the current property is changed - mark it
    synchronized( lock ) {
      if( !dirty && ( value != oldValue && ( value == null || !value.equals( oldValue ) ) ) ) {
        dirty = true;
      }
    }
  }

  public void addProperties( Map<String, Object> properties ) {
    for( Map.Entry<String, Object> entry : properties.entrySet() ) {
      addProperty( entry.getKey(), entry.getValue() );
    }
  }

  public boolean isDirty() {
    synchronized( lock ) {
      return dirty;
    }
  }

  public void setDirty( boolean isDirty ) {
    synchronized( lock ) {
      dirty = isDirty;
    }
  }
}
