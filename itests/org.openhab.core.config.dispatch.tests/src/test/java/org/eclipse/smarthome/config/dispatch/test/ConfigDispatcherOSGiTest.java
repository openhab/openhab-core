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
package org.eclipse.smarthome.config.dispatch.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.config.dispatch.internal.ConfigDispatcher;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ConfigDispatcherOSGiTest extends JavaOSGiTest {

    @Rule
    public TemporaryFolder tmpBaseFolder = new TemporaryFolder();

    private ConfigurationAdmin configAdmin;

    private ConfigDispatcher cd;

    private static final String CONFIGURATION_BASE_DIR = "configurations";
    private static final String SEP = File.separator;

    private static String defaultConfigFile;

    private Configuration configuration;
    private static String configBaseDirectory;

    @BeforeClass
    public static void setUpClass() {
        // Store the default values in order to restore them after all the tests are finished.
        defaultConfigFile = System.getProperty(ConfigDispatcher.SERVICECFG_PROG_ARGUMENT);
    }

    @Before
    public void setUp() throws Exception {
        configBaseDirectory = tmpBaseFolder.getRoot().getAbsolutePath();
        FileUtils.copyDirectory(new File(CONFIGURATION_BASE_DIR), new File(configBaseDirectory));

        configAdmin = getService(ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));

        cd = new ConfigDispatcher();
        cd.setConfigurationAdmin(configAdmin);
    }

    @After
    public void tearDown() throws Exception {
        // Clear the configuration with the current pid from the persistent store.
        if (configuration != null) {
            configuration.delete();
        }
    }

    @AfterClass
    public static void tearDownClass() {
        // Set the system properties to their initial values.
        setSystemProperty(ConfigDispatcher.SERVICECFG_PROG_ARGUMENT, defaultConfigFile);
    }

    @Test
    public void allConfigurationFilesWithLocalPIDsAreProcessedAndConfigurationIsUpdated() {
        String configDirectory = configBaseDirectory + SEP + "local_pid_conf";
        String servicesDirectory = "local_pid_services";

        String defaultConfigFileName = configDirectory + SEP + "local.pid.default.file.cfg";

        initialize(defaultConfigFileName);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        // Assert that a file with local pid from the root configuration directory is processed.
        verifyValueOfConfigurationProperty("local.default.pid", "default.property", "default.value");

        // Assert that a file with local pid from the services directory is processed.
        verifyValueOfConfigurationProperty("local.service.first.pid", "service.property", "service.value");

        // Assert that more than one file with local pid from the services directory is processed.
        verifyValueOfConfigurationProperty("local.service.second.pid", "service.property", "service.value");
    }

    private String getAbsoluteConfigDirectory(String configDirectory, String servicesDirectory) {
        return configDirectory + SEP + servicesDirectory;
    }

    @Test
    public void whenTheConfigurationFileNameContainsDotAndNoPIDIsDefinedTheFileNameBecomesPID() {
        String configDirectory = configBaseDirectory + SEP + "no_pid_conf";
        String servicesDirectory = "no_pid_services";
        // In this test case we need configuration files, which names contain dots.
        String defaultConfigFilePath = configDirectory + SEP + "no.pid.default.file.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /* Assert that the configuration is updated with the name of the file in the root directory as a pid. */
        verifyValueOfConfigurationProperty("no.pid.default.file", "default.property", "default.value");

        /* Assert that the configuration is updated with the name of the file in the services directory as a pid. */
        verifyValueOfConfigurationProperty("no.pid.service.file", "service.property", "service.value");
    }

    @Test
    public void IfNoPIDIsDefinedInConfigurationFileWithNo_dotNameTheDefaultNamespacePlusTheFileNameBecomesPID() {
        String configDirectory = configBaseDirectory + SEP + "no_pid_no_dot_conf";
        String servicesDirectory = "no_pid_no_dot_services";
        // In this test case we need configuration files, which names don't contain dots.
        String defaultConfigFilePath = configDirectory + SEP + "default.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the configuration is updated with the default
         * namespace the no-dot name of the file in the root directory as pid.
         */
        verifyValueOfConfigurationProperty(ConfigDispatcher.SERVICE_PID_NAMESPACE + ".default",
                "no.dot.default.property", "no.dot.default.value");

        /*
         * Assert that the configuration is updated with the default namespace
         * the no-dot name of the file in the services directory as pid.
         */
        verifyValueOfConfigurationProperty(ConfigDispatcher.SERVICE_PID_NAMESPACE + ".service",
                "no.dot.service.property", "no.dot.service.value");
    }

    @Test
    public void whenLocalPIDDoesNotContainDotTheDefaultNamespacePlusThatPIDIsTheOverallPID() {
        String configDirectory = configBaseDirectory + SEP + "local_pid_no_dot_conf";
        String servicesDirectory = "local_pid_no_dot_services";
        String defaultConfigFilePath = configDirectory + SEP + "local.pid.no.dot.default.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the configuration is updated with the default namespace
         * the no-dot pid from the file in the the root directory.
         */
        verifyValueOfConfigurationProperty(ConfigDispatcher.SERVICE_PID_NAMESPACE + ".default", "default.property",
                "default.value");

        /*
         * Assert that the configuration is updated with the default namespace
         * the no-dot pid from the file in the the services directory.
         */
        verifyValueOfConfigurationProperty(ConfigDispatcher.SERVICE_PID_NAMESPACE + ".service", "service.property",
                "service.value");
    }

    @Test
    public void whenLocalPIDIsAnEmptyStringTheDefaultNamespacePlusDotIsTheOverallPID() {
        // This test case is a corner case for the above test.
        String configDirectory = configBaseDirectory + SEP + "local_pid_empty_conf";
        String servicesDirectory = "local_pid_empty_services";
        String defaultConfigFilePath = configDirectory + SEP + "local.pid.empty.default.file.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        verifyValueOfConfigurationProperty(ConfigDispatcher.SERVICE_PID_NAMESPACE + ".", "default.property",
                "default.value");

        verifyValueOfConfigurationProperty(ConfigDispatcher.SERVICE_PID_NAMESPACE + ".", "service.property",
                "service.value");
    }

    @Test
    public void valueIsStillValidIfItIsLeftEmpty() {
        String configDirectory = configBaseDirectory + SEP + "local_pid_no_value_conf";
        String servicesDirectory = "local_pid_no_value_services";
        String defaultConfigFilePath = configDirectory + SEP + "local.pid.no.value.default.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the configuration is updated with
         * an empty value for a file in the root directory.
         */
        verifyValueOfConfigurationProperty("default.pid", "default.property", "");

        /*
         * Assert that the configuration is updated with
         * an empty value for a file in the services directory.
         */
        verifyValueOfConfigurationProperty("service.pid", "service.property", "");
    }

    @Test
    public void ifPropertyIsLeftEmptyAConfigurationWithTheGivenPIDWillNotExist() {
        String configDirectory = configBaseDirectory + SEP + "local_pid_no_property_conf";
        String servicesDirectory = "local_pid_no_property_services";
        String defaultConfigFilePath = configDirectory + SEP + "local.pid.no.property.default.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the configuration is not updated with an
         * empty-string property from the file in the root directory.
         */
        verifyNoPropertiesForConfiguration("no.property.default.pid");

        /*
         * Assert that the configuration is not updated with an
         * empty-string property from the file in the services directory.
         */
        verifyNoPropertiesForConfiguration("no.property.service.pid");
    }

    @Test
    public void propertiesForLocalPIDCanBeOverridden() {
        String configDirectory = configBaseDirectory + SEP + "local_pid_conflict_conf";
        String servicesDirectory = "local_pid_conflict_services";
        String defaultConfigFilePath = configDirectory + SEP + "local.pid.conflict.default.file.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the second processed property from the
         * file in the root directory overrides the first one.
         */
        verifyValueOfConfigurationProperty("same.default.local.pid", "default.property", "default.value2");

        /*
         * Assert that the second processed property from the file
         * in the services directory overrides the first one.
         */
        verifyValueOfConfigurationProperty("same.service.local.pid", "service.property", "service.value2");
    }

    @Test
    public void commentLinesAreNotProcessed() {
        String configDirectory = configBaseDirectory + SEP + "comments_conf";
        String servicesDirectory = "comments_services";
        String defaultConfigFilePath = configDirectory + SEP + "comments.default.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the configuration is not updated with a
         * pid from a comment from a file in the root directory.
         */
        verifyNoPropertiesForConfiguration("comment.default.pid");

        /*
         * Assert that the configuration is not updated with a
         * pid from a comment from a file in the services directory.
         */
        verifyNoPropertiesForConfiguration("comment.service.pid");
    }

    @Test
    public void txtFilesAreNotProcessed() {
        String configDirectory = configBaseDirectory + SEP + "txt_conf";
        String servicesDirectory = "txt_services";
        String defaultConfigFilePath = configDirectory + SEP + "txt.default.txt";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the configuration is not updated with
         * pid from a txt file in the root directory.
         */
        verifyNoPropertiesForConfiguration("txt.default.pid");

        /*
         * Assert that the configuration is not updated with
         * pid from a txt file in the services directory.
         */
        verifyNoPropertiesForConfiguration("txt.service.pid");
    }

    @Test
    public void allConfigurationFilesWithGlobalPIDsAreProcessedAndConfigurationIsUpdated() {
        String configDirectory = configBaseDirectory + SEP + "global_pid_conf";
        String servicesDirectory = "global_pid_services";
        String defaultConfigFilePath = configDirectory + SEP + "global.pid.default.file.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        // Assert that a file with global pid from the root configuration directory is processed.
        verifyValueOfConfigurationProperty("global.default.pid", "default.property", "default.value");

        // Assert that a file with global pid from the services directory is processed.
        verifyValueOfConfigurationProperty("global.service.first.pid", "service.property", "service.value");

        // Assert that more than one file with global pid from the services directory is processed.
        verifyValueOfConfigurationProperty("global.service.second.pid", "service.property", "service.value");
    }

    @Test
    public void ifThePropertyValuePairIsPrefixedWithLocalPIDInTheSameFileTheGlobalPIDIsIgnored() {
        String configDirectory = configBaseDirectory + SEP + "ignored_global_pid_conf";
        String servicesDirectory = "ignored_global_pid_services";
        String defaultConfigFilePath = configDirectory + SEP + "ignored.global.default.pid.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        // Assert that the configuration is not updated with the global pid in the root directory.
        verifyNoPropertiesForConfiguration("ignored.global.default.pid");

        // Assert that the configuration is updated with the local pid in the root directory.
        verifyValueOfConfigurationProperty("not.ignored.local.default.pid", "local.default.property",
                "local.default.value");

        // Assert that the configuration is not updated with the global pid in the services directory.
        verifyNoPropertiesForConfiguration("ignored.global.service.pid");

        // Assert that the configuration is updated with the local pid in the services directory.
        verifyValueOfConfigurationProperty("not.ignored.local.service.pid", "local.service.property",
                "local.service.value");
    }

    @Test
    public void ifThePropertyIsNotPrefixedWithLocalPIDTheLastPIDBecomesPIDForThatProperty() {
        String configDirectory = configBaseDirectory + SEP + "last_pid_conf";
        String servicesDirectory = "last_pid_services";
        String defaultConfigFilePath = configDirectory + SEP + "first.global.default.pid.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that a property=value pair is associated with the global pid,
         * defined in the above line in a file in the root directory
         * rather than with the local pid, defined in the below line.
         */
        verifyValueOfConfigurationProperty("first.global.default.pid", "global.default.property1",
                "global.default.value1");
        verifyNotExistingConfigurationProperty("last.local.default.pid", "global.default.property1");

        /*
         * Assert that a property=value pair is associated with the last defined local pid,
         * rather than with the global pid, defined in the first line of a file in the root directory.
         */
        verifyValueOfConfigurationProperty("last.local.default.pid", "global.default.property2",
                "global.default.value2");
        verifyNotExistingConfigurationProperty("first.global.default.pid", "global.default.property2");

        /*
         * Assert that a property=value pair is associated with the global pid,
         * defined in the above line in a file in the services directory
         * rather than with the local pid, defined in the below line.
         */
        verifyValueOfConfigurationProperty("first.global.service.pid", "global.service.property1",
                "global.service.value1");
        verifyNotExistingConfigurationProperty("last.local.default.pid", "global.service.property1");

        /*
         * Assert that a property=value pair is associated with the last defined local pid,
         * rather than with the global pid, defined in the first line of a file in the services directory.
         */
        verifyValueOfConfigurationProperty("last.local.service.pid", "global.service.property2",
                "global.service.value2");
        verifyNotExistingConfigurationProperty("first.global.service.pid", "global.service.property2");
    }

    @Test
    public void ifThereIsNoPropertyValuePairForGlobalPIDAConfigurationWithTheGivenPIDWillNotExist() {
        String configDirectory = configBaseDirectory + SEP + "global_pid_no_pair_conf";
        String servicesDirectory = "global_pid_no_pair_services";
        String defaultConfigFilePath = configDirectory + SEP + "global.pid.no.pair.default.file.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that pid with no property=value pair
         * is not processed in a file in the root directory.
         */
        verifyNoPropertiesForConfiguration("no.pair.default.pid");

        /*
         * Assert that pid with no property=value pair
         * is not processed in a file in the services directory.
         */
        verifyNoPropertiesForConfiguration("no.pair.service.pid");
    }

    @Test
    public void whenGlobalPIDIsEmptyStringItRemainsAnEmptyString() {
        String configDirectory = configBaseDirectory + SEP + "global_pid_empty_conf";
        String servicesDirectory = "global_pid_empty_services";

        initialize(null);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        // Assert that an empty-string pid from a file in the services directory is processed.
        verifyValueOfConfigurationProperty("", "global.service.property", "global.service.value");
    }

    @Test
    public void PropertyValuePairsForALocalPIDInDifferentFilesAreStillAssociatedWithThatPID() {
        String configDirectory = configBaseDirectory + SEP + "local_pid_different_files_no_conflict_conf";
        String servicesDirectory = "local_pid_different_files_no_conflict_services";
        String defaultConfigFilePath = configDirectory + SEP + "local.pid.default.file.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the configuration is updated with all the property=value
         * pairs for the same local pid from all the processed files.
         */
        verifyValueOfConfigurationProperty("local.pid", "first.property", "first.value");
        verifyValueOfConfigurationProperty("local.pid", "second.property", "second.value");
        verifyValueOfConfigurationProperty("local.pid", "third.property", "third.value");
        verifyValueOfConfigurationProperty("local.pid", "fourth.property", "fourth.value");
    }

    @Test
    public void propertiesForTheSameLocalPIDInDifferentFilesMustBeOverriddenInTheOrderTheyWereLastModified()
            throws Exception {
        String configDirectory = configBaseDirectory + SEP + "local_pid_different_files_conflict_conf";
        String servicesDirectory = "local_pid_different_files_conflict_services";
        String defaultConfigFilePath = configDirectory + SEP + "local.pid.default.file.cfg";
        String lastModifiedFileName = "last.modified.service.file.cfg";

        /*
         * Every file from servicesDirectory contains this property, but with different value.
         * The value for this property in the last processed file must override the previous
         * values for the same property in the configuration.
         */
        String conflictProperty = "property";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        // Modify this file, so that we are sure it is the last modified file in servicesDirectory.
        File fileToModify = new File(configDirectory + SEP + servicesDirectory + SEP + lastModifiedFileName);
        FileUtils.touch(fileToModify);
        cd.processConfigFile(fileToModify);

        String value = getLastModifiedValueForPoperty(getAbsoluteConfigDirectory(configDirectory, servicesDirectory),
                conflictProperty);

        /*
         * Assert that the property for the same local pid in the last modified file
         * has overridden the other properties for that pid from previously modified files.
         */
        verifyValueOfConfigurationProperty("local.conflict.pid", conflictProperty, value);
    }

    @Test
    public void whenPropertyValuePairsForAGlobalPIDAreInDifferentFilesPropertiesWillNotBeMerged() throws IOException {
        String configDirectory = configBaseDirectory + SEP + "global_pid_different_files_no_merge_conf";
        String servicesDirectory = "global_pid_different_files_no_merge_services";

        initialize(null);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        // Modify this file, so that we are sure it is the last modified file
        File lastModified = new File(configDirectory + SEP + servicesDirectory + SEP + "global.pid.service.c.file.cfg");
        FileUtils.touch(lastModified);
        cd.processConfigFile(lastModified);

        /*
         * Assert that the configuration is updated only with the property=value
         * pairs which are parsed last:
         */
        verifyNotExistingConfigurationProperty("different.files.global.pid", "first.property");
        verifyNotExistingConfigurationProperty("different.files.global.pid", "second.property");
        verifyValueOfConfigurationProperty("different.files.global.pid", "third.property", "third.value");
    }

    @Test
    public void whenLocalPIDIsDefinedForGlobalPIDFile_AbortParsing() {
        String configDirectory = configBaseDirectory + SEP + "global_pid_with_local_pid_line_error";
        String servicesDirectory = "global_pid_with_local_pid_line_services_error";

        initialize(null);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the configuration is updated only with the property=value
         * pairs which are parsed last:
         */
        verifyNotExistingConfiguration("global.service.pid");
    }

    @Test
    public void whenContextExistsInExlusivePIDCreateMultipleServices() throws IOException {
        String configDirectory = configBaseDirectory + SEP + "multiple_service_contexts";
        String servicesDirectory = "multiple_contexts";

        initialize(null);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        verifyValueOfConfigurationPropertyWithContext("service.pid#ctx1", "property1", "value1");
        verifyValueOfConfigurationPropertyWithContext("service.pid#ctx2", "property1", "value2");
    }

    @Test
    public void whenContextExistsInExlusivePIDCreateMultipleServicesUpdateWithDuplicate() throws IOException {
        String configDirectory = configBaseDirectory + SEP + "multiple_service_contexts_duplicates";
        String servicesDirectory = "multiple_contexts";

        initialize(null);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        File srcDupFile = new File(configDirectory + SEP + "duplicate" + SEP + "service-ctx1duplicate.cfg");

        cd.processConfigFile(srcDupFile);

        // only ctx1 is overwritten by service-ctx1duplicate.cfg
        verifyValueOfConfigurationPropertyWithContext("service.pid#ctx1", "property1", "valueDup");
        verifyValueOfConfigurationPropertyWithContext("service.pid#ctx2", "property1", "value2");
    }

    @Test
    public void whenContextExistsInExlusivePIDCreateMultipleServicesAndDeleteOneOfThem() throws IOException {
        String configDirectory = configBaseDirectory + SEP + "multiple_service_contexts";
        String servicesDirectory = "multiple_contexts";

        initialize(null);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        verifyValueOfConfigurationPropertyWithContext("service.pid#ctx1", "property1", "value1");
        verifyValueOfConfigurationPropertyWithContext("service.pid#ctx2", "property1", "value2");

        File serviceConfigFile = new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory),
                "service-ctx1.cfg");

        cd.fileRemoved(serviceConfigFile.getAbsolutePath());

        Configuration c1 = getConfigurationWithContext("service.pid#ctx1");
        assertThat(c1, is(nullValue()));

        Configuration c2 = getConfigurationWithContext("service.pid#ctx2");
        assertThat(c2.getProperties().get("property1"), is("value2"));
    }

    @Test
    public void whenExclusivePIDFileIsDeleted_DeleteTheConfiguration() throws IOException {
        String configDirectory = configBaseDirectory + SEP + "exclusive_pid_file_removed_during_runtime";
        String servicesDirectory = "exclusive_pid_file_removed_during_runtime_services";

        initialize(null);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        verifyValueOfConfigurationProperty("global.service.pid", "property1", "value1");

        String pid = "service.pid.cfg";
        File serviceConfigFile = new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory), pid);

        cd.fileRemoved(serviceConfigFile.getAbsolutePath());

        waitForAssert(() -> {
            try {
                configuration = configAdmin.getConfiguration(pid);
            } catch (IOException e) {
                throw new IllegalArgumentException("IOException occured while retrieving configuration for pid " + pid,
                        e);
            }
            assertThat(configuration.getProperties(), is(nullValue()));
        });
    }

    @Test
    public void whenExclusiveConfigIsTruncated_OverrideReducedConfig() throws IOException {
        String configDirectory = configBaseDirectory + SEP + "exclusive_pid_overrides_configuration_on_update";
        String servicesDirectory = "exclusive_pid_overrides_configuration_on_update_services";

        File serviceConfigFile = new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory),
                "service.pid.cfg");

        initialize(null);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the configuration is updated with all properties:
         */
        verifyValueOfConfigurationProperty("global.service.pid", "property1", "value1");
        verifyValueOfConfigurationProperty("global.service.pid", "property2", "value2");

        truncateLastLine(serviceConfigFile);

        cd.processConfigFile(serviceConfigFile);

        /*
         * Assert that the configuration is updated without the truncated property/value pair:
         */
        verifyValueOfConfigurationProperty("global.service.pid", "property1", "value1");
        verifyNotExistingConfigurationProperty("global.service.pid", "property2");
    }

    @Test
    public void whenExclusiveConfigFileIsDeleted_shouldRemoveConfigFromConfigAdmin() throws IOException {
        String configDirectory = configBaseDirectory + SEP + "exclusive_pid_configuration_removed_after_file_delete";
        String servicesDirectory = "exclusive_pid_configuration_removed_after_file_delete_services";

        File serviceConfigFile = new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory),
                "service.pid.cfg");

        initialize(null);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that the configuration is updated with all properties:
         */
        String pid = "global.service.pid";
        verifyValueOfConfigurationProperty(pid, "property1", "value1");
        verifyValueOfConfigurationProperty(pid, "property2", "value2");

        // remember the file content and delete the file:
        cd.fileRemoved(serviceConfigFile.getAbsolutePath());

        /*
         * Assert that the configuration was deleted from configAdmin
         */
        waitForAssert(() -> {
            try {
                configuration = configAdmin.getConfiguration(pid);
            } catch (IOException e) {
                throw new IllegalArgumentException("IOException occured while retrieving configuration for pid " + pid,
                        e);
            }
            assertThat(configuration.getProperties(), is(nullValue()));
        });
    }

    @Test
    public void propertiesForTheSameGlobalPIDInDifferentFilesMustBeOverriddenInTheOrderTheyWereLastModified()
            throws Exception {
        String configDirectory = configBaseDirectory + SEP + "global_pid_different_files_conflict_conf";
        String servicesDirectory = "global_pid_different_files_conflict_services";
        String defaultConfigFilePath = configDirectory + SEP + "global.pid.default.file.cfg";
        String lastModifiedFileName = "global.pid.last.modified.service.file.cfg";

        /*
         * Every file from servicesDirectory contains this property, but with different value.
         * The value for this property in the last processed file will override the previous
         * values for the same property in the configuration.
         */
        String conflictProperty = "property";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        // Modify this file, so that we are sure it is the last modified file in servicesDirectory.
        File fileToModify = new File(configDirectory + SEP + servicesDirectory + SEP + lastModifiedFileName);
        FileUtils.touch(fileToModify);
        cd.processConfigFile(fileToModify);

        String value = getLastModifiedValueForPoperty(getAbsoluteConfigDirectory(configDirectory, servicesDirectory),
                conflictProperty);

        /*
         * Assert that the property for the same global pid in the last modified file
         * has overridden the other properties for that pid from previously modified files.
         */
        verifyValueOfConfigurationProperty("different.files.global.pid", conflictProperty, value);
    }

    @Test
    public void whenExclusivePIDisDefinedInlineFromDifferentFile_skipTheLine() {
        String configDirectory = configBaseDirectory + SEP + "global_and_local_pid_different_files_conf";
        String servicesDirectory = "global_and_local_pid_no_conflict_services";
        String defaultConfigFilePath = configDirectory + SEP + "global.and.local.pid.default.file.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        // Assert that the configuration is updated with all the properties for a pid from all the processed files.
        verifyValueOfConfigurationProperty("no.conflict.global.and.local.pid", "exclusive.property", "global.value");
        verifyNotExistingConfigurationProperty("no.conflict.global.and.local.pid", "inline.property");
    }

    @Test
    public void localPIDIsOverriddenByGlobalPIDInDifferentFileIfTheFileWithTheGlobalOneIsModifiedLast()
            throws Exception {
        String configDirectory = configBaseDirectory + SEP + "global_and_local_pid_different_files_conf";
        String servicesDirectory = "global_and_local_pid_overridden_local_services";
        String defaultConfigFilePath = configDirectory + SEP + "global.and.local.pid.default.file.cfg";
        String lastModifiedFileName = "a.overriding.global.pid.service.file.cfg";

        /*
         * Both files(with local and global pid) contain this property, but with different value.
         * The value for this property in the last processed file must override the previous
         * values for the same property in the configuration.
         */
        String conflictProperty = "global.and.local.property";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        // Modify this file, so that we are sure it is the last modified file in servicesDirectory.
        File fileToModify = new File(configDirectory + SEP + servicesDirectory + SEP + lastModifiedFileName);
        FileUtils.touch(fileToModify);
        cd.processConfigFile(fileToModify);

        String value = getLastModifiedValueForPoperty(getAbsoluteConfigDirectory(configDirectory, servicesDirectory),
                conflictProperty);

        /*
         * Assert that the global pid from the last modified file
         * has overridden the local pid from previously processed file.
         */
        verifyValueOfConfigurationProperty("overridden.local.pid", conflictProperty, value);
    }

    @Test
    public void globalPIDIsOverriddenByLocalPIDInDifferentFileIfTheFileWithTheLocalOneIsModifiedLast()
            throws Exception {
        String configDirectory = configBaseDirectory + SEP + "global_and_local_pid_different_files_conf";
        String servicesDirectory = "global_and_local_pid_overridden_global_services";
        String defaultConfigFilePath = configDirectory + SEP + "global.and.local.pid.default.file.cfg";
        String lastModifiedFileName = "a.overriding.local.pid.service.file.cfg";

        /*
         * Both files(with local and global pid) contain this property, but with different value.
         * The value for this property in the last processed file must override the previous
         * values for the same property in the configuration.
         */
        String conflictProperty = "global.and.local.property";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        // Modify this file, so that we are sure it is the last modified file in servicesDirectory.
        File fileToModify = new File(configDirectory + SEP + servicesDirectory + SEP + lastModifiedFileName);
        FileUtils.touch(fileToModify);
        cd.processConfigFile(fileToModify);

        String value = getLastModifiedValueForPoperty(getAbsoluteConfigDirectory(configDirectory, servicesDirectory),
                conflictProperty);

        /*
         * Assert that the local pid from the last modified file
         * has overridden the global pid from previously processed file.
         */
        verifyValueOfConfigurationProperty("overridden.global.pid", conflictProperty, value);
    }

    @Test
    public void propertiesForPIDInFilesInServicesDirectoryMustOverrideDefaultProperties() {
        String configDirectory = configBaseDirectory + SEP + "default_vs_services_files_conf";
        String servicesDirectory = "default_vs_services_files_services";
        String defaultConfigFilePath = configDirectory + SEP + "default.file.cfg";

        initialize(defaultConfigFilePath);

        cd.processConfigFile(new File(getAbsoluteConfigDirectory(configDirectory, servicesDirectory)));

        /*
         * Assert that a file from the services directory is processed
         * after a file from the root directory, therefore the configuration
         * is updated with properties from the file in the services directory.
         */
        verifyValueOfConfigurationProperty("default.and.service.global.pid", "property1", "value1.2");
        verifyValueOfConfigurationProperty("default.and.service.local.pid", "property2", "value2.2");
    }

    private void initialize(String defaultConfigFile) {
        setSystemProperty(ConfigDispatcher.SERVICECFG_PROG_ARGUMENT, defaultConfigFile);

        cd.activate(bundleContext);
    }

    private Configuration getConfigurationWithContext(String pidWithContext) {
        String pid = null;
        String configContext = null;
        if (pidWithContext.contains(ConfigConstants.SERVICE_CONTEXT_MARKER)) {
            pid = pidWithContext.split(ConfigConstants.SERVICE_CONTEXT_MARKER)[0];
            configContext = pidWithContext.split(ConfigConstants.SERVICE_CONTEXT_MARKER)[1];
        } else {
            throw new IllegalArgumentException("PID does not have a context");
        }
        Configuration[] configs = null;
        try {
            configs = configAdmin.listConfigurations("(&(service.factoryPid=" + pid + ")("
                    + ConfigConstants.SERVICE_CONTEXT + "=" + configContext + "))");
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "IOException occured while retrieving configuration for pid " + pidWithContext, e);
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException(
                    "InvalidSyntaxException occured while retrieving configuration for pid " + pidWithContext, e);
        }

        if (configs == null) {
            return null;
        }

        assertThat(configs.length, is(1));

        return configs[0];
    }

    private void verifyValueOfConfigurationPropertyWithContext(String pidWithContext, String property, String value) {
        waitForAssert(() -> {
            Configuration configuration = getConfigurationWithContext(pidWithContext);

            assertThat(configuration, is(notNullValue()));
            assertThat(configuration.getProperties(), is(notNullValue()));
            assertThat(configuration.getProperties().get(property), is(equalTo(value)));
        });
    }

    private void verifyValueOfConfigurationProperty(String pid, String property, String value) {
        waitForAssert(() -> {
            try {
                configuration = configAdmin.getConfiguration(pid);
            } catch (IOException e) {
                throw new IllegalArgumentException("IOException occured while retrieving configuration for pid " + pid,
                        e);
            }
            assertThat(configuration, is(notNullValue()));
            assertThat(configuration.getProperties(), is(notNullValue()));
            assertThat(configuration.getProperties().get(property), is(equalTo(value)));
        });
    }

    private void verifyNotExistingConfiguration(String pid) {
        /*
         * If a property is not present in the configuration's properties,
         * configuration.getProperties().get(property) should return null.
         *
         * Sending events, related to modification of file, is a OS specific action.
         * So when we check if a configuration is updated, we use separate waitForAssert-s
         * in order to be sure that the events are processed before the assertion.
         */
        waitForAssert(() -> {
            try {
                configuration = configAdmin.getConfiguration(pid);
            } catch (IOException e) {
                throw new IllegalArgumentException("IOException occured while retrieving configuration for pid " + pid,
                        e);
            }
            assertThat(configuration, is(notNullValue()));
            assertThat(configuration.getProperties(), is(nullValue()));
        });
    }

    private void verifyNotExistingConfigurationProperty(String pid, String property) {
        /*
         * If a property is not present in the configuration's properties,
         * configuration.getProperties().get(property) should return null.
         *
         * Sending events, related to modification of file, is a OS specific action.
         * So when we check if a configuration is updated, we use separate waitForAssert-s
         * in order to be sure that the events are processed before the assertion.
         */
        waitForAssert(() -> {
            try {
                configuration = configAdmin.getConfiguration(pid);
            } catch (IOException e) {
                throw new IllegalArgumentException("IOException occured while retrieving configuration for pid " + pid,
                        e);
            }
            assertThat(configuration, is(notNullValue()));
            assertThat(configuration.getProperties(), is(notNullValue()));
            assertThat(configuration.getProperties().get(property), is(nullValue()));
        });
    }

    private void verifyNoPropertiesForConfiguration(String pid) {
        // We have to wait for all the files to be processed and the configuration to be updated.
        waitForAssert(() -> {
            try {
                configuration = configAdmin.getConfiguration(pid);
            } catch (IOException e) {
                throw new IllegalArgumentException("IOException occured while retrieving configuration for pid " + pid,
                        e);
            }
            assertThat(configuration.getProperties(), is(nullValue()));
        });
    }

    private void truncateLastLine(File file) throws IOException {
        List<String> lines = IOUtils.readLines(new FileInputStream(file));
        IOUtils.writeLines(lines.subList(0, lines.size() - 1), "\n", new FileOutputStream(file));
    }

    private String getLastModifiedValueForPoperty(String path, String property) {
        // This method will return null, if there are no files in the directory.
        String value = null;

        File file = getLastModifiedFileFromDir(path);
        if (file == null) {
            return null;
        }
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            List<String> lines = IOUtils.readLines(fileInputStream);
            for (String line : lines) {
                if (line.contains(property + "=")) {
                    value = StringUtils.substringAfter(line, property + "=");
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "An exception " + e + " was thrown, while reading from file " + file.getPath(), e);
        }

        return value;
    }

    /*
     * Mainly used the code, provided in
     * http://stackoverflow.com/questions/2064694/how-do-i-find-the-last-modified-file-in-a-directory-in-java
     */
    private File getLastModifiedFileFromDir(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return null;
        }

        File lastModifiedFile = files[0];
        for (int i = 1; i < files.length; i++) {
            if (lastModifiedFile.lastModified() < files[i].lastModified()) {
                lastModifiedFile = files[i];
            }
        }
        return lastModifiedFile;
    }

    private static void setSystemProperty(String systemProperty, String initialValue) {
        if (initialValue != null) {
            System.setProperty(systemProperty, initialValue);
        } else {
            /* A system property cannot be set to null, it has to be cleared */
            System.clearProperty(systemProperty);
        }
    }
}
