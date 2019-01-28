/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.sitemap.internal;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.model.core.EventType;
import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.model.core.ModelRepositoryChangeListener;
import org.eclipse.smarthome.model.sitemap.Sitemap;
import org.eclipse.smarthome.model.sitemap.SitemapProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides access to the sitemap model files.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
@NonNullByDefault
@Component(service = SitemapProvider.class)
public class SitemapProviderImpl implements SitemapProvider, ModelRepositoryChangeListener {

    private static final String SITEMAP_MODEL_NAME = "sitemap";
    protected static final String SITEMAP_FILEEXT = "." + SITEMAP_MODEL_NAME;

    private final Logger logger = LoggerFactory.getLogger(SitemapProviderImpl.class);

    private @NonNullByDefault({}) ModelRepository modelRepo;

    private final Map<String, Sitemap> sitemapModelCache = new ConcurrentHashMap<>();

    private final Set<ModelRepositoryChangeListener> modelChangeListeners = new CopyOnWriteArraySet<>();

    @Reference
    public void setModelRepository(ModelRepository modelRepo) {
        this.modelRepo = modelRepo;
    }

    public void unsetModelRepository(ModelRepository modelRepo) {
        this.modelRepo = null;
    }

    @Activate
    protected void activate() {
        refreshSitemapModels();
        modelRepo.addModelRepositoryChangeListener(this);
    }

    @Deactivate
    protected void deactivate() {
        if (modelRepo != null) {
            modelRepo.removeModelRepositoryChangeListener(this);
        }
        sitemapModelCache.clear();
    }

    @Override
    public @Nullable Sitemap getSitemap(String sitemapName) {
        String filename = sitemapName + SITEMAP_FILEEXT;
        Sitemap sitemap = sitemapModelCache.get(filename);
        if (sitemap != null) {
            if (!sitemap.getName().equals(sitemapName)) {
                logger.warn(
                        "Filename `{}` does not match the name `{}` of the sitemap - please fix this as you might see unexpected behavior otherwise.",
                        filename, sitemap.getName());
            }
            return sitemap;
        } else {
            logger.trace("Sitemap {} cannot be found", sitemapName);
            return null;
        }
    }

    @Override
    public Set<String> getSitemapNames() {
        return sitemapModelCache.keySet().stream().map(name -> StringUtils.removeEnd(name, SITEMAP_FILEEXT))
                .collect(Collectors.toSet());
    }

    @Override
    public void modelChanged(String modelName, EventType type) {
        if (modelName.endsWith(SITEMAP_FILEEXT)) {
            if (type == EventType.REMOVED) {
                sitemapModelCache.remove(modelName);
            } else {
                EObject sitemap = modelRepo.getModel(modelName);
                // if the sitemap file is empty it will not be in the repo and thus there is no need to cache it here
                if (sitemap instanceof Sitemap) {
                    sitemapModelCache.put(modelName, (Sitemap) sitemap);
                }
            }
        }
        for (ModelRepositoryChangeListener listener : modelChangeListeners) {
            listener.modelChanged(modelName, type);
        }
    }

    private void refreshSitemapModels() {
        sitemapModelCache.clear();
        Iterable<String> sitemapNames = modelRepo.getAllModelNamesOfType(SITEMAP_MODEL_NAME);
        for (String sitemapName : sitemapNames) {
            sitemapModelCache.put(sitemapName, (Sitemap) modelRepo.getModel(sitemapName));
        }
    }

    @Override
    public void addModelChangeListener(ModelRepositoryChangeListener listener) {
        modelChangeListeners.add(listener);
    }

    @Override
    public void removeModelChangeListener(ModelRepositoryChangeListener listener) {
        modelChangeListeners.remove(listener);
    }

}
