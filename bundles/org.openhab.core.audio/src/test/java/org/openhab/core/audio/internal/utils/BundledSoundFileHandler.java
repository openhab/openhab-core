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
package org.openhab.core.audio.internal.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.openhab.core.audio.internal.AudioManagerTest;
import org.openhab.core.config.core.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to handle bundled resources.
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class BundledSoundFileHandler implements Closeable {
    private static final String MP3_FILE_NAME = "mp3AudioFile.mp3";
    private static final String WAV_FILE_NAME = "wavAudioFile.wav";
    private final Logger logger = LoggerFactory.getLogger(BundledSoundFileHandler.class);

    private static void copy(final String resourcePath, final String filePath) throws IOException {
        try (InputStream is = AudioManagerTest.class.getResourceAsStream(resourcePath)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);

            new File(filePath).getParentFile().mkdirs();
            try (OutputStream outStream = new FileOutputStream(filePath)) {
                outStream.write(buffer);
            }
        }
    }

    private final Path tmpdir;
    private final String mp3FilePath;
    private final String wavFilePath;

    public BundledSoundFileHandler() throws IOException {
        tmpdir = Files.createTempDirectory(null);

        final Path configdir = tmpdir.resolve("configdir");
        System.setProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT, configdir.toString());

        mp3FilePath = configdir.resolve("sounds/" + MP3_FILE_NAME).toString();
        copy("/configuration/sounds/mp3AudioFile.mp3", mp3FilePath);
        wavFilePath = configdir.resolve("sounds/" + WAV_FILE_NAME).toString();
        copy("/configuration/sounds/wavAudioFile.wav", wavFilePath);
    }

    @Override
    public void close() {
        System.setProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT, ConfigConstants.DEFAULT_CONFIG_FOLDER);

        if (tmpdir != null) {
            try {
                Files.walk(tmpdir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException ex) {
                logger.error("Exception while deleting files", ex);
            }
        }
    }

    public String mp3FileName() {
        return MP3_FILE_NAME;
    }

    public String mp3FilePath() {
        return mp3FilePath;
    }

    public String wavFileName() {
        return WAV_FILE_NAME;
    }

    public String wavFilePath() {
        return wavFilePath;
    }

}
