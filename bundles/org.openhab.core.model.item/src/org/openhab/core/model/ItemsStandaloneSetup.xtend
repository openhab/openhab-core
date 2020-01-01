/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.model

import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.resource.IResourceServiceProvider

/** 
 * Initialization support for running Xtext languages
 * without equinox extension registry
 */
class ItemsStandaloneSetup extends ItemsStandaloneSetupGenerated {
    def static void doSetup() {
        new ItemsStandaloneSetup().createInjectorAndDoEMFRegistration()
    }
    
    def static void unregister() {
        EPackage.Registry.INSTANCE.remove("https://openhab.org/model/Items");
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove("items");
        IResourceServiceProvider.Registry.INSTANCE.getExtensionToFactoryMap().remove("items");
    }
}
