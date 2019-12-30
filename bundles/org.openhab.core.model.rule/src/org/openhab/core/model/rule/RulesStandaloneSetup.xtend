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
package org.openhab.core.model.rule

import com.google.inject.Guice
import com.google.inject.Injector
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.openhab.core.model.script.ScriptServiceUtil
import org.openhab.core.model.script.ScriptStandaloneSetup
import org.openhab.core.model.script.ServiceModule
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.openhab.core.model.script.engine.ScriptEngine

/** 
 * Initialization support for running Xtext languages
 * without equinox extension registry
 */
class RulesStandaloneSetup extends RulesStandaloneSetupGenerated {
    static Injector injector
    
    private ScriptServiceUtil scriptServiceUtil;
    private ScriptEngine scriptEngine;
    
    override Injector createInjectorAndDoEMFRegistration() {
        ScriptStandaloneSetup.doSetup(scriptServiceUtil, scriptEngine);

        val Injector injector = createInjector();
        register(injector);
        return injector;
    }
    
    
    def RulesStandaloneSetup setScriptServiceUtil(ScriptServiceUtil scriptServiceUtil) {
        this.scriptServiceUtil = scriptServiceUtil;
        return this;
    }
    
    def RulesStandaloneSetup setScriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
        return this;
    }

    def ScriptServiceUtil getScriptServiceUtil() {
        return scriptServiceUtil;
    }
    
    def ScriptEngine getScriptEngine() {
        return scriptEngine;
    }
    
    def static void doSetup(ScriptServiceUtil scriptServiceUtil, ScriptEngine scriptEngine) {
        if (injector === null) {
            injector = new RulesStandaloneSetup().setScriptServiceUtil(scriptServiceUtil).setScriptEngine(scriptEngine).createInjectorAndDoEMFRegistration()
        }
    }
    
    override Injector createInjector() {
        return Guice.createInjector(new ServiceModule(scriptServiceUtil, scriptEngine), new RulesRuntimeModule());
    }

    def static Injector getInjector() {
        return injector
    }
    
    def static unregister() {
        EPackage.Registry.INSTANCE.remove("https://openhab.org/model/Rules");
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().remove("rules");
        IResourceServiceProvider.Registry.INSTANCE.getExtensionToFactoryMap().remove("rules");
        
        injector = null;
    }
}
