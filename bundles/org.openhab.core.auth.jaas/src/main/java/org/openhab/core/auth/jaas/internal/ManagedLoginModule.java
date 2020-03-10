package org.openhab.core.auth.jaas.internal;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.Credentials;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = LoginModule.class, property = { "jaas.realmName=openhab" })
public class ManagedLoginModule implements LoginModule {

    private final Logger logger = LoggerFactory.getLogger(ManagedLoginModule.class);

    private UserRegistry userRegistry;

    @Nullable
    private Subject subject;

    @Nullable
    private User user;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        this.subject = subject;
    }

    @Override
    public boolean login() throws LoginException {
        try {
            // try to get the UserRegistry instance
            BundleContext bundleContext = FrameworkUtil.getBundle(UserRegistry.class).getBundleContext();
            ServiceReference<UserRegistry> serviceReference = bundleContext.getServiceReference(UserRegistry.class);

            userRegistry = bundleContext.getService(serviceReference);
        } catch (Exception e) {
            logger.error("Cannot initialize the ManagedLoginModule", e);
            throw new LoginException("Authorization failed");
        }

        try {
            Credentials credentials = (Credentials) this.subject.getPrivateCredentials().iterator().next();
            userRegistry.authenticate(credentials);
            return true;
        } catch (AuthenticationException e) {
            throw new LoginException(e.getMessage());
        }
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean logout() throws LoginException {
        // TODO Auto-generated method stub
        return false;
    }

}
