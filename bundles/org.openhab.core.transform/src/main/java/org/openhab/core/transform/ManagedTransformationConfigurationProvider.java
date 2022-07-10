/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.transform;

import java.util.Objects;
import java.util.regex.Matcher;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.storage.StorageService;
import org.openhab.core.transform.ManagedTransformationConfigurationProvider.PersistedTransformationConfiguration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ManagedTransformationConfigurationProvider} implements a {@link TransformationConfigurationProvider} for
 * managed configurations stored in a JSON database
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = { TransformationConfigurationProvider.class,
        ManagedTransformationConfigurationProvider.class }, immediate = true)
public class ManagedTransformationConfigurationProvider
        extends AbstractManagedProvider<TransformationConfiguration, String, PersistedTransformationConfiguration>
        implements TransformationConfigurationProvider {

    @Activate
    public ManagedTransformationConfigurationProvider(final @Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return TransformationConfiguration.class.getName();
    }

    @Override
    protected String keyToString(String key) {
        return key;
    }

    @Override
    protected @Nullable TransformationConfiguration toElement(String key,
            PersistedTransformationConfiguration persistableElement) {
        return new TransformationConfiguration(persistableElement.uid, persistableElement.label,
                persistableElement.type, persistableElement.context, persistableElement.language,
                persistableElement.content);
    }

    @Override
    protected PersistedTransformationConfiguration toPersistableElement(TransformationConfiguration element) {
        return new PersistedTransformationConfiguration(element);
    }

    @Override
    public void add(TransformationConfiguration element) {
        checkConfiguration(element);
        super.add(element);
    }

    @Override
    public @Nullable TransformationConfiguration update(TransformationConfiguration element) {
        checkConfiguration(element);
        return super.update(element);
    }

    private static void checkConfiguration(TransformationConfiguration element) {
        Matcher matcher = TransformationConfigurationRegistry.CONFIG_UID_PATTERN.matcher(element.getUID());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "The transformation configuration UID '" + element.getUID() + "' is invalid.");
        }
        if (!Objects.equals(element.getLanguage(), matcher.group("language"))) {
            throw new IllegalArgumentException("The transformation configuration UID '" + element.getUID()
                    + "' contains(misses) a language, but it is not set (set).");
        }
        if (!Objects.equals(element.getType(), matcher.group("type"))) {
            throw new IllegalArgumentException("The transformation configuration UID '" + element.getUID()
                    + "' is not matching the type '" + element.getType() + "'.");
        }
    }

    public static class PersistedTransformationConfiguration {
        public @NonNullByDefault({}) String uid;
        public @NonNullByDefault({}) String label;
        public @NonNullByDefault({}) String type;
        public @NonNullByDefault({}) String context;
        public @Nullable String language;
        public @NonNullByDefault({}) String content;

        protected PersistedTransformationConfiguration() {
            // default constructor for deserialization
        }

        public PersistedTransformationConfiguration(TransformationConfiguration configuration) {
            this.uid = configuration.getUID();
            this.label = configuration.getLabel();
            this.type = configuration.getType();
            this.context = configuration.getContext();
            this.language = configuration.getLanguage();
            this.content = configuration.getContent();
        }
    }
}
