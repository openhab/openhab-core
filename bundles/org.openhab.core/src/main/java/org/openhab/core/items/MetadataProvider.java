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
package org.openhab.core.items;

import java.util.Collection;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.common.registry.Provider;

/**
 * This is a marker interface for metadata provider implementations that should be used to register those as an OSGi
 * service.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Mark Herwege - Added reserved namespaces
 */
@NonNullByDefault
public interface MetadataProvider extends Provider<Metadata> {

    /**
     * A {@link MetadataProvider} implementation can reserve a metadata namespace. Only a single provider for this
     * namespace can provide metadata for this namespace. Updating metadata in this namespace will have to be with this
     * provider, and is refused if the provider is not a {@link ManagedProvider}.
     *
     * This is useful if providers calculate metadata and this metadata is not meant to be persisted with a
     * {@link ManagedProvider}. An example is semantics metadata provided by the {@link SemanticsMetadataProvider}.
     * Implementations are expected to return an immutable {@link Collection}.
     *
     * The default implementation returns an empty {@link Set}.
     *
     * @return collection reserved namespaces
     */
    public default Collection<String> getReservedNamespaces() {
        return Set.of();
    }
}
