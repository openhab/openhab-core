/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.addon.marketplace.internal.community;

import static org.openhab.core.addon.marketplace.MarketplaceConstants.*;
import static org.openhab.core.addon.marketplace.internal.community.CommunityMarketplaceAddonService.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.Addon;
import org.openhab.core.addon.marketplace.MarketplaceAddonHandler;
import org.openhab.core.addon.marketplace.MarketplaceHandlerException;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.storage.Storage;
import org.openhab.core.storage.StorageService;
import org.openhab.core.transform.ManagedTransformationProvider.PersistedTransformation;
import org.openhab.core.transform.Transformation;
import org.openhab.core.transform.TransformationProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * A {@link MarketplaceAddonHandler} implementation, which handles community provided transformations
 *
 * @author Jan N. Klugg - Initial contribution
 *
 */
@Component(immediate = true)
@NonNullByDefault
public class CommunityTransformationAddonHandler implements MarketplaceAddonHandler, TransformationProvider {
    private final Logger logger = LoggerFactory.getLogger(CommunityTransformationAddonHandler.class);

    private final ObjectMapper yamlMapper;
    private final Gson gson = new Gson();
    private final Storage<PersistedTransformation> storage;
    private final List<ProviderChangeListener<Transformation>> changeListeners = new CopyOnWriteArrayList<>();

    @Activate
    public CommunityTransformationAddonHandler(final @Reference StorageService storageService) {
        this.storage = storageService.getStorage("org.openhab.marketplace.transformation",
                this.getClass().getClassLoader());

        this.yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.findAndRegisterModules();
        this.yamlMapper.setDateFormat(new SimpleDateFormat("MMM d, yyyy, hh:mm:ss aa", Locale.ENGLISH));
        yamlMapper.setAnnotationIntrospector(new AnnotationIntrospectorPair(new SerializedNameAnnotationIntrospector(),
                yamlMapper.getSerializationConfig().getAnnotationIntrospector()));
    }

    @Override
    public boolean supports(String type, String contentType) {
        return "transformation".equals(type) && TRANSFORMATIONS_CONTENT_TYPE.equals(contentType);
    }

    @Override
    public boolean isInstalled(String id) {
        return storage.containsKey(id);
    }

    @Override
    public void install(Addon addon) throws MarketplaceHandlerException {
        try {
            String yamlDownloadUrl = (String) addon.getProperties().get(YAML_DOWNLOAD_URL_PROPERTY);
            String yamlContent = (String) addon.getProperties().get(YAML_CONTENT_PROPERTY);
            String jsonDownloadUrl = (String) addon.getProperties().get(JSON_DOWNLOAD_URL_PROPERTY);
            String jsonContent = (String) addon.getProperties().get(JSON_CONTENT_PROPERTY);

            PersistedTransformation persistedTransformation;

            if (yamlDownloadUrl != null) {
                persistedTransformation = addTransformationFromYAML(addon.getUid(),
                        downloadTransformation(yamlDownloadUrl));
            } else if (yamlContent != null) {
                persistedTransformation = addTransformationFromYAML(addon.getUid(), yamlContent);
            } else if (jsonDownloadUrl != null) {
                persistedTransformation = addTransformationFromJSON(addon.getUid(),
                        downloadTransformation(jsonDownloadUrl));
            } else if (jsonContent != null) {
                persistedTransformation = addTransformationFromJSON(addon.getUid(), jsonContent);
            } else {
                throw new IllegalArgumentException(
                        "Couldn't find the transformation in the add-on entry. The starting code fence may not be marked as ```yaml");
            }
            Transformation transformation = map(persistedTransformation);

            changeListeners.forEach(l -> l.added(this, transformation));
        } catch (IOException e) {
            logger.error("Transformation from marketplace cannot be downloaded: {}", e.getMessage());
            throw new MarketplaceHandlerException("Transformation cannot be downloaded.", e);
        } catch (Exception e) {
            logger.error("Transformation from marketplace is invalid: {}", e.getMessage());
            throw new MarketplaceHandlerException("Transformation is not valid.", e);
        }
    }

    @Override
    public void uninstall(Addon addon) throws MarketplaceHandlerException {
        PersistedTransformation toRemoveElement = storage.remove(addon.getUid());
        if (toRemoveElement != null) {
            Transformation toRemove = map(toRemoveElement);
            changeListeners.forEach(l -> l.removed(this, toRemove));
        }
    }

    private String downloadTransformation(String urlString) throws IOException {
        URL u;
        try {
            u = (new URI(urlString)).toURL();
        } catch (IllegalArgumentException | URISyntaxException e) {
            throw new IOException(e);
        }
        try (InputStream in = u.openStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private PersistedTransformation addTransformationFromYAML(String id, String yaml) {
        try {
            PersistedTransformation transformation = yamlMapper.readValue(yaml, PersistedTransformation.class);
            storage.put(id, transformation);
            return transformation;
        } catch (IOException e) {
            logger.error("Unable to parse YAML: {}", e.getMessage());
            throw new IllegalArgumentException("Unable to parse YAML");
        }
    }

    private PersistedTransformation addTransformationFromJSON(String id, String json) {
        try {
            PersistedTransformation transformation = Objects
                    .requireNonNull(gson.fromJson(json, PersistedTransformation.class));
            storage.put(id, transformation);
            return transformation;
        } catch (JsonParseException e) {
            logger.error("Unable to parse JSON: {}", e.getMessage());
            throw new IllegalArgumentException("Unable to parse JSON");
        }
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<Transformation> listener) {
        changeListeners.add(listener);
    }

    @Override
    public Collection<Transformation> getAll() {
        return storage.getValues().stream().filter(Objects::nonNull).map(Objects::requireNonNull).map(this::map)
                .toList();
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Transformation> listener) {
        changeListeners.remove(listener);
    }

    private Transformation map(PersistedTransformation persistedTransformation) {
        return new Transformation(persistedTransformation.uid, persistedTransformation.label,
                persistedTransformation.type, persistedTransformation.configuration);
    }
}
