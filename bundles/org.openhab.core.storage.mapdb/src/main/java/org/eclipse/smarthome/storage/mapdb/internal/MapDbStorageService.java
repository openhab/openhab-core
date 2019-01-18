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
package org.eclipse.smarthome.storage.mapdb.internal;

import java.io.File;

import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.storage.DeletableStorage;
import org.eclipse.smarthome.core.storage.DeletableStorageService;
import org.eclipse.smarthome.core.storage.StorageService;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of {@link StorageService} provides abilities to store
 * data in the lightweight key-value-store <a href="http://www.mapdb.org">MapDB</a>.
 *
 * @author Thomas.Eichstaedt-Engelen - Initial Contribution and API
 * @author Alex Tugarev - Added getStorage for name only
 * @author Markus Rathgeb - Use {@link DeletableStorageService}
 */
@Component(name = "org.eclipse.smarthome.storage.mapdb", immediate = true, service = { StorageService.class,
        DeletableStorageService.class }, property = "storage.format=mapdb")
public class MapDbStorageService implements DeletableStorageService {

    private final Logger logger = LoggerFactory.getLogger(MapDbStorageService.class);

    /** the name of the mapdb database ({@code storage.mapdb}) */
    private static final String DB_FILE_NAME = "storage.mapdb";

    /** holds the local instance of the MapDB database */
    private DB db;

    /** the folder name to store mapdb databases ({@code mapdb} by default) */
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
    public <T> DeletableStorage<T> getStorage(String name, ClassLoader classLoader) {
        return new MapDbStorage<T>(db, name, classLoader);
    }

    @Override
    public <T> DeletableStorage<T> getStorage(String name) {
        return getStorage(name, null);
    }

}
