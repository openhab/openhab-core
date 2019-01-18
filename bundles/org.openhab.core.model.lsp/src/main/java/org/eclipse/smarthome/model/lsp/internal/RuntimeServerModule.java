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
package org.eclipse.smarthome.model.lsp.internal;

import java.util.concurrent.ExecutorService;

import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.model.script.ScriptServiceUtil;
import org.eclipse.smarthome.model.script.engine.ScriptEngine;
import org.eclipse.xtext.ide.ExecutorServiceProvider;
import org.eclipse.xtext.ide.server.DefaultProjectDescriptionFactory;
import org.eclipse.xtext.ide.server.IProjectDescriptionFactory;
import org.eclipse.xtext.ide.server.IWorkspaceConfigFactory;
import org.eclipse.xtext.ide.server.LanguageServerImpl;
import org.eclipse.xtext.ide.server.ProjectWorkspaceConfigFactory;
import org.eclipse.xtext.ide.server.UriExtensions;
import org.eclipse.xtext.resource.IContainer;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.containers.ProjectDescriptionBasedContainerManager;

import com.google.inject.AbstractModule;

/**
 * This class configures the injector for the Language Server.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class RuntimeServerModule extends AbstractModule {

    private final ScriptServiceUtil scriptServiceUtil;
    private final ScriptEngine scriptEngine;

    public RuntimeServerModule(ScriptServiceUtil scriptServiceUtil, ScriptEngine scriptEngine) {
        this.scriptServiceUtil = scriptServiceUtil;
        this.scriptEngine = scriptEngine;
    }

    @Override
    protected void configure() {
        binder().bind(ExecutorService.class).toProvider(ExecutorServiceProvider.class);

        bind(UriExtensions.class).toInstance(new MappingUriExtensions(ConfigConstants.getConfigFolder()));
        bind(LanguageServer.class).to(LanguageServerImpl.class);
        bind(IResourceServiceProvider.Registry.class).toProvider(new RegistryProvider(scriptServiceUtil, scriptEngine));
        bind(IWorkspaceConfigFactory.class).to(ProjectWorkspaceConfigFactory.class);
        bind(IProjectDescriptionFactory.class).to(DefaultProjectDescriptionFactory.class);
        bind(IContainer.Manager.class).to(ProjectDescriptionBasedContainerManager.class);
    }

}
