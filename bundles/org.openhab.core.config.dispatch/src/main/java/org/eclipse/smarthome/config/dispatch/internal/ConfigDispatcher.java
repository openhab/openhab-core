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
package org.eclipse.smarthome.config.dispatch.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 * This class provides a mean to read any kind of configuration data from
 * config folder files and dispatch it to the different bundles using the {@link ConfigurationAdmin} service.
 *
 * <p>
 * The name of the configuration folder can be provided as a program argument "smarthome.configdir" (default is "conf").
 * Configurations for OSGi services are kept in a subfolder that can be provided as a program argument
 * "smarthome.servicedir" (default is "services"). Any file in this folder with the extension .cfg will be processed.
 *
 * <p>
 * The format of the configuration file is similar to a standard property file, with the exception that the property
 * name can be prefixed by the service pid of the {@link ManagedService}:
 *
 * <p>
 * &lt;service-pid&gt;:&lt;property&gt;=&lt;value&gt;
 *
 * <p>
 * In case the pid does not contain any ".", the default service pid namespace is prefixed, which can be defined by the
 * program argument "smarthome.servicepid" (default is "org.eclipse.smarthome").
 *
 * <p>
 * If no pid is defined in the property line, the default pid namespace will be used together with the filename. E.g. if
 * you have a file "security.cfg", the pid that will be used is "org.eclipse.smarthome.security".
 *
 * <p>
 * Last but not least, a pid can be defined in the first line of a cfg file by prefixing it with "pid:", e.g.
 * "pid: com.acme.smarthome.security".
 *
 * @author Kai Kreuzer - Initial contribution and API
 * @author Petar Valchev - Added sort by modification time, when configuration files are read
 * @author Ana Dimova - Reduce to a single watch thread for all class instances
 * @author Henning Treu - Delete orphan exclusive configuration from configAdmin
 * @author Stefan Triller - Add support for service contexts
 */
@Component(immediate = true, service = ConfigDispatcher.class)
public class ConfigDispatcher {

    private final Logger logger = LoggerFactory.getLogger(ConfigDispatcher.class);

    private final Gson gson = new Gson();

    /** The program argument name for setting the service pid namespace */
    public static final String SERVICEPID_PROG_ARGUMENT = "smarthome.servicepid";

    /** The property to recognize a service instance created by a service factory */
    public static final String SERVICE_CONTEXT = "esh.servicecontext";

    /** The property to separate service PIDs from their contexts */
    public static final String SERVICE_CONTEXT_MARKER = "#";

    /**
     * The program argument name for setting the default services config file
     * name
     */
    public static final String SERVICECFG_PROG_ARGUMENT = "smarthome.servicecfg";

    /** The default namespace for service pids */
    public static final String SERVICE_PID_NAMESPACE = "org.eclipse.smarthome";

    /** The default services configuration filename */
    public static final String SERVICE_CFG_FILE = "smarthome.cfg";

    private static final String PID_MARKER = "pid:";

    private static final String EXCLUSIVE_PID_STORE_FILE = "configdispatcher_pid_list.json";

    private ExclusivePIDMap exclusivePIDMap;

    private ConfigurationAdmin configAdmin;

    private File exclusivePIDStore;

    @Activate
    public void activate(BundleContext bundleContext) {
        exclusivePIDStore = bundleContext.getDataFile(EXCLUSIVE_PID_STORE_FILE);
        loadExclusivePIDList();
        readDefaultConfig();
    }

    private void loadExclusivePIDList() {
        try (FileReader reader = new FileReader(exclusivePIDStore)) {
            exclusivePIDMap = gson.fromJson(reader, ExclusivePIDMap.class);
            if (exclusivePIDMap != null) {
                exclusivePIDMap.initializeProcessPIDMapping();
            }
        } catch (JsonSyntaxException | JsonIOException e) {
            logger.error("Error parsing exclusive pids from '{}': {}", exclusivePIDStore.getAbsolutePath(),
                    e.getMessage());
        } catch (IOException e) {
            logger.debug("Error loading exclusive pids from '{}': {}", exclusivePIDStore.getAbsolutePath(),
                    e.getMessage());
        } finally {
            if (exclusivePIDMap == null) {
                exclusivePIDMap = new ExclusivePIDMap();
            }
        }
    }

    private void storeCurrentExclusivePIDList() {
        try (FileWriter writer = new FileWriter(exclusivePIDStore)) {
            exclusivePIDMap.setCurrentExclusivePIDList();
            gson.toJson(exclusivePIDMap, writer);
        } catch (JsonIOException | IOException e) {
            logger.error("Error storing exclusive PID list in bundle data file: {}", e.getMessage());
        }
    }

    private Configuration getConfigurationWithContext(String pidWithContext)
            throws IOException, InvalidSyntaxException {
        if (!pidWithContext.contains(ConfigConstants.SERVICE_CONTEXT_MARKER)) {
            throw new IllegalArgumentException("Given PID should be followed by a context");
        }
        String pid = pidWithContext.split(ConfigConstants.SERVICE_CONTEXT_MARKER)[0];
        String context = pidWithContext.split(ConfigConstants.SERVICE_CONTEXT_MARKER)[1];

        Configuration[] configs = configAdmin.listConfigurations("(&(" + ConfigurationAdmin.SERVICE_FACTORYPID + "="
                + pid + ")(" + ConfigConstants.SERVICE_CONTEXT + "=" + context + "))");

        if (configs == null || configs.length == 0) {
            return null;
        }
        Configuration configuration = configs[0];

        if (configs.length > 1) {
            logger.error("More than one configuration with PID '{}' exists, using entry '{}'.", pidWithContext,
                    configuration.getProperties().get(Constants.SERVICE_PID));
        }

        return configuration;
    }

    private void processOrphanExclusivePIDs() {
        for (String orphanPID : exclusivePIDMap.getOrphanPIDs()) {
            try {
                Configuration configuration = null;
                if (orphanPID.contains(ConfigConstants.SERVICE_CONTEXT_MARKER)) {
                    configuration = getConfigurationWithContext(orphanPID);
                } else {
                    configuration = configAdmin.getConfiguration(orphanPID, null);
                }
                if (configuration != null) {
                    configuration.delete();
                }
                logger.debug("Deleting configuration for orphan pid {}", orphanPID);
            } catch (IOException | InvalidSyntaxException e) {
                logger.error("Error deleting configuration for orphan pid {}.", orphanPID);
            }
        }
    }

    private String getDefaultServiceConfigPath() {
        String progArg = System.getProperty(SERVICECFG_PROG_ARGUMENT);
        if (progArg != null) {
            return progArg;
        } else {
            return ConfigConstants.getConfigFolder() + File.separator + SERVICE_CFG_FILE;
        }
    }

    private void readDefaultConfig() {
        String defaultCfgPath = getDefaultServiceConfigPath();
        try {
            internalProcessConfigFile(new File(defaultCfgPath));
        } catch (IOException e) {
            logger.warn("Could not process default config file '{}': {}", defaultCfgPath, e.getMessage());
        }
    }

    public void processConfigFile(File dir) {
        if (dir.isDirectory() && dir.exists()) {
            File[] files = dir.listFiles();
            // Sort the files by modification time,
            // so that the last modified file is processed last.
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File left, File right) {
                    return Long.valueOf(left.lastModified()).compareTo(right.lastModified());
                }
            });
            for (File file : files) {
                try {
                    internalProcessConfigFile(file);
                } catch (IOException e) {
                    logger.warn("Could not process config file '{}': {}", file.getName(), e.getMessage());
                }
            }
        } else {
            try {
                internalProcessConfigFile(dir);
            } catch (IOException e) {
                logger.warn("Could not process config file '{}': {}", dir.getName(), e.getMessage());
            }
        }
        processOrphanExclusivePIDs();
        storeCurrentExclusivePIDList();
    }

    private static String getServicePidNamespace() {
        String progArg = System.getProperty(SERVICEPID_PROG_ARGUMENT);
        if (progArg != null) {
            return progArg;
        } else {
            return SERVICE_PID_NAMESPACE;
        }
    }

    /**
     * The filename of a given configuration file is assumed to be the service PID. If the filename
     * without extension contains ".", we assume it is the fully qualified name.
     *
     * @param configFile The configuration file
     * @return The PID
     */
    private String pidFromFilename(File configFile) {
        String filenameWithoutExt = StringUtils.substringBeforeLast(configFile.getName(), ".");
        if (filenameWithoutExt.contains(".")) {
            // it is a fully qualified namespace
            return filenameWithoutExt;
        } else {
            return getServicePidNamespace() + "." + filenameWithoutExt;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void internalProcessConfigFile(File configFile) throws IOException, FileNotFoundException {
        if (configFile.isDirectory() || !configFile.getName().endsWith(".cfg")) {
            logger.debug("Ignoring file '{}'", configFile.getName());
            return;
        }
        logger.debug("Processing config file '{}'", configFile.getName());

        // we need to remember which configuration needs to be updated
        // because values have changed.
        Map<Configuration, Dictionary> configsToUpdate = new HashMap<Configuration, Dictionary>();

        // also cache the already retrieved configurations for each pid
        Map<Configuration, Dictionary> configMap = new HashMap<Configuration, Dictionary>();

        String pid = pidFromFilename(configFile);
        String context = null;

        // configuration file contains a PID Marker
        List<String> lines = IOUtils.readLines(new FileInputStream(configFile));
        String exclusivePID = lines.size() > 0 ? getPIDFromLine(lines.get(0)) : null;
        if (exclusivePID != null) {
            if (exclusivePIDMap.contains(exclusivePID)) {
                logger.warn(
                        "The file {} subsequently defines the exclusive PID '{}'. Overriding existing configuration now.",
                        configFile.getAbsolutePath(), exclusivePID);
            }

            pid = exclusivePID;

            if (exclusivePID.contains(ConfigConstants.SERVICE_CONTEXT_MARKER)) {
                // split pid and context
                pid = exclusivePID.split(ConfigConstants.SERVICE_CONTEXT_MARKER)[0];
                context = exclusivePID.split(ConfigConstants.SERVICE_CONTEXT_MARKER)[1];
            }

            lines = lines.subList(1, lines.size());
            exclusivePIDMap.setProcessedPID(exclusivePID, configFile.getAbsolutePath());
        } else if (exclusivePIDMap.contains(pid)) {
            // the pid was once from an exclusive file but there is either a second non-exclusive-file with config
            // entries or the `pid:` marker was removed.
            exclusivePIDMap.removeExclusivePID(pid);
        }

        Configuration configuration = null;
        // if we have a context we need to create a service factory
        if (context != null) {
            try {
                // try to find configuration using our context property
                configuration = getConfigurationWithContext(exclusivePID);
            } catch (InvalidSyntaxException e) {
                logger.error("Failed to lookup config for file '{}' for PID '{}' with context '{}'",
                        configFile.getName(), pid, context);
            }

            if (configuration == null) {
                logger.debug("Creating factory configuration for PID '{}' with context '{}'", pid, context);
                configuration = configAdmin.createFactoryConfiguration(pid, null);
            } else {
                logger.warn("Configuration for '{}' already exists, updating it with file '{}'.", exclusivePID,
                        configFile.getName());
            }
        } else {
            configuration = configAdmin.getConfiguration(pid, null);
        }

        // this file does only contain entries for this PID and no other files do contain further entries for this PID.
        if (context == null && exclusivePIDMap.contains(pid)) {
            configMap.put(configuration, new Properties());
        } else if (context != null && exclusivePIDMap.contains(exclusivePID)) {
            Dictionary p = new Properties();
            p.put(ConfigConstants.SERVICE_CONTEXT, context);
            configMap.put(configuration, p);
        }

        for (String line : lines) {
            ParseLineResult parsedLine = parseLine(configFile.getPath(), line);
            // no valid configuration line, so continue
            if (parsedLine.isEmpty()) {
                continue;
            }

            if (exclusivePIDMap.contains(pid) && parsedLine.pid != null && !pid.equals(parsedLine.pid)) {
                logger.error("Error parsing config file {}. Exclusive PID {} found but line starts with {}.",
                        configFile.getName(), pid, parsedLine.pid);
                return;
            }

            if (!exclusivePIDMap.contains(pid) && parsedLine.pid != null && exclusivePIDMap.contains(parsedLine.pid)) {
                logger.error(
                        "Error parsing config file {}. The PID {} is exclusive but defined in another file, skipping the line.",
                        configFile.getName(), parsedLine.pid);
                continue;
            }

            if (parsedLine.pid != null) {
                pid = parsedLine.pid;
                configuration = configAdmin.getConfiguration(pid, null);
            }

            Dictionary configProperties = configMap.get(configuration);
            if (configProperties == null) {
                configProperties = configuration.getProperties() != null ? configuration.getProperties()
                        : new Properties();
                configMap.put(configuration, configProperties);
            }
            if (!parsedLine.value.equals(configProperties.get(parsedLine.property))) {
                configProperties.put(parsedLine.property, parsedLine.value);
                configsToUpdate.put(configuration, configProperties);
            }
        }

        for (Entry<Configuration, Dictionary> entry : configsToUpdate.entrySet()) {
            entry.getKey().update(entry.getValue());
        }

        storeCurrentExclusivePIDList();
    }

    public void fileRemoved(String path) {
        exclusivePIDMap.setFileRemoved(path);
        processOrphanExclusivePIDs();
        storeCurrentExclusivePIDList();
    }

    private String getPIDFromLine(String line) {
        if (line.startsWith(PID_MARKER)) {
            return line.substring(PID_MARKER.length()).trim();
        }

        return null;
    }

    private ParseLineResult parseLine(final String filePath, final String line) {
        String trimmedLine = line.trim();
        if (trimmedLine.startsWith("#") || trimmedLine.isEmpty()) {
            return new ParseLineResult();
        }

        String pid = null; // no override of the pid is default
        String key = StringUtils.substringBefore(trimmedLine, "=");
        if (key.contains(":")) {
            pid = StringUtils.substringBefore(key, ":");
            trimmedLine = trimmedLine.substring(pid.length() + 1);
            pid = pid.trim();
            // PID is not fully qualified, so prefix with namespace
            if (!pid.contains(".")) {
                pid = getServicePidNamespace() + "." + pid;
            }
        }
        if (!trimmedLine.isEmpty() && trimmedLine.substring(1).contains("=")) {
            String property = StringUtils.substringBefore(trimmedLine, "=");
            String value = trimmedLine.substring(property.length() + 1);
            return new ParseLineResult(pid, property.trim(), value.trim());
        } else {
            logger.warn("Could not parse line '{}'", line);
            return new ParseLineResult();
        }
    }

    @Reference
    public void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = null;
    }

    /**
     * Represents a result of parseLine().
     */
    private class ParseLineResult {
        public String pid;
        public String property;
        public String value;

        public ParseLineResult() {
            this(null, null, null);
        }

        public ParseLineResult(String pid, String property, String value) {
            this.pid = pid;
            this.property = property;
            this.value = value;
        }

        public boolean isEmpty() {
            return pid == null && property == null && value == null;
        }
    }

    /**
     * The {@link ExclusivePIDMap} serves two purposes:
     * 1. Store the exclusive PIDs which where processed by the {@link ConfigDispatcher} in the bundle data file in JSON
     * format.
     * 2. Map the processed PIDs to the absolute file paths of their config files. This way orphan PIDs from the bundle
     * data file will be recognised and their corresponding configuration will be deleted from configAdmin.
     */
    private class ExclusivePIDMap {

        /**
         * The list will be stored in the bundle cache and loaded on bundle start.
         * This way we can sync the processed files and delete all orphan configurations from the configAdmin.
         */
        private List<String> exclusivePIDs = new ArrayList<>();

        /**
         * The internal Map of PIDs to filenames will only be used during runtime to determine exclusively used
         * service config files.
         * The map will hold a 1:1 relation mapping from an exclusive PID to its absolute path in the file system.
         */
        private transient Map<String, String> processedPIDMapping = new HashMap<>();

        public void setProcessedPID(String pid, String pathToFile) {
            processedPIDMapping.put(pid, pathToFile);
        }

        public void removeExclusivePID(String pid) {
            processedPIDMapping.remove(pid);
        }

        public void setFileRemoved(String absolutePath) {
            for (Entry<String, String> entry : processedPIDMapping.entrySet()) {
                if (entry.getValue().equals(absolutePath)) {
                    entry.setValue(null);
                    return; // we expect a 1:1 relation between PID and path
                }
            }
        }

        public void initializeProcessPIDMapping() {
            processedPIDMapping = new HashMap<>();
            for (String pid : exclusivePIDs) {
                processedPIDMapping.put(pid, null);
            }
        }

        /**
         * Collect PIDs which where not processed (mapped path is null).
         *
         * @return the list of PIDs which where not processed either during #activate or on file deleted event.
         */
        public List<String> getOrphanPIDs() {
            return processedPIDMapping.entrySet().stream().filter(e -> e.getValue() == null).map(e -> e.getKey())
                    .collect(Collectors.toList());
        }

        /**
         * Set the exclusivePID list to the processed PIDs (mapped path is not null).
         */
        public void setCurrentExclusivePIDList() {
            exclusivePIDs = processedPIDMapping.entrySet().stream().filter(e -> e.getValue() != null)
                    .map(e -> e.getKey()).collect(Collectors.toList());
        }

        public boolean contains(String pid) {
            return processedPIDMapping.containsKey(pid);
        }
    }

}
