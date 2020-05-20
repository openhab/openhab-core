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
package org.openhab.core.auth.oauth2client.internal;

import static org.openhab.core.auth.oauth2client.internal.StorageRecordType.*;

import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.StorageCipher;
import org.openhab.core.auth.oauth2client.internal.cipher.SymmetricKeyCipher;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

/**
 * This class handles the storage directly. It is internal to the OAuthClientService and there is
 * little need to study this.
 *
 * The first role of this handler storing and caching the access token response, and persisted parameters.
 *
 * The storage contains these:
 * 1. INDEX_HANDLES = json string-set of all handles
 * 2. <handle>.LastUsed = system-time-milliseconds
 * 3. <handle>.AccessTokenResponse = Json of AccessTokenResponse
 * 4. <handle>.ServiceConfiguration = Json of PersistedParameters
 *
 * If at any time, the storage is not available, it is still possible to read existing access tokens from store.
 * The last-used statistics for this access token is broken. It is a measured risk to take.
 *
 * If at any time, the storage is not available, it is not able to write any new access tokens into store.
 *
 * All entries are subject to removal if they have not been used for 183 days or more (half year).
 * The recycle is performed when then instance is deactivated
 *
 * @author Gary Tse - Initial contribution
 */
@NonNullByDefault
@Component(property = "CIPHER_TARGET=SymmetricKeyCipher")
public class OAuthStoreHandlerImpl implements OAuthStoreHandler {

    // easy mocking with protected access
    protected static final int EXPIRE_DAYS = 183;
    protected static final int ACCESS_TOKEN_CACHE_SIZE = 50;
    private static final String STORE_NAME = "StorageHandler.For.OAuthClientService";
    private static final String STORE_KEY_INDEX_OF_HANDLES = "INDEX_HANDLES";

    private final Set<String> allHandles = new HashSet<>(); // must be initialized
    private final StorageFacade storageFacade;

    private final Set<StorageCipher> allAvailableStorageCiphers = new LinkedHashSet<>();
    private Optional<StorageCipher> storageCipher = Optional.empty();

    private final Logger logger = LoggerFactory.getLogger(OAuthStoreHandlerImpl.class);

    @Activate
    public OAuthStoreHandlerImpl(final @Reference StorageService storageService) {
        storageFacade = new StorageFacade(storageService.getStorage(STORE_NAME));
    }

    @Activate
    public void activate(Map<String, Object> properties) throws GeneralSecurityException {
        // this allows future implementations to change cipher by just setting the CIPHER_TARGET
        String cipherTarget = (String) properties.getOrDefault("CIPHER_TARGET", SymmetricKeyCipher.CIPHER_ID);

        // choose the cipher by the cipherTarget
        storageCipher = allAvailableStorageCiphers.stream()
                .filter(cipher -> cipher.getUniqueCipherId().equals(cipherTarget)).findFirst();

        logger.debug("Using Cipher: {}", storageCipher
                .orElseThrow(() -> new GeneralSecurityException("No StorageCipher with target=" + cipherTarget)));
    }

    /**
     * Deactivate and free resources.
     */
    @Deactivate
    public void deactivate() {
        storageFacade.close(); // this removes old entries
        // DS will take care of other references
    }

    @Override
    public @Nullable AccessTokenResponse loadAccessTokenResponse(String handle) throws GeneralSecurityException {
        AccessTokenResponse accessTokenResponseFromStore = (AccessTokenResponse) storageFacade.get(handle,
                ACCESS_TOKEN_RESPONSE);

        if (accessTokenResponseFromStore == null) {
            // token does not exist
            return null;
        }

        AccessTokenResponse decryptedAccessToken = decryptToken(accessTokenResponseFromStore);
        return decryptedAccessToken;
    }

    @Override
    public void saveAccessTokenResponse(String handle, @Nullable AccessTokenResponse pAccessTokenResponse) {
        AccessTokenResponse accessTokenResponse = pAccessTokenResponse;
        if (accessTokenResponse == null) {
            accessTokenResponse = new AccessTokenResponse(); // put empty
        }

        AccessTokenResponse encryptedToken;
        try {
            encryptedToken = encryptToken(accessTokenResponse);
        } catch (GeneralSecurityException e) {
            logger.warn("Unable to encrypt token, storing as-is", e);
            encryptedToken = accessTokenResponse;
        }
        storageFacade.put(handle, encryptedToken);
    }

    @Override
    public void remove(String handle) {
        storageFacade.removeByHandle(handle);
    }

    @Override
    public void removeAll() {
        storageFacade.removeAll();
        allHandles.clear();
    }

    @Override
    public void savePersistedParams(String handle, @Nullable PersistedParams persistedParams) {
        storageFacade.put(handle, persistedParams);
    }

    @Override
    public @Nullable PersistedParams loadPersistedParams(String handle) {
        PersistedParams persistedParams = (PersistedParams) storageFacade.get(handle, SERVICE_CONFIGURATION);
        return persistedParams;
    }

    private AccessTokenResponse encryptToken(AccessTokenResponse accessTokenResponse) throws GeneralSecurityException {
        AccessTokenResponse encryptedAccessToken = (AccessTokenResponse) accessTokenResponse.clone();

        if (accessTokenResponse.getAccessToken() != null) {
            encryptedAccessToken.setAccessToken(encrypt(accessTokenResponse.getAccessToken()));
        }
        if (accessTokenResponse.getRefreshToken() != null) {
            encryptedAccessToken.setRefreshToken(encrypt(accessTokenResponse.getRefreshToken()));
        }
        return encryptedAccessToken;
    }

    private AccessTokenResponse decryptToken(AccessTokenResponse accessTokenResponse) throws GeneralSecurityException {
        AccessTokenResponse decryptedToken = (AccessTokenResponse) accessTokenResponse.clone();
        if (!storageCipher.isPresent()) {
            return decryptedToken; // do nothing if no cipher
        }
        logger.debug("Decrypting token: {}", accessTokenResponse);
        decryptedToken.setAccessToken(storageCipher.get().decrypt(accessTokenResponse.getAccessToken()));
        decryptedToken.setRefreshToken(storageCipher.get().decrypt(accessTokenResponse.getRefreshToken()));
        return decryptedToken;
    }

    private @Nullable String encrypt(String token) throws GeneralSecurityException {
        if (!storageCipher.isPresent()) {
            return token; // do nothing if no cipher
        } else {
            StorageCipher cipher = storageCipher.get();
            return cipher.encrypt(token);
        }
    }

    /**
     * Static policy -- don't want to change cipher on the fly!
     * There may be multiple storage ciphers, choose the one that matches the target (done at activate)
     *
     */
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE)
    protected synchronized void setStorageCipher(StorageCipher storageCipher) {
        // keep all ciphers
        allAvailableStorageCiphers.add(storageCipher);
    }

    protected synchronized void unsetStorageCipher(StorageCipher storageCipher) {
        allAvailableStorageCiphers.remove(storageCipher);
        if (this.storageCipher.isPresent() && this.storageCipher.get() == storageCipher) {
            this.storageCipher = Optional.empty();
        }
    }

    private boolean isExpired(@Nullable LocalDateTime lastUsed) {
        if (lastUsed == null) {
            return false;
        }
        // (last used + 183 days < now) then it is expired
        return lastUsed.plusDays(EXPIRE_DAYS).isBefore(LocalDateTime.now());
    }

    /**
     * This is designed to simplify all the locking required for the store.
     */
    private class StorageFacade implements AutoCloseable {
        private final Storage<String> storage;
        private final Lock storageLock = new ReentrantLock(); // for all operations on the storage
        private final Gson gson;

        public StorageFacade(Storage<String> storage) {
            this.storage = storage;
            // Add adapters for LocalDateTime
            gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class,
                            (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> LocalDateTime
                                    .parse(json.getAsString()))
                    .registerTypeAdapter(LocalDateTime.class,
                            (JsonSerializer<LocalDateTime>) (date, type,
                                    jsonSerializationContext) -> new JsonPrimitive(date.toString()))
                    .setPrettyPrinting().create();
        }

        public Set<String> getAllHandlesFromIndex() {
            Set<String> handlesFromStoreageIndex = new HashSet<>();
            try {
                String allHandlesStr = get(STORE_KEY_INDEX_OF_HANDLES);
                logger.debug("All available handles: {}", allHandlesStr);
                if (allHandlesStr == null) {
                    return handlesFromStoreageIndex;
                }
                return gson.fromJson(allHandlesStr, HashSet.class);
            } catch (RuntimeException storeNotAvailable) {
                return handlesFromStoreageIndex; // empty
            }
        }

        public @Nullable String get(String key) {
            storageLock.lock();
            try {
                return storage.get(key);
            } finally {
                storageLock.unlock();
            }
        }

        public @Nullable Object get(String handle, StorageRecordType recordType) {
            storageLock.lock();
            try {
                String value = storage.get(recordType.getKey(handle));
                if (value == null) {
                    return null;
                }

                // update last used when it is an access token
                if (ACCESS_TOKEN_RESPONSE.equals(recordType)) {
                    try {
                        AccessTokenResponse accessTokenResponse = gson.fromJson(value, AccessTokenResponse.class);
                        return accessTokenResponse;
                    } catch (Exception e) {
                        logger.error(
                                "Unable to deserialize json, discarding AccessTokenResponse.  "
                                        + "Please check json against standard or with oauth provider. json:\n{}",
                                value, e);
                        return null;
                    }
                } else if (SERVICE_CONFIGURATION.equals(recordType)) {
                    try {
                        PersistedParams params = gson.fromJson(value, PersistedParams.class);
                        return params;
                    } catch (Exception e) {
                        logger.error("Unable to deserialize json, discarding PersistedParams. json:\n{}", value, e);
                        return null;
                    }
                } else if (LAST_USED.equals(recordType)) {
                    try {
                        LocalDateTime lastUsedDate = gson.fromJson(value, LocalDateTime.class);
                        return lastUsedDate;
                    } catch (Exception e) {
                        logger.info("Unable to deserialize json, reset LAST_USED to now.  json:\n{}", value);
                        return LocalDateTime.now();
                    }
                }
                return null;
            } finally {
                storageLock.unlock();
            }
        }

        public void put(String handle, @Nullable LocalDateTime lastUsed) {
            storageLock.lock();
            try {
                if (lastUsed == null) {
                    storage.put(LAST_USED.getKey(handle), (String) null);
                } else {
                    String gsonStr = gson.toJson(lastUsed);
                    storage.put(LAST_USED.getKey(handle), gsonStr);
                }
            } finally {
                storageLock.unlock();
            }
        }

        public void put(String handle, @Nullable AccessTokenResponse accessTokenResponse) {
            storageLock.lock();
            try {
                if (accessTokenResponse == null) {
                    storage.put(ACCESS_TOKEN_RESPONSE.getKey(handle), (String) null);
                } else {
                    String gsonAccessTokenStr = gson.toJson(accessTokenResponse);
                    storage.put(ACCESS_TOKEN_RESPONSE.getKey(handle), gsonAccessTokenStr);
                    String gsonDateStr = gson.toJson(LocalDateTime.now());
                    storage.put(LAST_USED.getKey(handle), gsonDateStr);

                    if (!allHandles.contains(handle)) {
                        // update all handles index
                        allHandles.add(handle);
                        storage.put(STORE_KEY_INDEX_OF_HANDLES, gson.toJson(allHandles));
                    }
                }
            } finally {
                storageLock.unlock();
            }
        }

        public void put(String handle, @Nullable PersistedParams persistedParams) {
            storageLock.lock();
            try {
                if (persistedParams == null) {
                    storage.put(SERVICE_CONFIGURATION.getKey(handle), (String) null);
                } else {
                    String gsonPersistedParamsStr = gson.toJson(persistedParams);
                    storage.put(SERVICE_CONFIGURATION.getKey(handle), gsonPersistedParamsStr);
                    String gsonDateStr = gson.toJson(LocalDateTime.now());
                    storage.put(LAST_USED.getKey(handle), gsonDateStr);
                    if (!allHandles.contains(handle)) {
                        // update all handles index
                        allHandles.add(handle);
                        storage.put(STORE_KEY_INDEX_OF_HANDLES, gson.toJson(allHandles));
                    }
                }
            } finally {
                storageLock.unlock();
            }
        }

        public void removeByHandle(String handle) {
            logger.debug("Removing handle {} from storage", handle);
            storageLock.lock();
            try {
                if (allHandles.remove(handle)) { // entry exists and successfully removed
                    storage.remove(ACCESS_TOKEN_RESPONSE.getKey(handle));
                    storage.remove(LAST_USED.getKey(handle));
                    storage.remove(SERVICE_CONFIGURATION.getKey(handle));
                    storage.put(STORE_KEY_INDEX_OF_HANDLES, gson.toJson(allHandles)); // update all handles
                }
            } finally {
                storageLock.unlock();
            }
        }

        public void removeAll() {
            // no need any locks, the other methods will take care of this
            Set<String> allHandlesFromStore = getAllHandlesFromIndex();
            for (String handle : allHandlesFromStore) {
                removeByHandle(handle);
            }
        }

        @Override
        public void close() {
            boolean lockGained = false;
            try {
                // dont want to wait too long during shutdown or update
                lockGained = storageLock.tryLock(15, TimeUnit.SECONDS);

                // if lockGained within timeout, then try to remove old entries
                if (lockGained) {
                    String handlesSSV = this.storage.get(STORE_KEY_INDEX_OF_HANDLES);
                    if (handlesSSV != null) {
                        String[] handles = handlesSSV.trim().split(" ");
                        for (String handle : handles) {
                            LocalDateTime lastUsed = (LocalDateTime) get(handle, LAST_USED);
                            if (isExpired(lastUsed)) {
                                removeByHandle(handle);
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                // if lock is not acquired within the timeout or thread is interruted
                // then forget about the old entries, do not try to delete them.
                // re-setting thread state to interrupted
                Thread.currentThread().interrupt();
            } finally {
                if (lockGained) {
                    try {
                        storageLock.unlock();
                    } catch (IllegalMonitorStateException e) {
                        // never reach here normally
                        logger.error("Unexpected attempt to unlock without lock", e);
                    }
                }
            }
        }
    }
}
