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
package org.openhab.core.karaf.internal.jaas;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.openhab.core.auth.UserRegistry;
import org.osgi.service.component.annotations.Component;

/**
 * A JAAS realm description for the {@link UserRegistry} based login module.
 *
 * @author Yannick Schaus - initial contribution
 */
@Singleton
@Component(service = JaasRealm.class)
@Service
public class ManagedUserRealm implements JaasRealm {

    public static final String REALM_NAME = "openhab";
    public static final String MODULE_CLASS = "org.openhab.core.auth.jaas.internal.ManagedUserLoginModule";

    @Override
    public String getName() {
        return REALM_NAME;
    }

    @Override
    public int getRank() {
        return 1;
    }

    @Override
    public AppConfigurationEntry[] getEntries() {
        Map<String, Object> options = new HashMap<>();
        options.put(ProxyLoginModule.PROPERTY_MODULE, MODULE_CLASS);

        return new AppConfigurationEntry[] {
                new AppConfigurationEntry(MODULE_CLASS, LoginModuleControlFlag.SUFFICIENT, options) };
    }

}
