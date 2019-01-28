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
package org.eclipse.smarthome.storage.json.internal;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.storage.Storage;
import org.eclipse.smarthome.core.storage.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of {@link StorageService} provides a mechanism to store
 * data in Json files.
 *
 * @author Chris Jackson - Initial Contribution
 */
@Component(name = "org.eclipse.smarthome.storage.json", immediate = true, property = { //
        "service.pid=org.eclipse.smarthome.storage.json", //
        "service.config.description.uri=system:json_storage", //
        "service.config.label=Json Storage", //
        "service.config.category=system", //
        "storage.format=json" })
public class JsonStorageService implements StorageService {

    private static final int MAX_FILENAME_LENGTH = 127;

    private final Logger logger = LoggerFactory.getLogger(JsonStorageService.class);

    /** the folder name to store database ({@code jsondb} by default) */
    private String dbFolderName = "jsondb";

    private final String CFG_MAX_BACKUP_FILES = "backup_files";
    private final String CFG_WRITE_DELAY = "write_delay";
    private final String CFG_MAX_DEFER_DELAY = "max_defer_delay";

    private int maxBackupFiles = 5;
    private int writeDelay = 500;
    private int maxDeferredPeriod = 60000;

    private final Map<String, JsonStorage<Object>> storageList = new HashMap<String, JsonStorage<Object>>();

    @Activate
    protected void activate(ComponentContext cContext, Map<String, Object> properties) {
        dbFolderName = ConfigConstants.getUserDataFolder() + File.separator + dbFolderName;
        File folder = new File(dbFolderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        File backup = new File(dbFolderName, "backup");
        if (!backup.exists()) {
            backup.mkdirs();
        }
        logger.debug("Json Storage Service: Activated.");

        if (properties == null || properties.isEmpty()) {
            return;
        }

        try {
            if (properties.get(CFG_MAX_BACKUP_FILES) != null) {
                maxBackupFiles = Integer.parseInt((String) properties.get(CFG_MAX_BACKUP_FILES));
            }
        } catch (NumberFormatException nfe) {
            logger.error("Value {} for {} is invalid. Using {}.", properties.get(CFG_MAX_BACKUP_FILES),
                    CFG_MAX_BACKUP_FILES, maxBackupFiles);
        }
        try {
            if (properties.get(CFG_WRITE_DELAY) != null) {
                writeDelay = Integer.parseInt((String) properties.get(CFG_WRITE_DELAY));
            }
        } catch (NumberFormatException nfe) {
            logger.error("Value {} for {} is invalid. Using {}.", properties.get(CFG_WRITE_DELAY), CFG_WRITE_DELAY,
                    writeDelay);
        }
        try {
            if (properties.get(CFG_MAX_DEFER_DELAY) != null) {
                maxDeferredPeriod = Integer.parseInt((String) properties.get(CFG_MAX_DEFER_DELAY));
            }
        } catch (NumberFormatException nfe) {
            logger.error("Value {} for {} is invalid. Using {}.", properties.get(CFG_MAX_DEFER_DELAY),
                    CFG_MAX_DEFER_DELAY, maxDeferredPeriod);
        }
    }

    @Deactivate
    protected void deactivate() {
        // Since we're using a delayed commit, we need to write out any data
        for (JsonStorage<Object> storage : storageList.values()) {
            storage.flush();
        }
        logger.debug("Json Storage Service: Deactivated.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Storage<T> getStorage(String name, ClassLoader classLoader) {
        File legacyFile = new File(dbFolderName, name + ".json");
        File escapedFile = new File(dbFolderName, urlEscapeUnwantedChars(name) + ".json");

        File file = escapedFile;
        if (legacyFile.exists()) {
            file = legacyFile;
        }

        JsonStorage<T> newStorage = new JsonStorage<T>(file, classLoader, maxBackupFiles, writeDelay,
                maxDeferredPeriod);

        JsonStorage<Object> oldStorage = storageList.put(name, (JsonStorage<Object>) newStorage);
        if (oldStorage != null) {
            oldStorage.flush();
        }
        return newStorage;
    }

    @Override
    public <T> Storage<T> getStorage(String name) {
        return getStorage(name, null);
    }

    /**
     * Escapes all invalid url characters and strips the maximum length to 127 to be used as a file name
     *
     * @param s the string to be escaped
     * @return url-encoded string or the original string if UTF-8 is not supported on the system
     */
    protected String urlEscapeUnwantedChars(String s) {
        String result;
        try {
            result = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn("Encoding UTF-8 is not supported, might generate invalid filenames.");
            result = s;
        }
        int length = Math.min(result.length(), MAX_FILENAME_LENGTH);
        return result.substring(0, length);
    }
}
