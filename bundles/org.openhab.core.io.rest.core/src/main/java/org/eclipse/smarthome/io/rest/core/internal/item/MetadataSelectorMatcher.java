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
package org.eclipse.smarthome.io.rest.core.internal.item;

import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionRegistry;
import org.eclipse.smarthome.core.common.AbstractUID;
import org.eclipse.smarthome.core.items.MetadataRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Matches available metadata namespaces against the given namespace list or regular expression.
 *
 * @author Henning Treu - initial contribution
 *
 */
@Component(service = MetadataSelectorMatcher.class)
public class MetadataSelectorMatcher {

    private static final String METADATA_SCHEME = "metadata";
    private static final String METADATA_SCHEME_PREFIX = METADATA_SCHEME + ":";

    private MetadataRegistry metadataRegistry;

    private ConfigDescriptionRegistry configDescriptionRegistry;

    /**
     * Filter existing metadata namespaces against the given namespaeSelector. The given String might consist of a comma
     * separated list of namespaces as well as a regular expression.
     *
     * @param namespaceSelector a comma separated list of namespaces or regular expression.
     * @param locale the locale for config descriptions with the scheme "metadata".
     * @return a {@link Set} of matching namespaces.
     */
    public Set<String> filterNamespaces(String namespaceSelector, Locale locale) {
        if (namespaceSelector == null || namespaceSelector.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<String> originalNamespaces = Arrays.stream(namespaceSelector.split(",")) //
                    .filter(n -> !metadataRegistry.isInternalNamespace(n)) //
                    .map(n -> n.trim()) //
                    .collect(Collectors.toSet());

            String namespacePattern = originalNamespaces.stream().collect(Collectors.joining("|"));

            Pattern pattern = Pattern.compile(METADATA_SCHEME_PREFIX + "(" + namespacePattern + ")$");
            Collection<ConfigDescription> configDescriptions = configDescriptionRegistry.getConfigDescriptions(locale);

            Set<String> configNamespaces = configDescriptions.stream()
                    .filter(cd -> cd.getUID().getScheme().equals(METADATA_SCHEME)).map(cd -> cd.getUID().toString())
                    .filter(pattern.asPredicate()).map(uri -> uri.substring(METADATA_SCHEME_PREFIX.length()))
                    .collect(toSet());

            // merge configDescription namespaces and namespaces from the namespace selector:
            Set<String> result = new HashSet<>(originalNamespaces);
            result.addAll(configNamespaces);

            // filter all name spaces which do not match the UID segment pattern (this will be the regex tokens):
            return result.stream().filter(namespace -> namespace.matches(AbstractUID.SEGMENT_PATTERN)).collect(toSet());
        }
    }

    @Reference
    protected void setMetadataRegistry(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    protected void unsetMetadataRegistry(MetadataRegistry metadataRegistry) {
        this.metadataRegistry = null;
    }

    @Reference
    protected void setConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescriptionRegistry = configDescriptionRegistry;
    }

    protected void unsetConfigDescriptionRegistry(ConfigDescriptionRegistry configDescriptionRegistry) {
        this.configDescriptionRegistry = null;
    }
}
