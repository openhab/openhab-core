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
package org.openhab.core.automation.module.script.rulesupport.internal.loader;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Script File wrapper offering various methods to inspect the script
 *
 * @author Jonathan Gilbert - Initial contribution
 */
@NonNullByDefault
public class ScriptFileReference implements Comparable<ScriptFileReference> {
    private final AtomicBoolean queued = new AtomicBoolean();
    private final AtomicBoolean loaded = new AtomicBoolean();

    private final Path scriptFilePath;
    private final String scriptType;
    private final int startLevel;

    /**
     * A {@link ScriptFileReference} with a given scriptType
     *
     * @param scriptFilePath the {@link Path} of the file
     * @param scriptType the script type
     * @param startLevel the system start level required for this script
     */
    public ScriptFileReference(Path scriptFilePath, String scriptType, int startLevel) {
        this.scriptFilePath = scriptFilePath;
        this.scriptType = scriptType;
        this.startLevel = startLevel;
    }

    public String getScriptIdentifier() {
        return getScriptIdentifier(scriptFilePath);
    }

    public Path getScriptFilePath() {
        return scriptFilePath;
    }

    public String getScriptType() {
        return scriptType;
    }

    public int getStartLevel() {
        return startLevel;
    }

    public AtomicBoolean getLoadedStatus() {
        return loaded;
    }

    public AtomicBoolean getQueueStatus() {
        return queued;
    }

    @Override
    public int compareTo(ScriptFileReference other) {
        int startLevelCompare = Integer.compare(startLevel, other.startLevel);
        if (startLevelCompare != 0) {
            return startLevelCompare;
        }

        String name1 = scriptFilePath.getFileName().toString();
        String name2 = other.scriptFilePath.getFileName().toString();

        int nameCompare = name1.compareToIgnoreCase(name2);
        if (nameCompare != 0) {
            return nameCompare;
        } else {
            return scriptFilePath.getParent().toString()
                    .compareToIgnoreCase(other.scriptFilePath.getParent().toString());
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScriptFileReference other)) {
            return false;
        }
        return Objects.equals(scriptFilePath, other.scriptFilePath) && Objects.equals(scriptType, other.scriptType)
                && startLevel == other.startLevel;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scriptFilePath, scriptType, startLevel);
    }

    public static String getScriptIdentifier(Path scriptFilePath) {
        return scriptFilePath.toString();
    }
}
