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
package org.openhab.core.tools.internal;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.openhab.core.tools.Upgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MapDBUpgrader} removes the default persistence strategy.
 *
 * It upgrades MapDB data from v1 to v3.
 *
 * @author Holger Friedrich - Initial Contribution
 */
@NonNullByDefault
public class MapDBUpgrader implements Upgrader {
    private final Logger logger = LoggerFactory.getLogger(MapDBUpgrader.class);

    @Override
    public String getName() {
        return "mapDBv1 to v3 upgrader";
    }

    @Override
    public String getDescription() {
        return "Move persistence default strategy configuration to all persistence configuration without strategy defined";
    }

    @Override
    public boolean execute(@Nullable Path userdataPath, @Nullable Path confPath) {
        if (userdataPath == null) {
            logger.error("{} skipped: no userdata directory found.", getName());
            return false;
        }

        try {
            final Path DB_DIR = userdataPath.resolve("persistence").resolve("mapdb");
            final String DB_V1_FILE_NAME = "storage.mapdb";
            final String DB_V3_FILE_NAME = "storage3.mapdb";

            File dbv1File = DB_DIR.resolve(DB_V1_FILE_NAME).toFile();
            if (dbv1File.exists()) {
                logger.info("Found MapDB v1 file at {}, starting upgrade to v3", dbv1File.getAbsolutePath());
            } else {
                logger.info("No MapDB v1 file found at {}, skipping MapDB upgrade", dbv1File.getAbsolutePath());
                return true; // No old DB, nothing to upgrade, but not an error
            }
            // Open old store with MapDB 1.x (fully qualified to avoid name clash)
            org.mapdb.v1.DB oldDb = org.mapdb.v1.DBMaker.newFileDB(dbv1File).readOnly().make();

            // Open new store with MapDB 3.x (imported normally)
            File dbv3File = DB_DIR.resolve(DB_V3_FILE_NAME).toFile();
            if (dbv3File.exists()) {
                logger.warn("MapDB v3 file already exists at {}, it will be renamed to .bak",
                        dbv3File.getAbsolutePath());

                File backupFile = new File(dbv3File.getAbsolutePath() + ".bak");
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                if (!dbv3File.renameTo(backupFile)) {
                    logger.error("Failed to rename existing MapDB v3 file at {}, aborting upgrade",
                            dbv3File.getAbsolutePath());
                    return false;
                }
            }
            DB newDb = DBMaker.fileDB(dbv3File).fileMmapEnable().make();

            try {
                Map<String, String> oldMap = oldDb.getTreeMap("itemStore");

                ConcurrentMap<String, String> newMap = newDb.treeMap("itemStore", Serializer.STRING, Serializer.STRING)
                        .createOrOpen();

                newMap.putAll(oldMap);
                newDb.commit();

                logger.info("Migrated " + newMap.size() + " entries.");
            } finally {
                oldDb.close();
                newDb.close();
            }

            // finally remove MapDB 1.x file to mark the update as successful and avoid re-doing migration on --force
            File backupFile = new File(dbv1File.getAbsolutePath() + ".bak");
            if (backupFile.exists()) {
                backupFile.delete();
            }
            dbv1File.renameTo(backupFile);
            logger.info("MapDB upgrade completed successfully, old file renamed to .bak.");

            return true;
        } catch (Throwable e) {
            logger.error("Error during MapDB upgrade: ", e);
            return false;
        }
    }
}
