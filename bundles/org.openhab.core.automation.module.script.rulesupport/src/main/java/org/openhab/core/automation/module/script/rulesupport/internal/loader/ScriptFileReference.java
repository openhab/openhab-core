/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.automation.module.script.rulesupport.internal.loader;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.service.StartLevelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Script File wrapper offering various methods to inspect the script
 *
 * @author Jonathan Gilbert - initial contribution
 */
@NonNullByDefault
public class ScriptFileReference implements Comparable<ScriptFileReference> {

    private static final Set<String> EXCLUDED_FILE_EXTENSIONS = new HashSet<>(
            Arrays.asList("txt", "old", "example", "backup", "md", "swp", "tmp", "bak"));

    private static final Pattern[] startLevelPatterns = new Pattern[] { Pattern.compile(".*/sl(\\d{2})/[^/]+"), // script
                                                                                                                // in
                                                                                                                // immediate
                                                                                                                // slXX
                                                                                                                // directory
            Pattern.compile(".*/[^/]+\\.sl(\\d{2})\\.[^/.]+") // script named <name>.slXX.<ext>
    };

    private static final Logger logger = LoggerFactory.getLogger(ScriptFileReference.class);

    private final URL scriptFileURL;

    public ScriptFileReference(URL scriptFileURL) {
        this.scriptFileURL = scriptFileURL;
    }

    public URL getScriptFileURL() {
        return scriptFileURL;
    }

    public int getStartLevel() {
        for (Pattern p : startLevelPatterns) {
            Matcher m = p.matcher(scriptFileURL.getPath());
            if (m.find() && m.groupCount() > 0) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException nfe) {
                    logger.warn("Extracted start level {} from {}, but it's not an integer. Ignoring.", m.group(1),
                            scriptFileURL.getPath());
                }
            }
        }

        return StartLevelService.STARTLEVEL_RULEENGINE;
    }

    public Optional<String> getScriptType() {
        String fileName = scriptFileURL.getPath();
        int index = fileName.lastIndexOf(".");
        if (index == -1) {
            return Optional.empty();
        }
        String fileExtension = fileName.substring(index + 1);

        // ignore known file extensions for "temp" files
        if (EXCLUDED_FILE_EXTENSIONS.contains(fileExtension) || fileExtension.endsWith("~")) {
            return Optional.empty();
        }
        return Optional.of(fileExtension);
    }

    public String getScriptIdentifier() {
        return scriptFileURL.toString();
    }

    @Override
    public int compareTo(ScriptFileReference other) {
        try {
            Path path1 = Paths.get(scriptFileURL.toURI());
            String name1 = path1.getFileName().toString();
            logger.trace("o1 [{}], path1 [{}], name1 [{}]", scriptFileURL, path1, name1);

            Path path2 = Paths.get(other.scriptFileURL.toURI());
            String name2 = path2.getFileName().toString();
            logger.trace("o2 [{}], path2 [{}], name2 [{}]", other.scriptFileURL, path2, name2);

            int nameCompare = name1.compareToIgnoreCase(name2);
            if (nameCompare != 0) {
                return nameCompare;
            } else {
                return path1.getParent().toString().compareToIgnoreCase(path2.getParent().toString());
            }
        } catch (URISyntaxException e) {
            logger.error("URI syntax exception", e);
            return 0;
        }
    }
}
