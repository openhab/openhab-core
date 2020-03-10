package org.openhab.core.internal.auth;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.Credentials;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserProvider;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.openhab.core.common.registry.AbstractRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
@Component(service = UserRegistry.class, immediate = true)
public class UserRegistryImpl extends AbstractRegistry<User, String, UserProvider> implements UserRegistry {

    private final Logger logger = LoggerFactory.getLogger(UserRegistry.class);

    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 512;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";

    private int rank = 0;

    private static final SecureRandom RAND = new SecureRandom();

    @Activate
    public UserRegistryImpl(BundleContext context, Map<String, Object> properties) {
        super(UserProvider.class);
        super.activate(context);

        Object serviceRanking = properties.get(Constants.SERVICE_RANKING);
        if (serviceRanking instanceof Integer) {
            rank = (Integer) serviceRanking;
        } else {
            rank = 1; // takes precedence over other providers usually ranked 0
        }
    }

    // @Activate
    // protected void activate(BundleContext context, Map<String, Object> properties) {
    //
    // }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedUserProvider managedProvider) {
        super.setManagedProvider(managedProvider);
        super.addProvider(managedProvider);
    }

    protected void unsetManagedProvider(ManagedUserProvider managedProvider) {
        super.unsetManagedProvider(managedProvider);
        super.removeProvider(managedProvider);
    }

    @Override
    public User register(String username, String password, Set<String> roles) {
        String passwordSalt = generateSalt(KEY_LENGTH / 8).get();
        String passwordHash = hashPassword(password, passwordSalt).get();
        ManagedUser user = new ManagedUser(username, passwordSalt, passwordHash);
        user.setRoles(roles);
        super.add(user);
        return user;
    }

    private Optional<String> generateSalt(final int length) {

        if (length < 1) {
            logger.error("error in generateSalt: length must be > 0");
            return Optional.empty();
        }

        byte[] salt = new byte[length];
        RAND.nextBytes(salt);

        return Optional.of(Base64.getEncoder().encodeToString(salt));
    }

    private Optional<String> hashPassword(String password, String salt) {

        char[] chars = password.toCharArray();
        byte[] bytes = salt.getBytes();

        PBEKeySpec spec = new PBEKeySpec(chars, bytes, ITERATIONS, KEY_LENGTH);

        Arrays.fill(chars, Character.MIN_VALUE);

        try {
            SecretKeyFactory fac = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] securePassword = fac.generateSecret(spec).getEncoded();
            return Optional.of(Base64.getEncoder().encodeToString(securePassword));

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("Exception encountered in hashPassword", e);
            return Optional.empty();

        } finally {
            spec.clearPassword();
        }
    }

    @Override
    public Authentication authenticate(Credentials credentials) throws AuthenticationException {
        UsernamePasswordCredentials usernamePasswordCreds = (UsernamePasswordCredentials) credentials;
        User user = this.get(usernamePasswordCreds.getUsername());
        if (user == null) {
            throw new AuthenticationException("User not found");
        }
        if (!(user instanceof ManagedUser)) {
            throw new AuthenticationException("User is not managed");
        }

        ManagedUser managedUser = (ManagedUser) user;
        String hashedPassword = hashPassword(usernamePasswordCreds.getPassword(), managedUser.getPasswordSalt()).get();
        if (!hashedPassword.equals(managedUser.getPasswordHash())) {
            throw new AuthenticationException("Wrong password");
        }

        Authentication authentication = new Authentication(managedUser.getName());
        return authentication;
    }

    @Override
    public boolean supports(Class<? extends Credentials> type) {
        return (UsernamePasswordCredentials.class.isAssignableFrom(type));
    }

}
