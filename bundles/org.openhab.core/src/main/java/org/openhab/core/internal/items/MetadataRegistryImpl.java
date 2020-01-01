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
package org.openhab.core.internal.items;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ManagedMetadataProvider;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.items.MetadataRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This is the main implementing class of the {@link MetadataRegistry} interface. It
 * keeps track of all declared metadata of all metadata providers.
 *
 * @author Kai Kreuzer - Initial contribution
 */
@Component(immediate = true, service = MetadataRegistry.class)
@NonNullByDefault
public class MetadataRegistryImpl extends AbstractRegistry<Metadata, MetadataKey, MetadataProvider>
        implements MetadataRegistry {

    public MetadataRegistryImpl() {
        super(MetadataProvider.class);
    }

    @Override
    @Activate
    protected void activate(BundleContext context) {
        super.activate(context);
    }

    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public boolean isInternalNamespace(String namespace) {
        return namespace.startsWith(INTERNAL_NAMESPACE_PREFIX);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    @Override
    protected void setEventPublisher(EventPublisher eventPublisher) {
        super.setEventPublisher(eventPublisher);
    }

    @Override
    protected void unsetEventPublisher(EventPublisher eventPublisher) {
        super.unsetEventPublisher(eventPublisher);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedMetadataProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedMetadataProvider managedProvider) {
        super.unsetManagedProvider(managedProvider);
    }

    @Override
    public void removeItemMetadata(String itemName) {
        // remove our metadata for that item
        getManagedProvider()
                .ifPresent(managedProvider -> ((ManagedMetadataProvider) managedProvider).removeItemMetadata(itemName));
    }

}
