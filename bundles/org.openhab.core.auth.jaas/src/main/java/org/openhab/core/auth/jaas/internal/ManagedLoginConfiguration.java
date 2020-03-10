package org.openhab.core.auth.jaas.internal;

import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

public class ManagedLoginConfiguration extends Configuration {

    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        return new AppConfigurationEntry[] { new AppConfigurationEntry(ManagedLoginModule.class.getCanonicalName(),
                LoginModuleControlFlag.SUFFICIENT, new HashMap<String, Object>()) };
    }

}
