/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.internal.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.JavaTest;
import org.openhab.core.OpenHAB;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.service.WatchService;
import org.openhab.core.service.WatchService.Kind;
import org.osgi.framework.BundleContext;

/**
 * The {@link WatchServiceImplTest} is a
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WatchServiceImplTest extends JavaTest {
    private static final String SUB_DIR_PATH_NAME = "subDir";
    private static final String TEST_FILE_NANE = "testFile";

    private @NonNullByDefault({}) String systemConfDirProperty;

    private @NonNullByDefault({}) WatchServiceImpl.WatchServiceConfiguration configurationMock;

    private @NonNullByDefault({}) WatchServiceImpl watchService;
    private @NonNullByDefault({}) Path rootPath;
    private @NonNullByDefault({}) Path subDirPath;
    private @NonNullByDefault({}) TestWatchEventListener listener;

    @BeforeEach
    public void setup() throws IOException {
        // store property so we can restore later
        systemConfDirProperty = System.getProperty(OpenHAB.CONFIG_DIR_PROG_ARGUMENT);

        rootPath = Files.createDirectories(Path.of("target", "test-watcher"));
        subDirPath = Files.createDirectories(rootPath.resolve(SUB_DIR_PATH_NAME));
        ExecutorService ex = ThreadPoolManager.getScheduledPool("file-processing");
        System.setProperty(OpenHAB.CONFIG_DIR_PROG_ARGUMENT, rootPath.toString());

        when(configurationMock.name()).thenReturn("unnamed");
        when(configurationMock.path()).thenReturn("");

        watchService = new WatchServiceImpl(configurationMock, mock(BundleContext.class));
        listener = new TestWatchEventListener();
    }

    @AfterEach
    public void tearDown() throws IOException {
        watchService.deactivate();
        System.setProperty(OpenHAB.CONFIG_DIR_PROG_ARGUMENT, systemConfDirProperty);
    }

    @Test
    private void testFileInWatchedDir() throws IOException, InterruptedException {
        watchService.registerListener(listener, Path.of(""), false);

        Path testFile = rootPath.resolve(TEST_FILE_NANE);
        Path relativeTestFilePath = Path.of(TEST_FILE_NANE);

        Files.writeString(testFile, "initial content", StandardCharsets.UTF_8);
        assertEvent(relativeTestFilePath, Kind.CREATE);

        Files.writeString(testFile, "modified content", StandardCharsets.UTF_8);
        assertEvent(relativeTestFilePath, Kind.MODIFY);

        Files.writeString(testFile, "modified content", StandardCharsets.UTF_8);
        assertNoEvent();

        Files.delete(testFile);
        assertEvent(relativeTestFilePath, Kind.DELETE);
    }

    @Test
    private void testFileInWatchedSubDir() throws IOException, InterruptedException {
        // listener is listening to root and sub-dir
        watchService.registerListener(listener, Path.of(""), false);

        Path testFile = rootPath.resolve(SUB_DIR_PATH_NAME).resolve(TEST_FILE_NANE);
        Path relativeTestFilePath = Path.of(SUB_DIR_PATH_NAME, TEST_FILE_NANE);

        Files.writeString(testFile, "initial content", StandardCharsets.UTF_8);
        assertEvent(relativeTestFilePath, Kind.CREATE);

        Files.writeString(testFile, "modified content", StandardCharsets.UTF_8);
        assertEvent(relativeTestFilePath, Kind.MODIFY);

        Files.writeString(testFile, "modified content", StandardCharsets.UTF_8);
        assertNoEvent();

        Files.delete(testFile);
        assertEvent(relativeTestFilePath, Kind.DELETE);
    }

    @Test
    private void testFileInWatchedSubDir2() throws IOException, InterruptedException {
        // listener is only listening to sub-dir of root
        watchService.registerListener(listener, Path.of(SUB_DIR_PATH_NAME), false);

        Path testFile = rootPath.resolve(SUB_DIR_PATH_NAME).resolve(TEST_FILE_NANE);
        Path relativeTestFilePath = Path.of(TEST_FILE_NANE);

        Files.writeString(testFile, "initial content", StandardCharsets.UTF_8);
        assertEvent(relativeTestFilePath, Kind.CREATE);

        Files.writeString(testFile, "modified content", StandardCharsets.UTF_8);
        assertEvent(relativeTestFilePath, Kind.MODIFY);

        Files.writeString(testFile, "modified content", StandardCharsets.UTF_8);
        assertNoEvent();

        Files.delete(testFile);
        assertEvent(relativeTestFilePath, Kind.DELETE);
    }

    @Test
    private void testFileInUnwatchedSubDir() throws IOException, InterruptedException {
        watchService.registerListener(listener, Path.of(""), false);

        Path testFile = rootPath.resolve(SUB_DIR_PATH_NAME).resolve(TEST_FILE_NANE);

        Files.writeString(testFile, "initial content", StandardCharsets.UTF_8);
        assertNoEvent();

        Files.writeString(testFile, "modified content", StandardCharsets.UTF_8);
        assertNoEvent();

        Files.writeString(testFile, "modified content", StandardCharsets.UTF_8);
        assertNoEvent();

        Files.delete(testFile);
        assertNoEvent();
    }

    @Test
    private void testNewSubDirAlsoWatched() throws IOException, InterruptedException {
        watchService.registerListener(listener, Path.of(""), false);

        Path subDirSubDir = Files.createDirectories(rootPath.resolve(SUB_DIR_PATH_NAME).resolve(SUB_DIR_PATH_NAME));
        assertNoEvent();

        Path testFile = subDirSubDir.resolve(TEST_FILE_NANE);
        Path relativeTestFilePath = testFile.relativize(rootPath);

        Files.writeString(testFile, "initial content", StandardCharsets.UTF_8);
        assertEvent(relativeTestFilePath, Kind.CREATE);

        Files.writeString(testFile, "modified content", StandardCharsets.UTF_8);
        assertEvent(relativeTestFilePath, Kind.MODIFY);

        Files.writeString(testFile, "modified content", StandardCharsets.UTF_8);
        assertNoEvent();

        Files.delete(testFile);
        assertEvent(relativeTestFilePath, Kind.DELETE);

        Files.delete(subDirSubDir);
        assertNoEvent();
    }

    private void assertNoEvent() throws InterruptedException {
        Thread.sleep(5000);

        assertThat(listener.events, empty());
    }

    private void assertEvent(Path path, Kind kind) throws InterruptedException {
        waitForAssert(() -> assertThat(listener.events, not(empty())));
        Thread.sleep(500);

        assertThat(listener.events, hasSize(1));
        assertThat(listener.events, hasItem(new Event(path, kind)));
        listener.events.clear();
    }

    private class TestWatchEventListener implements WatchService.WatchEventListener {
        List<Event> events = new CopyOnWriteArrayList<>();

        @Override
        public void processWatchEvent(Kind kind, Path path) {
            events.add(new Event(path, kind));
        }
    }

    record Event(Path path, Kind kind) {
    }
}
