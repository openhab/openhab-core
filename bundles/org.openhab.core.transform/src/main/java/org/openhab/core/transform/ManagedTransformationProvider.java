/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.storage.StorageService;
import org.openhab.core.transform.ManagedTransformationProvider.PersistedTransformation;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ManagedTransformationProvider} implements a {@link TransformationProvider} for
 * managed transformations stored in a JSON database
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(service = { TransformationProvider.class, ManagedTransformationProvider.class }, immediate = true)
public class ManagedTransformationProvider extends
        AbstractManagedProvider<Transformation, String, PersistedTransformation> implements TransformationProvider {

    @Activate
    public ManagedTransformationProvider(final @Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return Transformation.class.getName();
    }

    @Override
    protected String keyToString(String key) {
        return key;
    }

    @Override
    protected @Nullable Transformation toElement(String key, PersistedTransformation persistableElement) {
        return new Transformation(persistableElement.uid, persistableElement.label, persistableElement.type,
                persistableElement.configuration);
    }

    @Override
    protected PersistedTransformation toPersistableElement(Transformation element) {
        return new PersistedTransformation(element);
    }

    @Override
    public void add(Transformation element) {
        checkConfiguration(element);
        super.add(element);
    }

    @Override
    public @Nullable Transformation update(Transformation element) {
        checkConfiguration(element);
        return super.update(element);
    }

    private static void checkConfiguration(Transformation element) {
        Matcher matcher = TransformationRegistry.CONFIG_UID_PATTERN.matcher(element.getUID());
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "The transformation configuration UID '" + element.getUID() + "' is invalid.");
        }
        if (!Objects.equals(element.getType(), matcher.group("type"))) {
            throw new IllegalArgumentException("The transformation configuration UID '" + element.getUID()
                    + "' is not matching the type '" + element.getType() + "'.");
        }
    }

    public static class PersistedTransformation {
        public @NonNullByDefault({}) String uid;
        public @NonNullByDefault({}) String label;
        public @NonNullByDefault({}) String type;
        public @NonNullByDefault({}) Map<String, String> configuration;

        protected PersistedTransformation() {
            // default constructor for deserialization
        }

        public PersistedTransformation(Transformation configuration) {
            this.uid = configuration.getUID();
            this.label = configuration.getLabel();
            this.type = configuration.getType();
            this.configuration = configuration.getConfiguration();
        }
    }
}
