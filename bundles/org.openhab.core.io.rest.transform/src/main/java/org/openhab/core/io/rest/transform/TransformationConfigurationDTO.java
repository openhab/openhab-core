/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.io.rest.transform;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.transform.TransformationConfiguration;

/**
 * The {@link TransformationConfigurationDTO} wraps a {@link TransformationConfiguration}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class TransformationConfigurationDTO {
    public String uid;
    public String label;
    public String type;
    public @Nullable String language;
    public Map<String, String> configuration;
    public boolean editable = false;

    public TransformationConfigurationDTO(TransformationConfiguration transformationConfiguration) {
        this.uid = transformationConfiguration.getUID();
        this.label = transformationConfiguration.getLabel();
        this.type = transformationConfiguration.getType();
        this.language = transformationConfiguration.getLanguage();
        this.configuration = transformationConfiguration.getConfiguration();
    }
}
