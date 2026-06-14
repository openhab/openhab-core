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
 * {@link YamlPageProvider} is an OSGi service that allows defining UI pages in YAML configuration files.
 * These pages are automatically exposed to the {@link org.openhab.core.ui.components.UIComponentRegistry}
 * under the {@code ui:page} namespace.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { UIComponentProvider.class, YamlPageProvider.class, YamlModelListener.class })
public class YamlPageProvider extends AbstractYamlRootUIComponentProvider<YamlPageDTO> {

    public static final String PAGES_NAMESPACE = "ui:page";

    @Override
    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public String getNamespace() {
        return PAGES_NAMESPACE;
    }

    @Override
    public String getUIComponentType() {
        return "page";
    }

    @Override
    public Class<YamlPageDTO> getElementClass() {
        return YamlPageDTO.class;
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 1;
    }
}
