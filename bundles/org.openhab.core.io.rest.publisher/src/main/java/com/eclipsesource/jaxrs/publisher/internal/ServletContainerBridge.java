/*******************************************************************************
 * Copyright (c) 2014,2015 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation
 *    Ivan Iliev - Performance Optimizations
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Request;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;


public class ServletContainerBridge extends HttpServlet implements Runnable {

  private final RootApplication application;
  private ServletContainer servletContainer;
  private ServletConfig servletConfig;
  private volatile boolean isJerseyReady;

  public ServletContainerBridge( RootApplication application ) {
    this.servletContainer = new ServletContainer( ResourceConfig.forApplication( application ) );
    this.application = application;
    this.isJerseyReady = false;
  }

  @Override
  public void run() {
    if( application.isDirty() ) {
      ClassLoader original = Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread().setContextClassLoader( Request.class.getClassLoader() );
        synchronized( this ) {
          if( !isJerseyReady() ) {
            // if jersey has not been initialized - use the init method
            getServletContainer().init( servletConfig );
          } else {
            // otherwise reload
            isJerseyReady = false;
            getServletContainer().reload( ResourceConfig.forApplication( application ) );
          }
          isJerseyReady = true;
        }
      } catch( ServletException e ) {
        throw new RuntimeException( e );
      } finally {
        Thread.currentThread().setContextClassLoader( original );
      }
    }
  }

  @Override
  public void init( ServletConfig config ) throws ServletException {
    application.setDirty( true );
    this.servletConfig = config;
  }

  @Override
  public void service( ServletRequest req, ServletResponse res ) throws ServletException, IOException {
    // if jersey has not yet been initialized return service unavailable
    if( isJerseyReady() ) {
      getServletContainer().service( req, res );
    } else {
      ( ( HttpServletResponse )res ).sendError( HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Jersey is not ready yet!" );
    }
  }

  @Override
  public void destroy() {
    synchronized( this ) {
      if( isJerseyReady() ) {
        getServletContainer().destroy();
        this.isJerseyReady = false;
        // create a new ServletContainer when the old one is destroyed.
        this.servletContainer = new ServletContainer( ResourceConfig.forApplication( application ) );
      }
    }
  }

  // for testing purposes
  ServletContainer getServletContainer() {
    return servletContainer;
  }

  // for testing purposes
  boolean isJerseyReady() {
    return isJerseyReady;
  }
}
