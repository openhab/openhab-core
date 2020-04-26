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
package org.openhab.core.model.lsp.internal;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.xtext.XtextPackage;
import org.eclipse.xtext.resource.FileExtensionProvider;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.IResourceServiceProvider.Registry;
import org.eclipse.xtext.resource.impl.BinaryGrammarResourceFactoryImpl;
import org.eclipse.xtext.resource.impl.ResourceServiceProviderRegistryImpl;
import org.openhab.core.model.ide.ItemsIdeSetup;
import org.openhab.core.model.persistence.ide.PersistenceIdeSetup;
import org.openhab.core.model.rule.ide.RulesIdeSetup;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.model.script.engine.ScriptEngine;
import org.openhab.core.model.script.ide.ScriptIdeSetup;
import org.openhab.core.model.sitemap.ide.SitemapIdeSetup;
import org.openhab.core.model.thing.ide.ThingIdeSetup;

import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Provides the Xtext Registry for the Language Server.
 *
 * It just piggy-backs the static Resgitry instance that the runtime bundles are using anyway.
 *
 * @author Simon Kaufmann - Initial contribution
 */
@Singleton
public class RegistryProvider implements Provider<IResourceServiceProvider.Registry> {

    private IResourceServiceProvider.Registry registry;
    private final ScriptServiceUtil scriptServiceUtil;
    private final ScriptEngine scriptEngine;

    public RegistryProvider(ScriptServiceUtil scriptServiceUtil, ScriptEngine scriptEngine) {
        this.scriptServiceUtil = scriptServiceUtil;
        this.scriptEngine = scriptEngine;
    }

    @Override
    public synchronized IResourceServiceProvider.Registry get() {
        if (registry == null) {
            registry = createRegistry();
        }
        return registry;
    }

    private Registry createRegistry() {
        registerDefaultFactories();

        IResourceServiceProvider.Registry registry = new ResourceServiceProviderRegistryImpl();
        register(registry, new ItemsIdeSetup().createInjector());
        register(registry, new PersistenceIdeSetup().createInjector());
        register(registry, new RulesIdeSetup().setScriptServiceUtil(scriptServiceUtil).setScriptEngine(scriptEngine)
                .createInjector());
        register(registry, new ScriptIdeSetup().setScriptServiceUtil(scriptServiceUtil).setScriptEngine(scriptEngine)
                .createInjector());
        register(registry, new SitemapIdeSetup().createInjector());
        register(registry, new ThingIdeSetup().createInjector());
        return registry;
    }

    private void registerDefaultFactories() {
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("ecore")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
        }
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xmi")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
        }
        if (!Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().containsKey("xtextbin")) {
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xtextbin",
                    new BinaryGrammarResourceFactoryImpl());
        }
        if (!EPackage.Registry.INSTANCE.containsKey(XtextPackage.eNS_URI)) {
            EPackage.Registry.INSTANCE.put(XtextPackage.eNS_URI, XtextPackage.eINSTANCE);
        }
    }

    private void register(IResourceServiceProvider.Registry registry, Injector injector) {
        IResourceServiceProvider resourceServiceProvider = injector.getInstance(IResourceServiceProvider.class);
        FileExtensionProvider extensionProvider = injector.getInstance(FileExtensionProvider.class);
        for (String ext : extensionProvider.getFileExtensions()) {
            if (registry.getExtensionToFactoryMap().containsKey(ext)) {
                if (extensionProvider.getPrimaryFileExtension() == ext) {
                    registry.getExtensionToFactoryMap().put(ext, resourceServiceProvider);
                }
            } else {
                registry.getExtensionToFactoryMap().put(ext, resourceServiceProvider);
            }

            IResourceFactory resourceFactory = injector.getInstance(IResourceFactory.class);
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(ext, resourceFactory);
        }
    }
}
