/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.model.yaml;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Static utility methods that are helpful when dealing with YAML models.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public class YamlModelUtils {

    public static final String PREFIX_TMP_MODEL = "tmp_";

    public static boolean isIsolatedModel(String modelName) {
        return modelName.startsWith(PREFIX_TMP_MODEL);
    }
}
