/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.internal.auth;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.AuthenticatedUser;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.Credentials;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserApiToken;
import org.openhab.core.auth.UserApiTokenCredentials;
import org.openhab.core.auth.UserProvider;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UserSession;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.common.registry.Provider;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of a {@link UserRegistry} for {@link ManagedUser} entities.
 *
 * @author Yannick Schaus - initial contribution
 */
@NonNullByDefault
@Component(service = { UserRegistry.class, AuthenticationProvider.class }, immediate = true)
public class UserRegistryImpl extends AbstractRegistry<User, String, UserProvider> implements UserRegistry {

    private final Logger logger = LoggerFactory.getLogger(UserRegistryImpl.class);

    private static final int PASSWORD_ITERATIONS = 65536;
    private static final int APITOKEN_ITERATIONS = 1024;
    private static final String APITOKEN_PREFIX = "oh";
    private static final int KEY_LENGTH = 512;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final SecureRandom RAND = new SecureRandom();

    @Activate
    public UserRegistryImpl(BundleContext context, Map<String, Object> properties) {
        super(UserProvider.class);
        super.activate(context);
    }

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
    public User register(String username, char[] password, Set<String> roles) {
        byte[] passwordSalt = generateSalt(KEY_LENGTH / 8);
        String passwordHash = hash(password, passwordSalt, PASSWORD_ITERATIONS).get();
        ManagedUser user = new ManagedUser(username, new String(passwordSalt, StandardCharsets.UTF_8), passwordHash);
        user.setRoles(new HashSet<>(roles));
        super.add(user);
        return user;
    }

    private byte[] generateSalt(final int length) {
        if (length < 1) {
            logger.error("error in generateSalt: length must be > 0");
            return new byte[0];
        }

        byte[] salt = new byte[length];
        RAND.nextBytes(salt);

        return Base64.getEncoder().encode(salt);
    }

    private Optional<String> hash(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);

        Arrays.fill(password, Character.MIN_VALUE);

        try {
            SecretKeyFactory fac = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] securePassword = fac.generateSecret(spec).getEncoded();
            return Optional.of(Base64.getEncoder().encodeToString(securePassword));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            logger.error("Exception encountered while hashing", e);
            return Optional.empty();
        } finally {
            spec.clearPassword();
        }
    }

    @Override
    public Authentication authenticate(Credentials credentials, boolean dispose) throws AuthenticationException {
        try {
            if (credentials instanceof UsernamePasswordCredentials usernamePasswordCreds) {
                User user = get(usernamePasswordCreds.getUsername());
                if (user == null) {
                    throw new AuthenticationException("User not found: " + usernamePasswordCreds.getUsername());
                }

                ManagedUser managedUser = (ManagedUser) user;
                String hashedPassword = hash(usernamePasswordCreds.getPassword(),
                        managedUser.getPasswordSalt().getBytes(StandardCharsets.UTF_8), PASSWORD_ITERATIONS).get();
                if (!hashedPassword.equals(managedUser.getPasswordHash())) {
                    throw new AuthenticationException("Wrong password for user " + usernamePasswordCreds.getUsername());
                }

                return new Authentication(managedUser.getName(),
                        managedUser.getRoles().stream().toArray(String[]::new));
            } else if (credentials instanceof UserApiTokenCredentials apiTokenCreds) {
                char[] apiToken = apiTokenCreds.getApiToken();
                int sepNum = 0;
                int offset = 0;
                char[][] parts = new char[2][];
                for (int i = 0; i < apiToken.length && sepNum < 2; i++) {
                    if (apiToken[i] == '.') {
                        parts[sepNum] = new char[i - offset];
                        System.arraycopy(apiToken, offset, parts[sepNum++], 0, i - offset);
                        offset = i + 1;
                    }
                }
                if (sepNum != 2 || !APITOKEN_PREFIX.equals(String.valueOf(parts[0]))) {
                    throw new AuthenticationException("Invalid API token format");
                }

                String name = String.valueOf(parts[1]);
                for (User user : getAll()) {
                    if (user instanceof AuthenticatedUser authenticatedUser) {
                        for (UserApiToken userApiToken : authenticatedUser.getApiTokens()) {
                            // only check if the name in the token matches
                            if (!userApiToken.getName().equals(name)) {
                                continue;
                            }
                            String incomingTokenHash = hash(apiTokenCreds.getApiToken(), userApiToken.getSalt(),
                                    APITOKEN_ITERATIONS).get();

                            if (incomingTokenHash.equals(userApiToken.getTokenHash())) {
                                return new Authentication(authenticatedUser.getName(),
                                        authenticatedUser.getRoles().toArray(String[]::new), userApiToken.getScope());
                            }
                        }
                    }
                }

                throw new AuthenticationException("Unknown API token");
            }

            throw new IllegalArgumentException("Invalid credential type");
        } finally {
            if (dispose) {
                credentials.dispose();
            }
        }
    }

    @Override
    public void changePassword(User user, char[] newPassword) {
        if (!(user instanceof ManagedUser)) {
            throw new IllegalArgumentException("User is not managed: " + user.getName());
        }

        ManagedUser managedUser = (ManagedUser) user;
        byte[] passwordSalt = generateSalt(KEY_LENGTH / 8);
        String passwordHash = hash(newPassword, passwordSalt, PASSWORD_ITERATIONS).get();
        managedUser.setPasswordSalt(new String(passwordSalt, StandardCharsets.UTF_8));
        managedUser.setPasswordHash(passwordHash);
        update(user);
    }

    @Override
    public void addUserSession(User user, UserSession session) {
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            throw new IllegalArgumentException("User authentication is not managed by openHAB: " + user.getName());
        }

        authenticatedUser.getSessions().add(session);
        update(user);
    }

    @Override
    public void removeUserSession(User user, UserSession session) {
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            throw new IllegalArgumentException("User authentication is not managed by openHAB: " + user.getName());
        }

        authenticatedUser.getSessions().remove(session);
        update(user);
    }

    @Override
    public void clearSessions(User user) {
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            throw new IllegalArgumentException("User authentication is not managed by openHAB: " + user.getName());
        }

        authenticatedUser.getSessions().clear();
        update(user);
    }

    @Override
    public char[] addUserApiToken(User user, String name, String scope) {
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            throw new IllegalArgumentException("User authentication is not managed by openHAB: " + user.getName());
        }
        if (!name.matches("[a-zA-Z0-9]*")) {
            throw new IllegalArgumentException("API token name format invalid, alphanumeric characters only");
        }

        byte[] tokenSalt = generateSalt(KEY_LENGTH / 8);
        byte[] rnd = new byte[64];
        RAND.nextBytes(rnd);

        byte[] base64Bytes = Base64.getEncoder().encode(rnd);
        char[] nameChars = name.toCharArray();
        int maxCapacity = APITOKEN_PREFIX.length() + nameChars.length + base64Bytes.length + 2;
        CharBuffer buffer = CharBuffer.allocate(maxCapacity);
        buffer.put(APITOKEN_PREFIX);
        buffer.put('.');
        buffer.put(nameChars);
        buffer.put('.');

        char c;
        for (byte b : base64Bytes) {
            c = (char) (b & 0xff); // Safe ASCII cast for Base64 characters
            if (c != '+' && c != '/' && c != '=') {
                buffer.put(c);
            }
        }
        buffer.flip();
        char[] token = new char[buffer.remaining()];
        buffer.get(token);

        // Clean up temporary buffers
        Arrays.fill(base64Bytes, (byte) 0);
        Arrays.fill(buffer.array(), Character.MIN_VALUE);

        char[] disposable = new char[token.length];
        System.arraycopy(token, 0, disposable, 0, token.length);
        String tokenHash = hash(disposable, tokenSalt, APITOKEN_ITERATIONS).get();

        UserApiToken userApiToken = new UserApiToken(name, tokenHash, tokenSalt, scope);

        authenticatedUser.getApiTokens().add(userApiToken);
        update(user);

        return token;
    }

    @Override
    public void removeUserApiToken(User user, UserApiToken userApiToken) {
        if (!(user instanceof AuthenticatedUser authenticatedUser)) {
            throw new IllegalArgumentException("User authentication is not managed by openHAB: " + user.getName());
        }

        authenticatedUser.getApiTokens().remove(userApiToken);
        update(user);
    }

    @Override
    public @Nullable User update(User element) {
        String key = element.getName();
        Provider<User> provider = getProvider(key);
        // If the provider of this element is a ManagedProvider,
        // invoke the update method of that provider instead of the default one
        // This allows for registering additional ManagedProviders, e.g., for providing LDAP users
        if (provider instanceof ManagedProvider<User, ?> managedProvider) {
            return managedProvider.update(element);
        }
        return super.update(element);
    }

    @Override
    public boolean supports(Class<? extends Credentials> type) {
        return (UsernamePasswordCredentials.class.isAssignableFrom(type));
    }
}
