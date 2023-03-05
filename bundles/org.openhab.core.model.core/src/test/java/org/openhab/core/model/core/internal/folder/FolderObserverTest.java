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
package org.openhab.core.model.core.internal.folder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.openhab.core.service.WatchService.Kind.CREATE;
import static org.openhab.core.service.WatchService.Kind.MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.model.core.ModelParser;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.WatchService;
import org.openhab.core.test.java.JavaTest;
import org.osgi.service.component.ComponentContext;

/**
 * A test class for {@link FolderObserver} class. The following test cases aim
 * to check if {@link FolderObserver} invokes the correct {@link ModelRepository}'s methods
 * with correct arguments when certain events in the watched directory are triggered.
 *
 * @author Mihaela Memova - Initial contribution
 * @author Stefan Triller - added hidden file test
 * @author Simon Kaufmann - ported to Java
 * @author Jan N. Klug - Refactored to UnitTest
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class FolderObserverTest extends JavaTest {
    private static final boolean IS_OS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
    private static final File WATCHED_DIRECTORY = new File("watcheddir");
    private static final File UNWATCHED_DIRECTORY = new File("unwatcheddir");
    private static final String EXISTING_SUBDIR_NAME = "existingsubdir";
    private static final File EXISTING_SUBDIR_PATH = new File(WATCHED_DIRECTORY, EXISTING_SUBDIR_NAME);
    private static final String MOCK_MODEL_TO_BE_REMOVED = "MockFileInModelForDeletion.java";

    private static final String INITIAL_FILE_CONTENT = "Initial content";

    private @NonNullByDefault({}) Dictionary<String, Object> configProps;
    private @NonNullByDefault({}) FolderObserver folderObserver;

    private @Mock @NonNullByDefault({}) ModelRepository modelRepoMock;
    private @Mock @NonNullByDefault({}) ModelParser modelParserMock;
    private @Mock @NonNullByDefault({}) ReadyService readyServiceMock;
    private @Mock @NonNullByDefault({}) WatchService watchServiceMock;
    private @Mock @NonNullByDefault({}) ComponentContext contextMock;

    @BeforeEach
    public void beforeEach() throws IOException, InterruptedException {
        configProps = new Hashtable<>();

        WATCHED_DIRECTORY.mkdirs();
        EXISTING_SUBDIR_PATH.mkdirs();

        when(modelParserMock.getExtension()).thenReturn("java");
        when(contextMock.getProperties()).thenReturn(configProps);
        when(watchServiceMock.getWatchPath()).thenReturn(WATCHED_DIRECTORY.toPath());

        folderObserver = new FolderObserver(modelRepoMock, readyServiceMock, watchServiceMock);
        folderObserver.addModelParser(modelParserMock);
    }

    /**
     * The FolderObserver service have to be stopped at the end of each test
     * as most of the tests are covering assertions on its initialization actions
     *
     * @throws Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        folderObserver.deactivate();

        try (Stream<Path> walk = Files.walk(WATCHED_DIRECTORY.toPath())) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    /**
     * The following method creates a file in an existing directory. The file's extension is
     * in the configuration properties and there is a registered ModelParser for it.
     * addOrRefreshModel() method invocation is expected
     *
     * @throws Exception
     */
    @Test
    public void testCreation() throws Exception {
        String validExtension = "java";

        configProps.put(EXISTING_SUBDIR_NAME, "txt,jpg," + validExtension);
        folderObserver.activate(contextMock);

        File file = new File(EXISTING_SUBDIR_PATH, "NewlyCreatedMockFile." + validExtension);
        Files.writeString(file.toPath(), INITIAL_FILE_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        waitForAssert(() -> assertThat(file.exists(), is(true)));
        folderObserver.processWatchEvent(CREATE, WATCHED_DIRECTORY.toPath().relativize(file.toPath()));

        verify(modelRepoMock).addOrRefreshModel(eq(file.getName()), any());
        verifyNoMoreInteractions(modelRepoMock);
    }

    /**
     * The following method creates a file in an existing directory. The file's extension is
     * in the configuration properties and there is a registered ModelParser for it.
     * Then the file's content is changed.
     * addOrRefreshModel() method invocation is expected
     *
     * @throws Exception
     */
    @Test
    public void testModification() throws Exception {
        String validExtension = "java";

        configProps.put(EXISTING_SUBDIR_NAME, "txt,jpg," + validExtension);
        folderObserver.activate(contextMock);

        File file = new File(EXISTING_SUBDIR_PATH, "MockFileForModification." + validExtension);
        Files.writeString(file.toPath(), INITIAL_FILE_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE);

        waitForAssert(() -> assertThat(file.exists(), is(true)));
        folderObserver.processWatchEvent(CREATE, WATCHED_DIRECTORY.toPath().relativize(file.toPath()));

        String text = "Additional content";
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        folderObserver.processWatchEvent(MODIFY, WATCHED_DIRECTORY.toPath().relativize(file.toPath()));

        verify(modelRepoMock, times(2)).addOrRefreshModel(eq(file.getName()), any());
        verifyNoMoreInteractions(modelRepoMock);
    }

    /**
     * The following method creates a file in an existing directory. The file's extension is
     * in the configuration properties but there is no parser for it.
     * No ModelRepository's method invocation is expected
     *
     * @throws Exception
     */
    @Test
    public void testCreationUntrackedExtension() throws Exception {
        String noParserExtension = "jpg";

        configProps.put(EXISTING_SUBDIR_NAME, "java,txt," + noParserExtension);
        folderObserver.activate(contextMock);

        File file = new File(EXISTING_SUBDIR_PATH, "NewlyCreatedMockFile." + noParserExtension);
        Files.writeString(file.toPath(), INITIAL_FILE_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        waitForAssert(() -> assertThat(file.exists(), is(true)));

        folderObserver.processWatchEvent(CREATE, WATCHED_DIRECTORY.toPath().relativize(file.toPath()));

        verifyNoInteractions(modelRepoMock);
    }

    /**
     * The following method creates a file in an existing directory. The file's extension is not
     * in the configuration properties but there is a parser for it.
     * No ModelRepository's method invocation is expected
     *
     * @throws Exception
     */
    @Test
    public void testCreationUntrackedDirectory() throws Exception {
        configProps.put(EXISTING_SUBDIR_NAME, "txt,jpg");
        folderObserver.activate(contextMock);

        File file = new File(EXISTING_SUBDIR_PATH, "NewlyCreatedMockFile.java");
        Files.writeString(file.toPath(), INITIAL_FILE_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        waitForAssert(() -> assertThat(file.exists(), is(true)));
        folderObserver.processWatchEvent(CREATE, WATCHED_DIRECTORY.toPath().relativize(file.toPath()));

        verifyNoInteractions(modelRepoMock);
    }

    /**
     * The following method tests the correct invocation of removeModel() method when a
     * folder-extensions pair is deleted from the configuration properties
     * and folderFileExtMap becomes empty
     */
    @Test
    public void testShutdown() {
        when(modelRepoMock.getAllModelNamesOfType(EXISTING_SUBDIR_NAME)).thenReturn(List.of(MOCK_MODEL_TO_BE_REMOVED));
        configProps.put(EXISTING_SUBDIR_NAME, "java,txt,jpg");
        folderObserver.activate(contextMock);

        folderObserver.deactivate();
        configProps.remove(EXISTING_SUBDIR_NAME);
        folderObserver.activate(contextMock);

        verify(modelRepoMock).removeModel(MOCK_MODEL_TO_BE_REMOVED);
    }

    /**
     * The following method test the configuration with a non existing subdirectory.
     */
    @Test
    public void testNonExisting() throws Exception {
        configProps.put("nonExistingSubdir", "txt,jpg,java");
        folderObserver.activate(contextMock);

        verifyNoInteractions(modelRepoMock);
    }

    /**
     * The following method creates a file in an existing directory
     * which has no valid extensions declared.
     * No ModelRepository's method invocation is expected
     *
     * @throws Exception
     */
    @Test
    public void testCreationNoExtensions() throws Exception {
        String subdir = "noExtensionsSubdir";
        new File(WATCHED_DIRECTORY, subdir).mkdirs();

        configProps.put(subdir, "");
        folderObserver.activate(contextMock);

        File file = new File(WATCHED_DIRECTORY, Paths.get(subdir, "MockFileInNoExtSubDir.txt").toString());
        Files.writeString(file.toPath(), INITIAL_FILE_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        waitForAssert(() -> assertThat(file.exists(), is(true)));

        folderObserver.processWatchEvent(CREATE, WATCHED_DIRECTORY.toPath().relativize(file.toPath()));

        verifyNoInteractions(modelRepoMock);
    }

    @Test
    public void testException() throws Exception {
        when(modelRepoMock.addOrRefreshModel(any(), any())).thenThrow(new IllegalStateException("intentional failure"));

        FolderObserver localFolderObserver = new FolderObserver(modelRepoMock, readyServiceMock, watchServiceMock);
        localFolderObserver.addModelParser(modelParserMock);

        String validExtension = "java";
        configProps.put(EXISTING_SUBDIR_NAME, "txt,jpg," + validExtension);
        localFolderObserver.activate(contextMock);

        File mockFileWithValidExt = new File(EXISTING_SUBDIR_PATH, "MockFileForModification." + validExtension);
        Files.writeString(mockFileWithValidExt.toPath(), INITIAL_FILE_CONTENT, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);
        localFolderObserver.processWatchEvent(CREATE,
                WATCHED_DIRECTORY.toPath().relativize(mockFileWithValidExt.toPath()));

        Files.writeString(mockFileWithValidExt.toPath(), "Additional content", StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);
        localFolderObserver.processWatchEvent(MODIFY,
                WATCHED_DIRECTORY.toPath().relativize(mockFileWithValidExt.toPath()));

        verify(modelRepoMock, times(2)).addOrRefreshModel(eq(mockFileWithValidExt.getName()), any());
    }

    /**
     * The following method creates a hidden file in an existing directory. The file's extension is
     * in the configuration properties and there is a registered ModelParser for it.
     * addOrRefreshModel() method invocation is NOT expected, the model should be ignored since the file is hidden
     *
     * @throws Exception
     */
    @Test
    public void testHiddenFile() throws Exception {
        String validExtension = "java";

        configProps.put(EXISTING_SUBDIR_NAME, "txt,jpg," + validExtension);
        folderObserver.activate(contextMock);

        String filename = ".HiddenNewlyCreatedMockFile." + validExtension;

        if (!IS_OS_WINDOWS) {
            File file = new File(EXISTING_SUBDIR_PATH, filename);
            Files.writeString(file.toPath(), INITIAL_FILE_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        } else {
            /*
             * In windows a hidden file cannot be created with a single api call.
             * The file must be created and afterwards it needs a filesystem property set for a file to be hidden.
             * But the initial creation already triggers the folder observer mechanism,
             * therefore the file is created in an unobserved directory, hidden and afterwards moved to the observed
             * directory
             */
            UNWATCHED_DIRECTORY.mkdirs();
            File file = new File(UNWATCHED_DIRECTORY, filename);
            file.createNewFile();
            Files.setAttribute(file.toPath(), "dos:hidden", true);
            try {
                Files.move(file.toPath(), EXISTING_SUBDIR_PATH.toPath());
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
            }
            try (Stream<Path> walk = Files.walk(UNWATCHED_DIRECTORY.toPath())) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
        folderObserver.processWatchEvent(CREATE, Path.of(EXISTING_SUBDIR_PATH.getName(), filename));
        verifyNoInteractions(modelRepoMock);
    }
}
