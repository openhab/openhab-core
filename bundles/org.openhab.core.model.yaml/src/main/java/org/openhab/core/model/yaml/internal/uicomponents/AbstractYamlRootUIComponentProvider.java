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

import static org.openhab.core.model.yaml.YamlModelUtils.isIsolatedModel;

import java.security.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link AbstractYamlRootUIComponentProvider} is an abstract class that contains common functionality needed
 * when implementing a {@link Provider} for UI components in YAML format.
 *
 * @author Ravi Nadahar - Initial contribution
 *
 * @param <D> the DTO type
 */
@NonNullByDefault
public abstract class AbstractYamlRootUIComponentProvider<D extends AbstractYamlRootUIComponentDTO>
        extends AbstractProvider<RootUIComponent> implements UIComponentProvider, YamlModelListener<D> {

    private final Logger logger = LoggerFactory.getLogger(AbstractYamlRootUIComponentProvider.class);

    protected final Map<String, List<RootUIComponent>> componentsMap = new ConcurrentHashMap<>();

    public void deactivate() {
        componentsMap.clear();
    }

    /**
     * @return The type of UI component, e.g. {@code widget} or {@code page}.
     */
    public abstract String getUIComponentType();

    @Override
    public Collection<RootUIComponent> getAll() {
        // Ignore isolated models
        return componentsMap.keySet().stream().filter(name -> !isIsolatedModel(name))
                .map(name -> componentsMap.getOrDefault(name, List.of())).flatMap(Collection::stream).toList();
    }

    public Collection<RootUIComponent> getAllFromModel(String modelName) {
        return List.copyOf(componentsMap.getOrDefault(modelName, List.of()));
    }

    @Override
    public void addedModel(String modelName, Collection<D> elements) {
        List<RootUIComponent> added = elements.stream().map(this::mapComponent).filter(Objects::nonNull).toList();
        Collection<RootUIComponent> modelComponents = Objects
                .requireNonNull(componentsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelComponents.addAll(added);
        added.forEach(component -> {
            logger.debug("model {} added {} {}", modelName, getUIComponentType(), component.getUID());
            if (!isIsolatedModel(modelName)) {
                notifyListenersAboutAddedElement(component);
            }
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<D> elements) {
        List<RootUIComponent> updated = elements.stream().map(this::mapComponent).filter(Objects::nonNull).toList();
        Collection<RootUIComponent> modelComponents = Objects
                .requireNonNull(componentsMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach(component -> {
            String uid = component.getUID();
            modelComponents.stream().filter(w -> w.getUID().equals(uid)).findFirst().ifPresentOrElse(oldComponent -> {
                modelComponents.remove(oldComponent);
                modelComponents.add(component);
                logger.debug("model {} updated {} {}", modelName, getUIComponentType(), uid);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutUpdatedElement(oldComponent, component);
                }
            }, () -> {
                modelComponents.add(component);
                logger.debug("model {} added {} {}", modelName, getUIComponentType(), uid);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutAddedElement(component);
                }
            });
        });
    }

    @Override
    public void removedModel(String modelName, Collection<D> elements) {
        Collection<RootUIComponent> modelComponents = componentsMap.getOrDefault(modelName, new ArrayList<>());
        elements.stream().map(D::getId).forEach(uid -> {
            modelComponents.stream().filter(c -> c.getUID().equals(uid)).findFirst().ifPresentOrElse(oldComponent -> {
                modelComponents.remove(oldComponent);
                logger.debug("model {} removed {} {}", modelName, getUIComponentType(), uid);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutRemovedElement(oldComponent);
                }
            }, () -> logger.debug("model {} {} {} not found", modelName, getUIComponentType(), uid));
        });

        if (modelComponents.isEmpty()) {
            componentsMap.remove(modelName);
        }
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    protected @Nullable RootUIComponent mapComponent(D dto) {
        String uid = dto.uid;
        if (uid == null || uid.isBlank()) {
            logger.warn("Skipping {} with missing uid", getUIComponentType());
            return null;
        }
        RootUIComponent component = new RootUIComponent(uid, dto.component != null ? dto.component : "");
        if (dto.config != null) {
            component.setConfig(dto.config);
        }
        if (dto.slots != null) {
            component.setSlots(dto.slots);
        }
        if (dto.tags != null) {
            component.addTags(dto.tags);
        }
        if (dto.props != null) {
            component.setProps(dto.props.toConfigDescriptionDTO());
        }
        Date timestamp = dto.timestamp;
        if (timestamp != null) {
            component.setTimestamp(timestamp);
        }
        return component;
    }
}
