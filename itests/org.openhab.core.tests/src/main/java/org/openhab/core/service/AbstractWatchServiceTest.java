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
package org.openhab.core.service;

import static java.nio.file.StandardWatchEventKinds.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openhab.core.test.java.JavaTest;

/**
 * Test for {@link AbstractWatchService}.
 *
 * @author Dimitar Ivanov - Initial contribution
 * @author Svilen Valkanov - Tests are modified to run on different Operating Systems
 * @author Ana Dimova - reduce to a single watch thread for all class instances
 * @author Simon Kaufmann - ported it from Groovy to Java
 */
public class AbstractWatchServiceTest extends JavaTest {

    private static final String WATCHED_DIRECTORY = "watchDirectory";

    // Fail if no event has been received within the given timeout
    private static int noEventTimeoutInSeconds;

    private RelativeWatchService watchService;

    @BeforeClass
    public static void setUpBeforeClass() {
        // set the NO_EVENT_TIMEOUT_IN_SECONDS according to the operating system used
        if (SystemUtils.IS_OS_MAC_OSX) {
            noEventTimeoutInSeconds = 15;
        } else {
            noEventTimeoutInSeconds = 3;
        }
    }

    @Before
    public void setup() {
        File watchDir = new File(WATCHED_DIRECTORY);
        watchDir.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        watchService.deactivate();
        clearWatchedDir();
        watchService.allFullEvents.clear();

        File watchedDirectory = new File(WATCHED_DIRECTORY);
        FileUtils.deleteDirectory(watchedDirectory);
    }

    private void clearWatchedDir() throws IOException {
        File watchedDirectory = new File(WATCHED_DIRECTORY);
        Stream.of(watchedDirectory.listFiles()).forEach(mockedFile -> {
            if (mockedFile.isFile()) {
                mockedFile.delete();
            } else {
                try {
                    FileUtils.deleteDirectory(mockedFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Test
    public void testInRoot() throws Exception {
        watchService = new RelativeWatchService(WATCHED_DIRECTORY, true);

        // File created in the watched directory
        assertByRelativePath("rootWatchFile");
    }

    @Test
    public void testInSub() throws Exception {
        watchService = new RelativeWatchService(WATCHED_DIRECTORY, true);

        // File created in a subdirectory of the watched directory
        assertByRelativePath("subDir" + File.separatorChar + "subDirWatchFile");
    }

    @Test
    public void testInSubSub() throws Exception {
        watchService = new RelativeWatchService(WATCHED_DIRECTORY, true);

        // File created in a sub sub directory of the watched directory
        assertByRelativePath("subDir" + File.separatorChar + "subSubDir" + File.separatorChar + "innerWatchFile");
    }

    @Test
    public void testIdenticalNames() throws Exception {
        watchService = new RelativeWatchService(WATCHED_DIRECTORY, true);

        String fileName = "duplicateFile";
        String innerFileName = "duplicateDir" + File.separatorChar + fileName;

        File innerfile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName);
        innerfile.getParentFile().mkdirs();

        // Activate the service when the subdir is also present. Else the subdir will not be registered
        watchService.activate();

        innerfile.createNewFile();

        // Assure that the ordering of the events will be always the same
        Thread.sleep(noEventTimeoutInSeconds * 1000);

        new File(WATCHED_DIRECTORY + File.separatorChar + fileName).createNewFile();

        assertEventCount(2);

        FullEvent innerFileEvent = watchService.allFullEvents.get(0);
        assertThat(innerFileEvent.eventKind, is(ENTRY_CREATE));
        assertThat(innerFileEvent.eventPath.toString(), is(WATCHED_DIRECTORY + File.separatorChar + innerFileName));

        FullEvent fileEvent = watchService.allFullEvents.get(1);
        assertThat(fileEvent.eventKind, is(ENTRY_CREATE));
        assertThat(fileEvent.eventPath.toString(), is(WATCHED_DIRECTORY + File.separatorChar + fileName));
    }

    @Test
    public void testExcludeSubdirs() throws Exception {
        // Do not watch the subdirectories of the root directory
        watchService = new RelativeWatchService(WATCHED_DIRECTORY, false);

        String innerFileName = "watchRequestSubDir" + File.separatorChar + "watchRequestInnerFile";

        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName);
        innerFile.getParentFile().mkdirs();

        watchService.activate();

        // Consequent creation and deletion in order to generate any watch events for the subdirectory
        innerFile.createNewFile();
        innerFile.delete();

        assertNoEventsAreProcessed();
    }

    @Test
    public void testIncludeSubdirs() throws Exception {
        // Do watch the subdirectories of the root directory
        watchService = new RelativeWatchService(WATCHED_DIRECTORY, true);

        String innerFileName = "watchRequestSubDir" + File.separatorChar + "watchRequestInnerFile";
        File innerFile = new File(WATCHED_DIRECTORY + File.separatorChar + innerFileName);
        // Make all the subdirectories before running the service
        innerFile.getParentFile().mkdirs();

        watchService.activate();

        innerFile.createNewFile();
        assertFileCreateEventIsProcessed(innerFile, innerFileName);

        watchService.allFullEvents.clear();
        assertNoEventsAreProcessed();
    }

    private void assertNoEventsAreProcessed() throws Exception {
        // Wait for a possible event for the maximum timeout
        Thread.sleep(noEventTimeoutInSeconds * 1000);

        assertEventCount(0);
    }

    private void assertFileCreateEventIsProcessed(File innerFile, String innerFileName) {
        // Single event for file creation is present
        assertEventCount(1);
        FullEvent fileEvent = watchService.allFullEvents.get(0);
        assertThat(fileEvent.eventKind, is(ENTRY_CREATE));
        assertThat(fileEvent.eventPath.toString(), is(WATCHED_DIRECTORY + File.separatorChar + innerFileName));
    }

    private void assertByRelativePath(String fileName) throws Exception {
        File file = new File(WATCHED_DIRECTORY + File.separatorChar + fileName);
        file.getParentFile().mkdirs();

        assertThat(file.exists(), is(false));

        // We have to be sure that all the subdirectories of the watched directory are created when the watched service
        // is activated
        watchService.activate();

        file.createNewFile();
        fullEventAssertionsByKind(fileName, ENTRY_CREATE, false);

        // File modified
        FileUtils.writeLines(file, Collections.singletonList("Additional content"), true);
        fullEventAssertionsByKind(fileName, ENTRY_MODIFY, false);

        // File modified but identical content
        FileUtils.writeLines(file, Collections.singletonList("Additional content"), false);
        assertNoEventsAreProcessed();

        // File deleted
        file.delete();
        fullEventAssertionsByKind(fileName, ENTRY_DELETE, true);
    }

    private void assertEventCount(int expected) {
        try {
            waitForAssert(() -> assertThat(watchService.allFullEvents.size(), is(expected)));
        } catch (AssertionError e) {
            watchService.allFullEvents.forEach(event -> event.toString());
            throw e;
        }
    }

    private void fullEventAssertionsByKind(String fileName, Kind<?> kind, boolean osSpecific) throws Exception {
        waitForAssert(() -> assertThat(watchService.allFullEvents.size() >= 1, is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);

        if (osSpecific && kind.equals(ENTRY_DELETE)) {
            // There is possibility that one more modify event is triggered on some OS
            // so sleep a bit extra time
            Thread.sleep(500);
            cleanUpOsSpecificModifyEvent();
        }

        assertEventCount(1);
        FullEvent fullEvent = watchService.allFullEvents.get(0);

        assertThat(fullEvent.eventPath.toString(), is(WATCHED_DIRECTORY + File.separatorChar + fileName));
        assertThat(fullEvent.eventKind, is(kind));
        assertThat(fullEvent.watchEvent.count() >= 1, is(true));
        assertThat(fullEvent.watchEvent.kind(), is(fullEvent.eventKind));
        String fileNameOnly = fileName.contains(File.separatorChar + "")
                ? fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1, fileName.length())
                : fileName;
        assertThat(fullEvent.watchEvent.context().toString(), is(fileNameOnly));

        // Clear all the asserted events
        watchService.allFullEvents.clear();
    }

    /**
     * Cleanup the OS specific ENTRY_MODIFY event as it will not be needed for the assertion
     */
    private void cleanUpOsSpecificModifyEvent() {
        // As the implementation of the watch events is OS specific, it can happen that when the file is deleted two
        // events are fired - ENTRY_MODIFY followed by an ENTRY_DELETE
        // This is usually observed on Windows and below is the workaround
        // Related discussion in StackOverflow:
        // http://stackoverflow.com/questions/28201283/watchservice-windows-7-when-deleting-a-file-it-fires-both-entry-modify-and-e
        boolean isDeletedWithPrecedingModify = watchService.allFullEvents.size() == 2
                && watchService.allFullEvents.get(0).eventKind.equals(ENTRY_MODIFY);
        if (isDeletedWithPrecedingModify) {
            // Remove the ENTRY_MODIFY element as it is not needed
            watchService.allFullEvents.remove(0);
        }
    }

    private static class RelativeWatchService extends AbstractWatchService {

        boolean watchSubDirs;

        // Synchronize list as several watcher threads can write into it
        public volatile List<FullEvent> allFullEvents = new CopyOnWriteArrayList<>();

        RelativeWatchService(String rootPath, boolean watchSubDirectories) {
            super(rootPath);
            watchSubDirs = watchSubDirectories;
        }

        @Override
        protected void processWatchEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
            FullEvent fullEvent = new FullEvent(event, kind, path);
            allFullEvents.add(fullEvent);
        }

        @Override
        protected Kind<?>[] getWatchEventKinds(Path subDir) {
            return new Kind[] { ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY };
        }

        @Override
        protected boolean watchSubDirectories() {
            return watchSubDirs;
        }

    }

    private static class FullEvent {
        WatchEvent<?> watchEvent;
        Kind<?> eventKind;
        Path eventPath;

        public FullEvent(WatchEvent<?> event, Kind<?> kind, Path path) {
            watchEvent = event;
            eventKind = kind;
            eventPath = path;
        }

        @Override
        public String toString() {
            return "Watch Event: count " + watchEvent.count() + "; kind: " + eventKind + "; path: " + eventPath;
        }
    }
}
