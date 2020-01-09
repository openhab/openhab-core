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
package org.openhab.core.storage.mapdb.internal;

import java.io.File;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.openhab.core.config.core.ConfigConstants;
import org.openhab.core.storage.DeletableStorage;
import org.openhab.core.storage.DeletableStorageService;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of {@link StorageService} provides abilities to store
 * data in the lightweight key-value-store <a href="http://www.mapdb.org">MapDB</a>.
 *
 * @author Thomas Eichstaedt-Engelen - Initial contribution
 * @author Alex Tugarev - Added getStorage for name only
 * @author Markus Rathgeb - Use {@link DeletableStorageService}
 */
@Component(name = "org.openhab.core.storage.mapdb", configurationPid = "org.openhab.storage.mapdb", immediate = true, service = {
        StorageService.class, DeletableStorageService.class }, property = "storage.format=mapdb")
@NonNullByDefault
public class MapDbStorageService implements DeletableStorageService {

    private final Logger logger = LoggerFactory.getLogger(MapDbStorageService.class);

    /* the name of the mapdb database ({@code storage.mapdb}) */
    private static final String DB_FILE_NAME = "storage.mapdb";

    /* holds the local instance of the MapDB database */
    private @NonNullByDefault({}) DB db;

    /* the folder name to store mapdb databases ({@code mapdb} by default) */
    private String dbFolderName = "mapdb";

    @Activate
    public void activate() {
        dbFolderName = ConfigConstants.getUserDataFolder() + File.separator + dbFolderName;
        File folder = new File(dbFolderName);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File dbFile = new File(dbFolderName, DB_FILE_NAME);
        db = DBMaker.newFileDB(dbFile).closeOnJvmShutdown().make();

        logger.debug("Opened MapDB file at '{}'.", dbFile.getAbsolutePath());
    }

    @Deactivate
    public void deactivate() {
        db.close();
        logger.debug("Deactivated MapDB Storage Service.");
    }

    @Override
    public <T> DeletableStorage<T> getStorage(String name, @Nullable ClassLoader classLoader) {
        return new MapDbStorage<>(db, name, classLoader);
    }

    @Override
    public <T> DeletableStorage<T> getStorage(String name) {
        return getStorage(name, null);
    }

}
