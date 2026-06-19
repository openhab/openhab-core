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
import org.openhab.core.ui.components.UIComponentRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * {@link YamlBlocksProvider} is an OSGi service that allows defining Blockly blocks collections in YAML format.
 * These blocks collections are automatically exposed to the {@link UIComponentRegistry} under the {@code ui:blocks}
 * namespace.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { UIComponentProvider.class, YamlBlocksProvider.class, YamlModelListener.class })
public class YamlBlocksProvider extends AbstractYamlRootUIComponentProvider<YamlBlocksDTO> {

    public static final String BLOCKS_NAMESPACE = "ui:blocks";

    @Override
    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public String getNamespace() {
        return BLOCKS_NAMESPACE;
    }

    @Override
    public String getUIComponentType() {
        return "blocks";
    }

    @Override
    public Class<YamlBlocksDTO> getElementClass() {
        return YamlBlocksDTO.class;
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 1;
    }
}
