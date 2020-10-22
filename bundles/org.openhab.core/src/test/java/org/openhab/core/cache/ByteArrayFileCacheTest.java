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
package org.openhab.core.cache;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.OpenHAB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for the {@link ByteArrayFileCache} class.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class ByteArrayFileCacheTest {

    private static final String SERVICE_PID = "org.openhab.core.test";

    private static final File USERDATA_FOLDER = new File(OpenHAB.getUserDataFolder());
    private static final File CACHE_FOLDER = new File(USERDATA_FOLDER, ByteArrayFileCache.CACHE_FOLDER_NAME);
    private static final File SERVICE_CACHE_FOLDER = new File(CACHE_FOLDER, SERVICE_PID);

    private static final String MP3_FILE_NAME = SERVICE_CACHE_FOLDER.getAbsolutePath() + "doorbell.mp3";
    private static final String TXT_FILE_NAME = SERVICE_CACHE_FOLDER.getAbsolutePath() + "doorbell.txt";

    private static @Nullable File txt_file;

    private final Logger logger = LoggerFactory.getLogger(ByteArrayFileCacheTest.class);

    private @NonNullByDefault({}) ByteArrayFileCache subject;

    @BeforeAll
    public static void init() throws IOException {
        // create temporary file
        txt_file = createTempFile();
    }

    @BeforeEach
    public void setUp() {
        subject = new ByteArrayFileCache(SERVICE_PID);
    }

    @AfterEach
    public void tearDown() {
        // delete all files
        subject.clear();
    }

    @AfterAll
    public static void cleanUp() {
        // delete all folders
        SERVICE_CACHE_FOLDER.delete();
        CACHE_FOLDER.delete();
        USERDATA_FOLDER.delete();
    }

    @Test
    public void testGetFileExtension() {
        assertThat(subject.getFileExtension("/var/log/openhab/"), is(nullValue()));
        assertThat(subject.getFileExtension("/var/log/foo.bar/"), is(nullValue()));
        assertThat(subject.getFileExtension("doorbell.mp3"), is(equalTo("mp3")));
        assertThat(subject.getFileExtension("/tmp/doorbell.mp3"), is(equalTo("mp3")));
        assertThat(subject.getFileExtension(MP3_FILE_NAME), is(equalTo("mp3")));
        assertThat(subject.getFileExtension(TXT_FILE_NAME), is(equalTo("txt")));
        assertThat(subject.getFileExtension("/var/log/openhab/.."), is(""));
        assertThat(subject.getFileExtension(".hidden"), is(equalTo("hidden")));
        assertThat(subject.getFileExtension("C:\\Program Files (x86)\\java\\bin\\javaw.exe"), is(equalTo("exe")));
        assertThat(subject.getFileExtension("https://www.youtube.com/watch?v=qYrpPrLY868"), is(nullValue()));
    }

    @Test
    public void testGetUniqueFileName() {
        String mp3UniqueFileName = subject.getUniqueFileName(MP3_FILE_NAME);
        assertThat(mp3UniqueFileName, is(equalTo(subject.getUniqueFileName(MP3_FILE_NAME))));

        String txtUniqueFileName = subject.getUniqueFileName(TXT_FILE_NAME);
        assertThat(txtUniqueFileName, is(equalTo(subject.getUniqueFileName(TXT_FILE_NAME))));

        assertThat(mp3UniqueFileName, is(not(equalTo(txtUniqueFileName))));
    }

    @Test
    public void testGet() {
        assertThrows(FileNotFoundException.class, () -> subject.get(TXT_FILE_NAME));
    }

    @Test
    public void testPut() throws IOException {
        byte[] buffer = readTempFile();
        subject.put(TXT_FILE_NAME, buffer);

        assertThat(subject.get(TXT_FILE_NAME), is(equalTo(buffer)));
    }

    @Test
    public void testPutIfAbsentAddsANewFile() throws IOException {
        byte[] buffer = readTempFile();
        subject.putIfAbsent(TXT_FILE_NAME, buffer);

        assertThat(subject.get(TXT_FILE_NAME), is(equalTo(buffer)));
    }

    @Test
    public void testPutIfAbsentDoesNotOverwriteExistingFile() throws IOException {
        byte[] buffer = readTempFile();
        subject.putIfAbsent(TXT_FILE_NAME, buffer);
        subject.putIfAbsent(TXT_FILE_NAME, TXT_FILE_NAME.getBytes());

        assertThat(subject.get(TXT_FILE_NAME), is(equalTo(buffer)));
    }

    @Test
    public void testPutIfAbsentAndGetAddsANewFile() throws IOException {
        byte[] buffer = readTempFile();

        assertThat(subject.putIfAbsentAndGet(TXT_FILE_NAME, buffer), is(equalTo(buffer)));
    }

    @Test
    public void testPutIfAbsentAndGetDoesNotOverwriteExistingFile() throws IOException {
        byte[] buffer = readTempFile();

        subject.putIfAbsentAndGet(TXT_FILE_NAME, buffer);
        assertThat(subject.putIfAbsentAndGet(TXT_FILE_NAME, TXT_FILE_NAME.getBytes()), is(equalTo(buffer)));
    }

    @Test
    public void testContainsKey() {
        assertThat(subject.containsKey(TXT_FILE_NAME), is(false));

        subject.put(TXT_FILE_NAME, readTempFile());

        assertThat(subject.containsKey(TXT_FILE_NAME), is(true));
    }

    @Test
    public void testRemove() {
        subject.put(TXT_FILE_NAME, readTempFile());
        subject.remove(TXT_FILE_NAME);

        assertThrows(FileNotFoundException.class, () -> subject.get(TXT_FILE_NAME));
    }

    @Test
    public void testClear() {
        subject.put(TXT_FILE_NAME, readTempFile());
        subject.clear();

        assertThrows(FileNotFoundException.class, () -> subject.get(TXT_FILE_NAME));
    }

    @Test
    public void clearExpiredClearsNothing() throws IOException {
        byte[] buffer = readTempFile();
        subject.put(TXT_FILE_NAME, buffer);
        subject.clearExpired();

        assertThat(subject.get(TXT_FILE_NAME), is(equalTo(buffer)));
    }

    @Test
    public void clearExpired() {
        subject = new ByteArrayFileCache(SERVICE_PID, 1);

        subject.put(TXT_FILE_NAME, readTempFile());

        // manipulate time of last use
        File fileInCache = subject.getUniqueFile(TXT_FILE_NAME);
        fileInCache.setLastModified(System.currentTimeMillis() - 2 * ByteArrayFileCache.ONE_DAY_IN_MILLIS);

        subject.clearExpired();

        assertThrows(FileNotFoundException.class, () -> subject.get(TXT_FILE_NAME));
    }

    private static File createTempFile() throws IOException {
        final File file = File.createTempFile("doorbell", "txt");
        file.deleteOnExit();
        return file;
    }

    private byte[] readTempFile() {
        if (txt_file != null) {
            try {
                return Files.readAllBytes(txt_file.toPath());
            } catch (IOException e) {
                logger.error("Error while reading temp file");
            }
        } else {
            logger.error("Temp file not found");
        }
        return new byte[0];
    }
}
