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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.ui.components.UIComponentProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * {@link YamlWidgetProvider} is an OSGi service that allows defining UI widgets in YAML configuration files.
 * These widgets are automatically exposed to the {@link org.openhab.core.ui.components.UIComponentRegistry}
 * under the {@code ui:widget} namespace.
 *
 * @author Jimmy Tanagra - Initial contribution
 * @author Ravi Nadahar - Refactored to extend {@link AbstractYamlRootUIComponentProvider}
 */
@NonNullByDefault
@Component(immediate = true, service = { UIComponentProvider.class, YamlWidgetProvider.class, YamlModelListener.class })
public class YamlWidgetProvider extends AbstractYamlRootUIComponentProvider<YamlWidgetDTO> {

    public static final String WIDGETS_NAMESPACE = "ui:widget";

    @Override
    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public String getNamespace() {
        return WIDGETS_NAMESPACE;
    }

    @Override
    public String getUIComponentType() {
        return "widget";
    }

    @Override
    public Class<YamlWidgetDTO> getElementClass() {
        return YamlWidgetDTO.class;
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 1;
    }
}
