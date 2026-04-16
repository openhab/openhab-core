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
package org.openhab.core.model.yaml.internal.widgets;

import static org.openhab.core.model.yaml.YamlModelUtils.isIsolatedModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.model.yaml.YamlModelListener;
import org.openhab.core.ui.components.RootUIComponent;
import org.openhab.core.ui.components.UIComponentProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link YamlWidgetProvider} is an OSGi service that allows defining UI widgets in YAML configuration files.
 * These widgets are automatically exposed to the {@link org.openhab.core.ui.components.UIComponentRegistry}
 * under the {@code ui:widget} namespace.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { UIComponentProvider.class, YamlWidgetProvider.class, YamlModelListener.class })
public class YamlWidgetProvider extends AbstractProvider<RootUIComponent>
        implements UIComponentProvider, YamlModelListener<YamlWidgetDTO> {

    public static final String WIDGETS_NAMESPACE = "ui:widget";

    private final Logger logger = LoggerFactory.getLogger(YamlWidgetProvider.class);

    private final Map<String, Collection<RootUIComponent>> widgetsMap = new ConcurrentHashMap<>();

    @Deactivate
    public void deactivate() {
        widgetsMap.clear();
    }

    @Override
    public String getNamespace() {
        return WIDGETS_NAMESPACE;
    }

    @Override
    public Collection<RootUIComponent> getAll() {
        // Ignore isolated models
        return widgetsMap.keySet().stream().filter(name -> !isIsolatedModel(name))
                .map(name -> widgetsMap.getOrDefault(name, List.of())).flatMap(Collection::stream).toList();
    }

    @Override
    public Class<YamlWidgetDTO> getElementClass() {
        return YamlWidgetDTO.class;
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 1;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public void addedModel(String modelName, Collection<YamlWidgetDTO> elements) {
        List<RootUIComponent> added = elements.stream().map(this::mapWidget).filter(Objects::nonNull).toList();
        Collection<RootUIComponent> modelWidgets = Objects
                .requireNonNull(widgetsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelWidgets.addAll(added);
        added.forEach(widget -> {
            logger.debug("model {} added widget {}", modelName, widget.getUID());
            if (!isIsolatedModel(modelName)) {
                notifyListenersAboutAddedElement(widget);
            }
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<YamlWidgetDTO> elements) {
        List<RootUIComponent> updated = elements.stream().map(this::mapWidget).filter(Objects::nonNull).toList();
        Collection<RootUIComponent> modelWidgets = Objects
                .requireNonNull(widgetsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach(widget -> {
            String uid = widget.getUID();
            modelWidgets.stream().filter(w -> w.getUID().equals(uid)).findFirst().ifPresentOrElse(oldWidget -> {
                modelWidgets.remove(oldWidget);
                modelWidgets.add(widget);
                logger.debug("model {} updated widget {}", modelName, uid);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutUpdatedElement(oldWidget, widget);
                }
            }, () -> {
                modelWidgets.add(widget);
                logger.debug("model {} added widget {}", modelName, uid);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutAddedElement(widget);
                }
            });
        });
    }

    @Override
    public void removedModel(String modelName, Collection<YamlWidgetDTO> elements) {
        Collection<RootUIComponent> modelWidgets = widgetsMap.getOrDefault(modelName, List.of());
        elements.stream().map(YamlWidgetDTO::getId).forEach(uid -> {
            modelWidgets.stream().filter(w -> w.getUID().equals(uid)).findFirst().ifPresentOrElse(oldWidget -> {
                modelWidgets.remove(oldWidget);
                logger.debug("model {} removed widget {}", modelName, uid);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutRemovedElement(oldWidget);
                }
            }, () -> logger.debug("model {} widget {} not found", modelName, uid));
        });

        if (modelWidgets.isEmpty()) {
            widgetsMap.remove(modelName);
        }
    }

    private @Nullable RootUIComponent mapWidget(YamlWidgetDTO dto) {
        String uid = dto.uid;
        if (uid == null || uid.isBlank()) {
            logger.warn("Skipping widget with missing uid");
            return null;
        }
        RootUIComponent widget = new RootUIComponent(uid, dto.component != null ? dto.component : "");
        if (dto.config != null) {
            widget.setConfig(dto.config);
        }
        if (dto.slots != null) {
            widget.setSlots(dto.slots);
        }
        if (dto.tags != null) {
            widget.addTags(dto.tags);
        }
        if (dto.props != null) {
            widget.setProps(dto.props);
        }
        return widget;
    }
}
