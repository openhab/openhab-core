/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.model.sitemap

import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.resource.IResourceServiceProvider
import com.google.inject.Injector

/** 
 * Initialization support for running Xtext languages
 * without equinox extension registry
 */
class SitemapStandaloneSetup extends SitemapStandaloneSetupGenerated {
    static Injector injector;

    def static Injector doSetup() {
        if (injector === null) {
            injector = new SitemapStandaloneSetup().createInjectorAndDoEMFRegistration();
        }
        return injector;
    }

    def static void unregister() {
        EPackage.Registry.INSTANCE.remove("https://openhab.org/model/Sitemap");
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove("sitemap");
        IResourceServiceProvider.Registry.INSTANCE.getExtensionToFactoryMap().remove("sitemap");
    }
}
