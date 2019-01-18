/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.thing

import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.resource.IResourceServiceProvider

/** 
 * Initialization support for running Xtext languages
 * without equinox extension registry
 */
class ThingStandaloneSetup extends ThingStandaloneSetupGenerated {
    def static void doSetup() {
        new ThingStandaloneSetup().createInjectorAndDoEMFRegistration()
    }
    
    def static void unregister() {
        EPackage.Registry.INSTANCE.remove("http://www.eclipse.org/smarthome/model/Thing");
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove("things");
        IResourceServiceProvider.Registry.INSTANCE.getExtensionToFactoryMap().remove("things");
    }
    
}
