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
package org.openhab.core.model.yaml.internal.uicomponents;

import org.openhab.core.model.yaml.YamlElementName;
import org.openhab.core.ui.components.RootUIComponent;

/**
 * The {@link YamlBlocksDTO} is a data transfer object used to serialize a Blockly blocks collection in YAML format.
 * It maps to a {@link RootUIComponent} in the {@code ui:blocks} namespace.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@YamlElementName("blocks")
public class YamlBlocksDTO extends AbstractYamlRootUIComponentDTO {

    @Override
    public String getUIComponentType() {
        return "blocks";
    }
}
