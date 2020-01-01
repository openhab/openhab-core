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
package org.openhab.core.id;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.openhab.core.config.core.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides a unique ID for the instance that can be used for identification, e.g. when
 * integrating with external systems. The UUID is generated only once and written to the file system, so that it does
 * not change over time.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class InstanceUUID {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceUUID.class);

    static final String UUID_FILE_NAME = "uuid";

    static String uuid = null;

    /**
     * Retrieves a unified unique id, based on {@link java.util.UUID.randomUUID()}
     *
     * @return a UUID which identifies the instance or null, if uuid cannot be persisted
     */
    public static synchronized String get() {
        if (uuid == null) {
            try {
                File file = new File(ConfigConstants.getUserDataFolder() + File.separator + UUID_FILE_NAME);

                if (!file.exists()) {
                    uuid = java.util.UUID.randomUUID().toString();
                    writeFile(file, uuid);
                } else {
                    uuid = readFirstLine(file);
                    if (StringUtils.isNotEmpty(uuid)) {
                        LOGGER.debug("UUID '{}' has been restored from file '{}'", file.getAbsolutePath(), uuid);
                    } else {
                        uuid = java.util.UUID.randomUUID().toString();
                        LOGGER.warn("UUID file '{}' has no content, rewriting it now with '{}'", file.getAbsolutePath(),
                                uuid);
                        writeFile(file, uuid);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Failed writing instance uuid file: {}", e.getMessage());
                return null;
            }
        }

        return uuid;
    }

    private static void writeFile(File file, String content) throws IOException {
        // create intermediary directories
        file.getParentFile().mkdirs();
        try (OutputStream outputStream = new FileOutputStream(file)) {
            IOUtils.write(content, outputStream);
        }
    }

    private static String readFirstLine(File file) {
        List<String> lines = null;
        try {
            lines = IOUtils.readLines(new FileInputStream(file));
        } catch (IOException ioe) {
            LOGGER.warn("Failed reading the UUID file '{}': {}", file.getAbsolutePath(), ioe.getMessage());
        }
        return lines == null || lines.isEmpty() ? "" : lines.get(0);
    }

}
