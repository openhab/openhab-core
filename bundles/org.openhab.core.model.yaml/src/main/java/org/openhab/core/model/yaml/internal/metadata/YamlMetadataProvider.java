/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.metadata;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.model.yaml.internal.AbstractYamlProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * The {@link YamlMetadataProvider} is a {@link MetadataProvider} for metadata in YAML files
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { MetadataProvider.class, YamlMetadataProvider.class, YamlModelListener.class })
public class YamlMetadataProvider extends AbstractYamlProvider<Metadata, MetadataKey, YamlMetadataDTO>
        implements MetadataProvider, YamlModelListener<YamlMetadataDTO> {

    @Activate
    public YamlMetadataProvider() {
        super(YamlMetadataDTO.class);
    }

    @Override
    protected @Nullable Metadata map(YamlMetadataDTO yamlElement) {
        return new Metadata(new MetadataKey(yamlElement.namespace, yamlElement.itemName), yamlElement.value,
                yamlElement.config);
    }
}
