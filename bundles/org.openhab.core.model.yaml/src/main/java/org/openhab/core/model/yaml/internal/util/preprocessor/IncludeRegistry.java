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
package org.openhab.core.model.yaml.internal.util.preprocessor;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.BidiSetBag;

/**
 * The {@link IncludeRegistry} manages a bidirectional association between
 * model names and the include files they reference.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
public final class IncludeRegistry {
    private final BidiSetBag<String, Path> index = new BidiSetBag<>();

    public synchronized void registerModelInclude(String modelName, Path include) {
        index.put(modelName, include);
    }

    public synchronized void removeModel(String modelName) {
        index.removeKey(modelName);
    }

    public synchronized Set<String> getModelsForInclude(Path include) {
        return new HashSet<>(index.getKeys(include));
    }

    public synchronized boolean hasInclude(Path include) {
        return !index.getKeys(include).isEmpty();
    }

    public synchronized void clear() {
        index.clear();
    }
}
