/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.tools.internal;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.tools.Upgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonInclude.Value;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

/**
 * The {@link YamlConfigurationV1TagsUpgrader} upgrades YAML Tags Configuration from List to Map.
 *
 * Convert list to map format for tags in V1 configuration files.
 *
 * Input file criteria:
 * - Search only in CONF/tags/, or in the given directory, and its subdirectories
 * - Contains a version key with value 1
 * - it must contain a tags key that is a list
 * - The tags list must contain a uid key
 * - If the above criteria are not met, the file will not be modified
 *
 * Output file will
 * - Retain `version: 1`
 * - convert tags list to a map with uid as key and the rest as map
 * - Preserve the order of the tags
 * - other keys will be unchanged
 * - A backup of the original file will be created with the extension `.yaml.org`
 * - If an .org file already exists, append a number to the end, e.g. `.org.1`
 *
 * @since 5.0.0
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public class YamlConfigurationV1TagsUpgrader implements Upgrader {
    private static final String VERSION = "version";

    private final Logger logger = LoggerFactory.getLogger(YamlConfigurationV1TagsUpgrader.class);

    private final ObjectMapper objectMapper;

    public YamlConfigurationV1TagsUpgrader() {
        // match the options used in {@link YamlModelRepositoryImpl}
        objectMapper = YAMLMapper.builder() //
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // omit "---" at file start
                .disable(YAMLGenerator.Feature.SPLIT_LINES) // do not split long lines
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR) // indent arrays
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) // use quotes only where necessary
                .enable(YAMLParser.Feature.PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS) // do not parse ON/OFF/... as booleans
                .findAndAddModules() //
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE) //
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY) //
                .defaultPropertyInclusion(Value.construct(Include.NON_NULL, Include.ALWAYS)) //
                .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN) //
                .build();
    }

    @Override
    public String getName() {
        return "yamlTagsListToMap";
    }

    @Override
    public String getDescription() {
        return "Upgrade YAML 'tags' list to map format on V1 configuration files";
    }

    @Override
    public boolean execute(@Nullable Path userdataPath, @Nullable Path confPath) {
        if (confPath == null) {
            logger.error("{} skipped: no conf directory found.", getName());
            return false;
        }

        String confEnv = System.getenv("OPENHAB_CONF");
        // If confPath is set to OPENHAB_CONF, look inside /tags/ subdirectory otherwise use the given confPath as is.
        // Make configPath "effectively final" inside the lambda below.
        Path configPath = confEnv != null && !confEnv.isBlank() && Path.of(confEnv).toAbsolutePath().equals(confPath)
                ? confPath.resolve("tags")
                : confPath;
        logger.info("Upgrading YAML tags configurations in '{}'", configPath);

        try {
            Files.walkFileTree(configPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(@NonNullByDefault({}) Path file,
                        @NonNullByDefault({}) BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile()) {
                        Path relativePath = configPath.relativize(file);
                        String modelName = relativePath.toString();
                        if (!relativePath.startsWith("automation") && modelName.endsWith(".yaml")) {
                            convertTagsListToMap(file);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(@NonNullByDefault({}) Path file,
                        @NonNullByDefault({}) IOException exc) throws IOException {
                    logger.warn("Failed to process {}: {}", file.toAbsolutePath(), exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Failed to walk through the directory {}: {}", configPath, e.getMessage());
            return false;
        }
        return true;
    }

    private void convertTagsListToMap(Path filePath) {
        try {
            JsonNode fileContent = objectMapper.readTree(filePath.toFile());

            JsonNode versionNode = fileContent.get(VERSION);
            if (versionNode == null || !versionNode.canConvertToInt() || versionNode.asInt() != 1) {
                logger.debug("{} skipped: it doesn't contain a version key", filePath);
                return;
            }

            JsonNode tagsNode = fileContent.get("tags");
            if (tagsNode == null || !tagsNode.isArray()) {
                logger.debug("{} skipped: it doesn't contain a 'tags' array.", filePath);
                return;
            }

            logger.debug("{} found containing v1 yaml file with a 'tags' array", filePath);
            fileContent.properties().forEach(entry -> {
                String key = entry.getKey();
                JsonNode node = entry.getValue();
                if ("tags".equals(key)) {
                    ObjectNode tagsMap = objectMapper.createObjectNode();
                    for (JsonNode tag : node) {
                        if (tag.hasNonNull("uid")) {
                            String uid = tag.get("uid").asText();
                            ((ObjectNode) tag).remove("uid");
                            tagsMap.set(uid, tag);
                        } else {
                            logger.warn("Tag {} does not have a uid, skipping", tag);
                        }
                    }
                    ((ObjectNode) fileContent).set(key, tagsMap);
                }
            });

            String output = objectMapper.writeValueAsString(fileContent);
            saveFile(filePath, output);
        } catch (IOException e) {
            logger.error("Failed to read YAML file {}: {}", filePath, e.getMessage());
            return;
        }
    }

    private void saveFile(Path filePath, String content) {
        Path backupPath = filePath.resolveSibling(filePath.getFileName() + ".org");
        int i = 1;
        while (Files.exists(backupPath)) {
            backupPath = filePath.resolveSibling(filePath.getFileName() + ".org." + i);
            i++;
        }
        try {
            Files.move(filePath, backupPath);
            Files.writeString(filePath, content);
            logger.info("{} converted to map format, and the original file saved as {}", filePath, backupPath);
        } catch (IOException e) {
            logger.error("Failed to save YAML file {}: {}", filePath, e.getMessage());
        }
    }
}
