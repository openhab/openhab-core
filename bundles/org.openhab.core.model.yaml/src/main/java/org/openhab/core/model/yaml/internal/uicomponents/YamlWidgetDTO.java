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

/**
 * The {@link YamlWidgetDTO} is a data transfer object used to serialize a UI widget in a YAML configuration file.
 * It maps to a {@link org.openhab.core.ui.components.RootUIComponent} in the {@code ui:widget} namespace.
 *
 * @author Jimmy Tanagra - Initial contribution
 * @author Ravi Nadahar - Refactored to extend {@link AbstractYamlRootUIComponentDTO}
 */
@YamlElementName("widgets")
public class YamlWidgetDTO extends AbstractYamlRootUIComponentDTO {

    public YamlWidgetDTO() {
    }

    @Override
    public String getUIComponentType() {
        return "widget";
    }
}
