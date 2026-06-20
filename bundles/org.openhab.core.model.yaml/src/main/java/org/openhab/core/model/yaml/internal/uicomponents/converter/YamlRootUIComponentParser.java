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
package org.openhab.core.model.yaml.internal.uicomponents.converter;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.uicomponents.YamlBlocksProvider;
import org.openhab.core.model.yaml.internal.uicomponents.YamlWidgetProvider;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.converter.RootUIComponentParser;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link YamlRootUIComponentParser} is the YAML parser for UI component objects.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { RootUIComponentParser.class })
public class YamlRootUIComponentParser implements RootUIComponentParser {

    private final YamlModelRepository modelRepository;
    private final YamlWidgetProvider widgetProvider;
    private final YamlBlocksProvider blocksProvider;

    @Activate
    public YamlRootUIComponentParser(@Reference YamlModelRepository modelRepository,
            @Reference YamlWidgetProvider widgetProvider, @Reference YamlBlocksProvider blocksProvider) {
        this.modelRepository = modelRepository;
        this.widgetProvider = widgetProvider;
        this.blocksProvider = blocksProvider;
    }

    @Override
    public String getParserFormat() {
        return "YAML";
    }

    @Override
    public @Nullable String startParsingFormat(String syntax, List<String> errors, List<String> warnings) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(syntax.getBytes(StandardCharsets.UTF_8));
        return modelRepository.createIsolatedModel(inputStream, errors, warnings);
    }

    @Override
    public Collection<RootUIComponent> getParsedObjects(String modelName) {
        return Stream.concat(widgetProvider.getAllFromModel(modelName).stream(),
                blocksProvider.getAllFromModel(modelName).stream()).toList();
    }

    @Override
    public Collection<? extends RootUIComponent> getParsedObjects(String modelName, RootUIComponentType type) {
        switch (type) {
            case BLOCK_LIBRARY:
                return blocksProvider.getAllFromModel(modelName);
            case WIDGET:
                return widgetProvider.getAllFromModel(modelName);
            default:
                return List.of();
        }
    }

    @Override
    public void finishParsingFormat(String modelName) {
        modelRepository.removeIsolatedModel(modelName);
    }
}
