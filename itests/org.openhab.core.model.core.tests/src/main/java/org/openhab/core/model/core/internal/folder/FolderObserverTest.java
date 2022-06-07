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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.model.core.ModelParser;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.WatchService;
import org.openhab.core.service.WatchServiceFactory;
import org.openhab.core.test.java.JavaOSGiTest;
import org.osgi.service.component.ComponentContext;
import org.slf4j.LoggerFactory;

/**
 * A test class for {@link FolderObserver} class. The following test cases aim
 * to check if {@link FolderObserver} invokes the correct {@link ModelRepository}'s methods
 * with correct arguments when certain events in the watched directory are triggered.
 *
 * Since we rely on the {@link WatchService} to "listen" for changes, we have to
 * be sure that it has been started.
 * Based on that putting the current Thread to sleep after each invocation of
 * {@link FolderObserver#activate} method is necessary.
 * On the other hand, creating, modifying and deleting files and folders causes invocation
 * of {@link WatchService.WatchEventListener#processWatchEvent} method. That method is called asynchronously
 * and we do not know exactly when the event will be handled (it is OS specific).
 * Since the assertions in the tests depend on handling the events,
 * putting the current Thread to sleep after the file operations is also necessary.
 *
 * @author Mihaela Memova - Initial contribution
 * @author Stefan Triller - added hidden file test
 * @author Simon Kaufmann - ported to Java
 */
@ExtendWith(MockitoExtension.class)
@NonNullByDefault
public class FolderObserverTest extends JavaOSGiTest {

    private static final boolean IS_OS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
    private static final File WATCHED_DIRECTORY = new File("watcheddir");
    private static final File UNWATCHED_DIRECTORY = new File("unwatcheddir");
    private static final String EXISTING_SUBDIR_NAME = "existingsubdir";
    private static final File EXISTING_SUBDIR_PATH = new File(WATCHED_DIRECTORY, EXISTING_SUBDIR_NAME);

    private static final int WAIT_EVENT_TO_BE_HANDLED = 500;

    private static final String MOCK_MODEL_TO_BE_REMOVED = "MockFileInModelForDeletion.java";

    private static final String INITIAL_FILE_CONTENT = "Initial content";

    private static final String WATCH_SERVICE_NAME = "testWatcher";

    private @NonNullByDefault({}) Dictionary<String, Object> configProps;
    private @NonNullByDefault({}) String defaultWatchedDir;
    private @NonNullByDefault({}) FolderObserver folderObserver;
    private @NonNullByDefault({}) WatchServiceFactory watchServiceFactory;
    private @NonNullByDefault({}) WatchService watchService;

    private @NonNullByDefault({}) ModelRepoDummy modelRepo;

    private @Mock @NonNullByDefault({}) ModelParser modelParserMock;
    private @Mock @NonNullByDefault({}) ReadyService readyServiceMock;
    private @Mock @NonNullByDefault({}) ComponentContext contextMock;

    @BeforeEach
    public void beforeEach() throws IOException, InterruptedException {
        configProps = new Hashtable<>();

        setupWatchedDirectory();
        setUpServices();
    }

    /**
     * The main configuration folder's path is saved in the defaultWatchedDir variable
     * in order to be restored after all the tests are finished.
     * For the purpose of the FolderObserverTest class a new folder is created.
     * Its path is set to the OpenHAB.CONFIG_DIR_PROG_ARGUMENT property.
     */
    private void setupWatchedDirectory() {
        WATCHED_DIRECTORY.mkdirs();
        EXISTING_SUBDIR_PATH.mkdirs();
    }

    private void setUpServices() throws IOException, InterruptedException {
        when(modelParserMock.getExtension()).thenReturn("java");
        when(contextMock.getProperties()).thenReturn(configProps);

        watchServiceFactory = getService(WatchServiceFactory.class);
        watchServiceFactory.createWatchService(WATCH_SERVICE_NAME, WATCHED_DIRECTORY.toPath().toAbsolutePath());

        waitForAssert(() -> {
            List<WatchService> watchServices = getServices(WatchService.class, i -> true);
            LoggerFactory.getLogger(FolderObserverTest.class).error("{}", watchServices);
            watchService = getService(WatchService.class,
                    s -> WATCH_SERVICE_NAME.equals(s.getProperty(WatchService.SERVICE_PROPERTY_NAME)));
            assertThat(watchService, is(notNullValue()));
        });

        modelRepo = new ModelRepoDummy();

        folderObserver = new FolderObserver(modelRepo, readyServiceMock, watchService) {
            @Override
            protected File getFile(String filename) {
                return new File(WATCHED_DIRECTORY + File.separator + filename);
            }
        };
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
        watchServiceFactory.removeWatchService(WATCH_SERVICE_NAME);

        try (Stream<Path> walk = Files.walk(WATCHED_DIRECTORY.toPath())) {
            walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }

        modelRepo.clean();
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
        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);
        waitForAssert(() -> assertThat(modelRepo.isRemoveModelMethodCalled, is(false)));
        waitForAssert(() -> assertThat(modelRepo.calledFileName, is(file.getName())));
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
        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);

        modelRepo.clean();

        String text = "Additional content";
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);
        waitForAssert(() -> assertThat(modelRepo.calledFileName, is(file.getName())));

        String finalFileContent;
        if (!IS_OS_WINDOWS) {
            finalFileContent = INITIAL_FILE_CONTENT + text;
        } else {
            finalFileContent = text;
        }

        waitForAssert(() -> assertThat(modelRepo.fileContent, is(finalFileContent)));
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

        Thread.sleep(WAIT_EVENT_TO_BE_HANDLED);
        waitForAssert(() -> assertThat(file.exists(), is(true)));
        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(false)));
        waitForAssert(() -> assertThat(modelRepo.isRemoveModelMethodCalled, is(false)));
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

        Thread.sleep(WAIT_EVENT_TO_BE_HANDLED);
        waitForAssert(() -> assertThat(file.exists(), is(true)));
        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(false)));
        waitForAssert(() -> assertThat(modelRepo.isRemoveModelMethodCalled, is(false)));
    }

    /**
     * The following method tests the correct invocation of removeModel() method when a
     * folder-extensions pair is deleted from the configuration properties
     * and folderFileExtMap becomes empty
     */
    @Test
    public void testShutdown() {
        configProps.put(EXISTING_SUBDIR_NAME, "java,txt,jpg");
        folderObserver.activate(contextMock);

        modelRepo.clean();

        folderObserver.deactivate();
        configProps.remove(EXISTING_SUBDIR_NAME);
        folderObserver.activate(contextMock);

        waitForAssert(() -> assertThat(modelRepo.isRemoveModelMethodCalled, is(true)));
        waitForAssert(() -> assertThat(modelRepo.calledFileName, is(MOCK_MODEL_TO_BE_REMOVED)));
    }

    /**
     * The following method test the configuration with a non existing subdirectory.
     */
    @Test
    public void testNonExisting() throws Exception {
        configProps.put("nonExistingSubdir", "txt,jpg,java");
        folderObserver.activate(contextMock);

        Thread.sleep(WAIT_EVENT_TO_BE_HANDLED);
        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(false)));
        waitForAssert(() -> assertThat(modelRepo.isRemoveModelMethodCalled, is(false)));
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

        Thread.sleep(WAIT_EVENT_TO_BE_HANDLED);
        waitForAssert(() -> assertThat(file.exists(), is(true)));
        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(false)));
        waitForAssert(() -> assertThat(modelRepo.isRemoveModelMethodCalled, is(false)));
    }

    @Test
    public void testException() throws Exception {
        ModelRepoDummy modelRepo = new ModelRepoDummy() {
            @Override
            public boolean addOrRefreshModel(String name, InputStream inputStream) {
                super.addOrRefreshModel(name, inputStream);
                throw new IllegalStateException("intentional failure.");
            }
        };
        FolderObserver localFolderObserver = new FolderObserver(modelRepo, readyServiceMock, watchService) {
            @Override
            protected File getFile(String filename) {
                return new File(WATCHED_DIRECTORY + File.separator + filename);
            }
        };

        localFolderObserver.addModelParser(modelParserMock);

        String validExtension = "java";
        configProps.put(EXISTING_SUBDIR_NAME, "txt,jpg," + validExtension);
        localFolderObserver.activate(contextMock);

        File mockFileWithValidExt = new File(EXISTING_SUBDIR_PATH, "MockFileForModification." + validExtension);
        Files.writeString(mockFileWithValidExt.toPath(), INITIAL_FILE_CONTENT, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE);

        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);

        modelRepo.clean();
        Files.writeString(mockFileWithValidExt.toPath(), "Additional content", StandardCharsets.UTF_8,
                StandardOpenOption.APPEND);

        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);
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
            /*
             * In some OS, like MacOS, creating an empty file is not related to sending an ENTRY_CREATE event.
             * So, it's necessary to put some initial content in that file.
             */
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
            } catch (java.nio.file.FileAlreadyExistsException e) {
            }
            try (Stream<Path> walk = Files.walk(UNWATCHED_DIRECTORY.toPath())) {
                walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }

        Thread.sleep(WAIT_EVENT_TO_BE_HANDLED);
        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(false)));
    }

    private static class ModelRepoDummy implements ModelRepository {

        public boolean isAddOrRefreshModelMethodCalled = false;
        public boolean isRemoveModelMethodCalled = false;
        public @Nullable String calledFileName;
        public @Nullable String fileContent;

        @Override
        public boolean addOrRefreshModel(String name, InputStream inputStream) {
            calledFileName = name;
            isAddOrRefreshModelMethodCalled = true;
            try {
                fileContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                inputStream.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return true;
        }

        @Override
        public boolean removeModel(String name) {
            calledFileName = name;
            isRemoveModelMethodCalled = true;
            return true;
        }

        /**
         * This method is invoked when a model is about to be deleted.
         * For the purposes of the FolderObserverTest class it is overridden and
         * it returns an array of exactly one model name.
         */
        @Override
        public Iterable<String> getAllModelNamesOfType(String modelType) {
            return List.of(MOCK_MODEL_TO_BE_REMOVED);
        }

        @Override
        public void reloadAllModelsOfType(String modelType) {
        }

        @Override
        public void addModelRepositoryChangeListener(ModelRepositoryChangeListener listener) {
        }

        @Override
        public void removeModelRepositoryChangeListener(ModelRepositoryChangeListener listener) {
        }

        @Override
        public @Nullable EObject getModel(String name) {
            return null;
        }

        public void clean() {
            isAddOrRefreshModelMethodCalled = false;
            isRemoveModelMethodCalled = false;
            calledFileName = null;
            fileContent = null;
        }

        @Override
        public Set<String> removeAllModelsOfType(String modelType) {
            return Set.of();
        }
    }
}
