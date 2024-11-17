import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.Credentials;
import org.openhab.core.auth.UserRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

@ExtendWith(MockitoExtension.class)
public class ManagedUserLoginModuleTest {

    @InjectMocks
    private ManagedUserLoginModule loginModule;

    @Mock
    private UserRegistry userRegistry;

    @Mock
    private Subject subject;

    @Mock
    private Credentials credentials;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private ServiceReference<UserRegistry> serviceReference;

    @BeforeEach
    public void setUp() {
        loginModule = new ManagedUserLoginModule();
        when(subject.getPrivateCredentials()).thenReturn(Collections.singleton(credentials));
    }

    @Test
    public void initializeSetsSubject() {
        loginModule.initialize(subject, null, null, Map.of());

        assertThat(loginModule.subject, is(subject));
    }

    @Test
    public void loginSucceedsWhenAuthenticationIsSuccessful() throws Exception {
        mockBundleContextAndUserRegistry();

        when(userRegistry.authenticate(credentials)).thenReturn(true);

        loginModule.initialize(subject, null, null, Map.of());
        boolean result = loginModule.login();

        assertThat(result, is(true));
    }

    @Test
    public void loginFailsWhenUserRegistryAuthenticationFails() throws Exception {
        mockBundleContextAndUserRegistry();

        doThrow(new AuthenticationException("Authentication failed")).when(userRegistry).authenticate(credentials);

        loginModule.initialize(subject, null, null, Map.of());

        LoginException exception = assertThrows(LoginException.class, () -> loginModule.login());
        assertThat(exception.getMessage(), is("Authentication failed"));
    }

    @Test
    public void loginThrowsLoginExceptionOnUserRegistryInitializationFailure() throws Exception {
        when(FrameworkUtil.getBundle(UserRegistry.class)).thenReturn(null);

        loginModule.initialize(subject, null, null, Map.of());

        LoginException exception = assertThrows(LoginException.class, () -> loginModule.login());
        assertThat(exception.getMessage(), is("Authorization failed"));
    }

    @Test
    public void commitReturnsTrue() throws Exception {
        loginModule.initialize(subject, null, null, Map.of());
        boolean result = loginModule.commit();

        assertThat(result, is(true));
    }

    @Test
    public void abortReturnsFalse() throws Exception {
        loginModule.initialize(subject, null, null, Map.of());
        boolean result = loginModule.abort();

        assertThat(result, is(false));
    }

    @Test
    public void logoutReturnsFalse() throws Exception {
        loginModule.initialize(subject, null, null, Map.of());
        boolean result = loginModule.logout();

        assertThat(result, is(false));
    }

    private void mockBundleContextAndUserRegistry() {
        when(FrameworkUtil.getBundle(UserRegistry.class).getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getServiceReference(UserRegistry.class)).thenReturn(serviceReference);
        when(bundleContext.getService(serviceReference)).thenReturn(userRegistry);
    }
}
