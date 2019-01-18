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
package org.eclipse.smarthome.config.core.internal.metadata;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter.Type;
import org.eclipse.smarthome.config.core.metadata.MetadataConfigDescriptionProvider;
import org.eclipse.smarthome.config.core.ConfigDescriptionParameterBuilder;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;
import org.eclipse.smarthome.config.core.ParameterOption;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * A {@link ConfigDescriptionProvider} which translated the information of {@link MetadataConfigDescriptionProvider}
 * implementations to normal {@link ConfigDescription}s.
 * <p>
 * It exposes the config description for the "main" value under
 *
 * <pre>
 * {@code
 *     metadata:<namespace>
 * }
 * </pre>
 *
 * and the config descriptions for the parameters under
 *
 * <pre>
 * {@code
 *     metadata:<namespace>:<value>
 * }
 * </pre>
 *
 * so that it becomes dependent of the main value and extensions can request different parameters from the user
 * depending on which main value was chosen. Implementations of course are free to ignore the {@code value} parameter
 * and always return the same set of config descriptions.
 *
 * @author Simon Kaufmann - initial contribution and API
 *
 */
@Component
@NonNullByDefault
public class MetadataConfigDescriptionProviderImpl implements ConfigDescriptionProvider {

    static final String SCHEME = "metadata";
    static final String SEPARATOR = ":";

    private final List<MetadataConfigDescriptionProvider> providers = new CopyOnWriteArrayList<>();

    @Override
    public Collection<ConfigDescription> getConfigDescriptions(@Nullable Locale locale) {
        List<ConfigDescription> ret = new LinkedList<>();
        ret.addAll(getValueConfigDescriptions(locale));
        return ret;
    }

    @Override
    public @Nullable ConfigDescription getConfigDescription(URI uri, @Nullable Locale locale) {
        if (!SCHEME.equals(uri.getScheme())) {
            return null;
        }
        String part = uri.getSchemeSpecificPart();
        String namespace = part.contains(SEPARATOR) ? part.substring(0, part.indexOf(SEPARATOR)) : part;
        String value = part.contains(SEPARATOR) ? part.substring(part.indexOf(SEPARATOR) + 1) : null;
        for (MetadataConfigDescriptionProvider provider : providers) {
            if (namespace.equals(provider.getNamespace())) {
                if (value == null) {
                    return createValueConfigDescription(provider, locale);
                } else {
                    return createParamConfigDescription(provider, value, locale);
                }
            }
        }
        return null;
    }

    private List<ConfigDescription> getValueConfigDescriptions(@Nullable Locale locale) {
        List<ConfigDescription> ret = new LinkedList<>();
        for (MetadataConfigDescriptionProvider provider : providers) {
            ret.add(createValueConfigDescription(provider, locale));
        }
        return ret;
    }

    private ConfigDescription createValueConfigDescription(MetadataConfigDescriptionProvider provider,
            @Nullable Locale locale) {
        String namespace = provider.getNamespace();
        String description = provider.getDescription(locale);
        List<ParameterOption> options = provider.getParameterOptions(locale);
        URI uri = URI.create(SCHEME + SEPARATOR + namespace);

        ConfigDescriptionParameterBuilder builder = ConfigDescriptionParameterBuilder.create("value", Type.TEXT);
        if (options != null && !options.isEmpty()) {
            builder.withOptions(options);
            builder.withLimitToOptions(true);
        } else {
            builder.withLimitToOptions(false);
        }
        builder.withDescription(description != null ? description : namespace);
        ConfigDescriptionParameter parameter = builder.build();

        return new ConfigDescription(uri, Collections.singletonList(parameter));
    }

    private @Nullable ConfigDescription createParamConfigDescription(MetadataConfigDescriptionProvider provider,
            String value, @Nullable Locale locale) {
        String namespace = provider.getNamespace();
        URI uri = URI.create(SCHEME + SEPARATOR + namespace + SEPARATOR + value);
        List<ConfigDescriptionParameter> parameters = provider.getParameters(value, locale);
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        return new ConfigDescription(uri, parameters);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addMetadataConfigDescriptionProvider(
            MetadataConfigDescriptionProvider metadataConfigDescriptionProvider) {
        providers.add(metadataConfigDescriptionProvider);
    }

    protected void removeMetadataConfigDescriptionProvider(
            MetadataConfigDescriptionProvider metadataConfigDescriptionProvider) {
        providers.remove(metadataConfigDescriptionProvider);
    }

}
