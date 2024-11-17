import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.auth.jaas.internal.ManagedUserLoginConfiguration;
import org.openhab.core.auth.jaas.internal.ManagedUserLoginModule;

public class ManagedUserLoginConfigurationTest {

    private ManagedUserLoginConfiguration configuration;

    @BeforeEach
    public void setUp() {
        configuration = new ManagedUserLoginConfiguration();
    }

    @Test
    public void getAppConfigurationEntryReturnsSingleEntry() {
        AppConfigurationEntry[] entries = configuration.getAppConfigurationEntry("testName");

        assertThat(entries, is(notNullValue()));
        assertThat(entries.length, is(1));
    }

    @Test
    public void getAppConfigurationEntryHasCorrectLoginModuleClass() {
        AppConfigurationEntry[] entries = configuration.getAppConfigurationEntry("testName");

        assertThat(entries[0].getLoginModuleName(), is(ManagedUserLoginModule.class.getCanonicalName()));
    }

    @Test
    public void getAppConfigurationEntryHasSufficientControlFlag() {
        AppConfigurationEntry[] entries = configuration.getAppConfigurationEntry("testName");

        assertThat(entries[0].getControlFlag(), is(LoginModuleControlFlag.SUFFICIENT));
    }

    @Test
    public void getAppConfigurationEntryHasEmptyOptions() {
        AppConfigurationEntry[] entries = configuration.getAppConfigurationEntry("testName");

        assertThat(entries[0].getOptions(), is(instanceOf(HashMap.class)));
        assertThat(entries[0].getOptions().isEmpty(), is(true));
    }
}
