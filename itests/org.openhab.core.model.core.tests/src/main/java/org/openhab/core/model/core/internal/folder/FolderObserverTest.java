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
package org.openhab.core.model.core.internal.folder;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.emf.ecore.EObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.config.core.ConfigConstants;
import org.openhab.core.model.core.ModelParser;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.core.ModelRepositoryChangeListener;
import org.openhab.core.service.AbstractWatchService;
import org.openhab.core.test.java.JavaOSGiTest;
import org.osgi.service.component.ComponentContext;

/**
 * A test class for {@link FolderObserver} class. The following test cases aim
 * to check if {@link FolderObserver} invokes the correct {@link ModelRepository}'s methods
 * with correct arguments when certain events in the watched directory are triggered.
 *
 * {@link AbstractWatchService#initializeWatchService} method is called in the
 * {@link FolderObserver#updated} method and initializing a new WatchService
 * is related to creating a new {@link AbstractWatchQueueReader} which starts in a new Thread.
 * Since we rely on the {@link AbstractWatchQueueReader} to "listen" for changes, we have to
 * be sure that it has been started.
 * Based on that putting the current Thread to sleep after each invocation of
 * {@link FolderObserver#updated} method is necessary.
 * On the other hand, creating, modifying and deleting files and folders causes invocation
 * of {@link AbstractWatchQueueReader#processWatchEvent} method. That method is called asynchronously
 * and we do not know exactly when the event will be handled (it is OS specific).
 * Since the assertions in the tests depend on handling the events,
 * putting the current Thread to sleep after the file operations is also necessary.
 *
 * @author Mihaela Memova - Initial contribution
 * @author Stefan Triller - added hidden file test
 * @author Simon Kaufmann - ported to Java
 */
public class FolderObserverTest extends JavaOSGiTest {

    private static final File WATCHED_DIRECTORY = new File("watcheddir");
    private static final File UNWATCHED_DIRECTORY = new File("unwatcheddir");
    private static final String EXISTING_SUBDIR_NAME = "existingsubdir";
    private static final File EXISTING_SUBDIR_PATH = new File(WATCHED_DIRECTORY, EXISTING_SUBDIR_NAME);

    private static final int WAIT_EVENT_TO_BE_HANDLED = 500;

    private static final String MOCK_MODEL_TO_BE_REMOVED = "MockFileInModelForDeletion.java";

    private static final String INITIAL_FILE_CONTENT = "Initial content";

    private FolderObserver folderObserver;
    private ModelRepoDummy modelRepo;

    @Mock
    private ModelParser modelParser;

    @Mock
    private ComponentContext context;
    private Dictionary<String, Object> configProps;

    private String defaultWatchedDir;

    @Before
    public void setUp() {
        initMocks(this);

        configProps = new Hashtable<>();

        setupWatchedDirectory();
        setUpServices();
    }

    /**
     * The main configuration folder's path is saved in the defaultWatchedDir variable
     * in order to be restored after all the tests are finished.
     * For the purpose of the FolderObserverTest class a new folder is created.
     * Its path is set to the ConfigConstants.CONFIG_DIR_PROG_ARGUMENT property.
     */
    private void setupWatchedDirectory() {
        defaultWatchedDir = System.getProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT);
        WATCHED_DIRECTORY.mkdirs();
        System.setProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT, WATCHED_DIRECTORY.getPath());
        EXISTING_SUBDIR_PATH.mkdirs();
    }

    private void setUpServices() {
        when(modelParser.getExtension()).thenReturn("java");
        when(context.getProperties()).thenReturn(configProps);

        modelRepo = new ModelRepoDummy();

        folderObserver = new FolderObserver();
        folderObserver.setModelRepository(modelRepo);
        folderObserver.addModelParser(modelParser);
    }

    /**
     * The FolderObserver service have to be stopped at the end of each test
     * as most of the tests are covering assertions on its initialization actions
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        folderObserver.deactivate();
        FileUtils.deleteDirectory(WATCHED_DIRECTORY);
        modelRepo.clean();
        if (defaultWatchedDir != null) {
            System.setProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT, defaultWatchedDir);
        } else {
            System.clearProperty(ConfigConstants.CONFIG_DIR_PROG_ARGUMENT);
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
        folderObserver.activate(context);

        File file = new File(EXISTING_SUBDIR_PATH, "NewlyCreatedMockFile." + validExtension);
        file.createNewFile();

        /*
         * In some OS, like MacOS, creating an empty file is not related to sending an ENTRY_CREATE event.
         * So, it's necessary to put some initial content in that file.
         */
        if (!SystemUtils.IS_OS_WINDOWS) {
            FileUtils.writeStringToFile(file, INITIAL_FILE_CONTENT);
        }

        waitForAssert(() -> assertThat(file.exists(), is(true)));
        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);
        waitForAssert(() -> assertThat(modelRepo.isRemoveModelMethodCalled, is(false)));
        waitForAssert(() -> assertThat(modelRepo.calledFileName, is(file.getName())));
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
        folderObserver.activate(context);

        File file = new File(EXISTING_SUBDIR_PATH, "MockFileForModification." + validExtension);
        file.createNewFile();

        /*
         * In some OS, like MacOS, creating an empty file is not related to sending an ENTRY_CREATE event. So, it's
         * necessary to put some initial content in that file.
         */
        if (!SystemUtils.IS_OS_WINDOWS) {
            FileUtils.writeStringToFile(file, INITIAL_FILE_CONTENT, true);
        }

        waitForAssert(() -> assertThat(file.exists(), is(true)));
        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);

        modelRepo.clean();

        String text = "Additional content";
        FileUtils.writeStringToFile(file, text, true);

        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);
        waitForAssert(() -> assertThat(modelRepo.calledFileName, is(file.getName())));

        String finalFileContent;
        if (!SystemUtils.IS_OS_WINDOWS) {
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
        folderObserver.activate(context);

        File file = new File(EXISTING_SUBDIR_PATH, "NewlyCreatedMockFile." + noParserExtension);
        file.createNewFile();

        sleep(WAIT_EVENT_TO_BE_HANDLED);
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
        folderObserver.activate(context);

        File file = new File(EXISTING_SUBDIR_PATH, "NewlyCreatedMockFile.java");
        file.createNewFile();

        sleep(WAIT_EVENT_TO_BE_HANDLED);
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
        folderObserver.activate(context);

        modelRepo.clean();

        folderObserver.deactivate();
        configProps.remove(EXISTING_SUBDIR_NAME);
        folderObserver.activate(context);

        waitForAssert(() -> assertThat(modelRepo.isRemoveModelMethodCalled, is(true)));
        waitForAssert(() -> assertThat(modelRepo.calledFileName, is(MOCK_MODEL_TO_BE_REMOVED)));
    }

    /**
     * The following method test the configuration with a non existing subdirectory.
     */
    @Test
    public void testNonExisting() {
        configProps.put("nonExistingSubdir", "txt,jpg,java");
        folderObserver.activate(context);

        sleep(WAIT_EVENT_TO_BE_HANDLED);
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
        folderObserver.activate(context);

        File file = new File(WATCHED_DIRECTORY, Paths.get(subdir, "MockFileInNoExtSubDir.txt").toString());
        file.createNewFile();

        sleep(WAIT_EVENT_TO_BE_HANDLED);
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
                throw new RuntimeException("intentional failure.");
            }
        };
        folderObserver.setModelRepository(modelRepo);

        String validExtension = "java";
        configProps.put(EXISTING_SUBDIR_NAME, "txt,jpg," + validExtension);
        folderObserver.activate(context);

        File mockFileWithValidExt = new File(EXISTING_SUBDIR_PATH, "MockFileForModification." + validExtension);
        mockFileWithValidExt.createNewFile();
        if (!SystemUtils.IS_OS_WINDOWS) {
            FileUtils.writeStringToFile(mockFileWithValidExt, INITIAL_FILE_CONTENT);
        }

        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(true)), DFL_TIMEOUT * 2,
                DFL_SLEEP_TIME);

        modelRepo.clean();
        FileUtils.writeStringToFile(mockFileWithValidExt, "Additional content", true);

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
        folderObserver.activate(context);

        String filename = ".HiddenNewlyCreatedMockFile." + validExtension;

        if (!SystemUtils.IS_OS_WINDOWS) {
            /*
             * In some OS, like MacOS, creating an empty file is not related to sending an ENTRY_CREATE event.
             * So, it's necessary to put some initial content in that file.
             */
            File file = new File(EXISTING_SUBDIR_PATH, filename);
            file.createNewFile();
            FileUtils.writeStringToFile(file, INITIAL_FILE_CONTENT);
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
            FileUtils.moveFileToDirectory(file, EXISTING_SUBDIR_PATH, false);
            FileUtils.deleteDirectory(UNWATCHED_DIRECTORY);
        }

        sleep(WAIT_EVENT_TO_BE_HANDLED);
        waitForAssert(() -> assertThat(modelRepo.isAddOrRefreshModelMethodCalled, is(false)));
    }

    private static class ModelRepoDummy implements ModelRepository {

        public boolean isAddOrRefreshModelMethodCalled = false;
        public boolean isRemoveModelMethodCalled = false;
        public String calledFileName;

        public String fileContent;

        @Override
        public boolean addOrRefreshModel(String name, InputStream inputStream) {
            calledFileName = name;
            isAddOrRefreshModelMethodCalled = true;
            try {
                fileContent = IOUtils.toString(inputStream);
                inputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
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
            List<String> arrayOfModelsToBeRemoved = Collections.singletonList(MOCK_MODEL_TO_BE_REMOVED);
            return arrayOfModelsToBeRemoved;
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
        public EObject getModel(String name) {
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
            return null;
        }
    }

}
