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
package org.openhab.core.model.yaml.internal.pages;

import static org.openhab.core.model.yaml.YamlModelUtils.isIsolatedModel;

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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link YamlPageProvider} is an OSGi service that allows defining UI pages in YAML configuration files.
 * These pages are automatically exposed to the {@link org.openhab.core.ui.components.UIComponentRegistry}
 * under the {@code ui:page} namespace.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { UIComponentProvider.class, YamlPageProvider.class, YamlModelListener.class })
public class YamlPageProvider extends AbstractProvider<RootUIComponent>
        implements UIComponentProvider, YamlModelListener<YamlPageDTO> {

    public static final String PAGES_NAMESPACE = "ui:page";

    private final Logger logger = LoggerFactory.getLogger(YamlPageProvider.class);

    private final Map<String, Collection<RootUIComponent>> pagesMap = new ConcurrentHashMap<>();

    @Deactivate
    public void deactivate() {
        pagesMap.clear();
    }

    @Override
    public String getNamespace() {
        return PAGES_NAMESPACE;
    }

    @Override
    public Collection<RootUIComponent> getAll() {
        // Ignore isolated models
        return pagesMap.keySet().stream().filter(name -> !isIsolatedModel(name))
                .map(name -> pagesMap.getOrDefault(name, List.of())).flatMap(Collection::stream).toList();
    }

    @Override
    public Class<YamlPageDTO> getElementClass() {
        return YamlPageDTO.class;
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
    public void addedModel(String modelName, Collection<YamlPageDTO> elements) {
        List<RootUIComponent> added = elements.stream().map(this::mapPage).filter(Objects::nonNull).toList();
        Collection<RootUIComponent> modelPages = Objects
                .requireNonNull(pagesMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelPages.addAll(added);
        added.forEach(page -> {
            logger.debug("model {} added page {}", modelName, page.getUID());
            if (!isIsolatedModel(modelName)) {
                notifyListenersAboutAddedElement(page);
            }
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<YamlPageDTO> elements) {
        List<RootUIComponent> updated = elements.stream().map(this::mapPage).filter(Objects::nonNull).toList();
        Collection<RootUIComponent> modelPages = Objects
                .requireNonNull(pagesMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach(page -> {
            String uid = page.getUID();
            modelPages.stream().filter(p -> p.getUID().equals(uid)).findFirst().ifPresentOrElse(oldPage -> {
                modelPages.remove(oldPage);
                modelPages.add(page);
                logger.debug("model {} updated page {}", modelName, uid);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutUpdatedElement(oldPage, page);
                }
            }, () -> {
                modelPages.add(page);
                logger.debug("model {} added page {}", modelName, uid);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutAddedElement(page);
                }
            });
        });
    }

    @Override
    public void removedModel(String modelName, Collection<YamlPageDTO> elements) {
        Collection<RootUIComponent> modelPages = pagesMap.getOrDefault(modelName, List.of());
        elements.stream().map(YamlPageDTO::getId).forEach(uid -> {
            modelPages.stream().filter(p -> p.getUID().equals(uid)).findFirst().ifPresentOrElse(oldPage -> {
                modelPages.remove(oldPage);
                logger.debug("model {} removed page {}", modelName, uid);
                if (!isIsolatedModel(modelName)) {
                    notifyListenersAboutRemovedElement(oldPage);
                }
            }, () -> logger.debug("model {} page {} not found", modelName, uid));
        });

        if (modelPages.isEmpty()) {
            pagesMap.remove(modelName);
        }
    }

    private @Nullable RootUIComponent mapPage(YamlPageDTO dto) {
        String uid = dto.uid;
        if (uid == null || uid.isBlank()) {
            logger.warn("Skipping page with missing uid");
            return null;
        }
        RootUIComponent page = new RootUIComponent(uid, dto.component != null ? dto.component : "");
        if (dto.config != null) {
            page.setConfig(dto.config);
        }
        page.setSlots(dto.slots);
        if (dto.tags != null) {
            page.addTags(dto.tags);
        }
        if (dto.props != null) {
            page.setProps(dto.props);
        }
        Date timestamp = dto.timestamp;
        if (timestamp != null) {
            page.setTimestamp(timestamp);
        }
        return page;
    }
}
