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
package org.eclipse.smarthome.automation.sample.extension.java.internal;

import org.eclipse.smarthome.automation.sample.extension.java.internal.handler.WelcomeHomeHandlerFactory;
import org.eclipse.smarthome.automation.sample.extension.java.internal.template.WelcomeHomeTemplateProvider;
import org.eclipse.smarthome.automation.sample.extension.java.internal.type.WelcomeHomeModuleTypeProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This class is responsible for starting and stopping the application that gives ability to the user to switch on the
 * air conditioner and lights in its home remotely. It initializes and registers or unregisters the
 * services that provide this functionality - Rule Provider, Rule Template Provider, Module Type Provider and Handler
 * Factory for handlers of the modules that compose the rules. Of course, these providers are not mandatory for each
 * application. Some applications may contain only Template Provider or Rule Provider, or Module Type Provider, or
 * Module Handler Factory for some particular module types. Also, to enable the user to have control over the settings
 * and to enforce execution, the demo initializes and registers a service that provides console commands.
 *
 * @author Ana Dimova - Initial Contribution
 *
 */
public class Activator implements BundleActivator {

    private WelcomeHomeModuleTypeProvider mtProvider;
    private WelcomeHomeTemplateProvider tProvider;
    private WelcomeHomeRulesProvider rulesProvider;
    private WelcomeHomeHandlerFactory handlerFactory;
    private WelcomeHomeCommands commands;

    @Override
    public void start(BundleContext context) throws Exception {
        mtProvider = new WelcomeHomeModuleTypeProvider();
        mtProvider.register(context);

        tProvider = new WelcomeHomeTemplateProvider();
        tProvider.register(context);

        rulesProvider = new WelcomeHomeRulesProvider();
        rulesProvider.register(context);

        handlerFactory = new WelcomeHomeHandlerFactory();
        handlerFactory.register(context);

        commands = new WelcomeHomeCommands(context, rulesProvider, handlerFactory);
        commands.register(context);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        mtProvider.unregister();
        mtProvider = null;

        tProvider.unregister();
        tProvider = null;

        commands.unregister();
        commands = null;

        rulesProvider.unregister();
        rulesProvider = null;

        handlerFactory.unregister();
        handlerFactory = null;
    }

}
