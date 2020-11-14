/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.io.rest.core.internal.item;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.AbstractUID;
import org.openhab.core.items.MetadataRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Matches available metadata namespaces against the given namespace list or regular expression.
 *
 * @author Henning Treu - Initial contribution
 */
@Component(service = MetadataSelectorMatcher.class)
@NonNullByDefault
public class MetadataSelectorMatcher {

    private final MetadataRegistry metadataRegistry;

    @Activate
    public MetadataSelectorMatcher(final @Reference MetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    /**
     * Filter existing metadata namespaces against the given namespaeSelector. The given String might consist of a comma
     * separated list of namespaces as well as a regular expression.
     *
     * @param namespaceSelector a comma separated list of namespaces or regular expression.
     * @param locale the locale for config descriptions with the scheme "metadata".
     * @return a {@link Set} of matching namespaces.
     */
    public Set<String> filterNamespaces(@Nullable String namespaceSelector, @Nullable Locale locale) {
        if (namespaceSelector == null || namespaceSelector.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<String> originalNamespaces = Arrays.stream(namespaceSelector.split(",")) //
                    .filter(n -> !metadataRegistry.isInternalNamespace(n)) //
                    .map(n -> n.trim()) //
                    .collect(Collectors.toSet());

            Set<String> allMetadataNamespaces = metadataRegistry.getAll().stream() //
                    .map(metadata -> metadata.getUID().getNamespace()) //
                    .distinct() //
                    .collect(Collectors.toSet());

            String namespacePattern = originalNamespaces.stream().collect(Collectors.joining("|"));

            Pattern pattern = Pattern.compile("(" + namespacePattern + ")$");

            Set<String> metadataNamespaces = allMetadataNamespaces.stream() //
                    .filter(n -> !metadataRegistry.isInternalNamespace(n)) //
                    .filter(pattern.asPredicate()).collect(Collectors.toSet());

            // merge metadata namespaces and namespaces from the namespace selector:
            Set<String> result = new HashSet<>(originalNamespaces);
            result.addAll(metadataNamespaces);

            // filter all name spaces which do not match the UID segment pattern (this will be the regex tokens):
            return result.stream().filter(namespace -> namespace.matches(AbstractUID.SEGMENT_PATTERN))
                    .collect(Collectors.toSet());
        }
    }
}
