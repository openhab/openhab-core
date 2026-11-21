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
package org.openhab.core.internal.auth;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.RoleDefinition;
import org.openhab.core.auth.RoleProvider;
import org.openhab.core.auth.RoleRegistry;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.service.ReadyService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The implementation of a {@link RoleRegistry} that aggregates {@link RoleDefinition} entities
 * from both the {@link DefaultRoleProvider} (built-in roles) and the {@link ManagedRoleProvider}
 * (user-created roles persisted in JSON DB).
 * <p>
 * Follows the {@code SemanticTagRegistryImpl} constructor-injection pattern: both providers are
 * added eagerly in the constructor, and {@link #addProvider} is overridden to prevent
 * double-registration by the OSGi service tracker.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
@Component(service = RoleRegistry.class, immediate = true)
public class RoleRegistryImpl extends AbstractRegistry<RoleDefinition, String, RoleProvider> implements RoleRegistry {

    private final DefaultRoleProvider defaultRoleProvider;
    private final ManagedRoleProvider managedProvider;
    private final Set<String> defaultRoleUIDs;

    @Activate
    public RoleRegistryImpl(@Reference DefaultRoleProvider defaultRoleProvider,
            @Reference ManagedRoleProvider managedProvider, @Reference ReadyService readyService) {
        super(RoleProvider.class);
        this.defaultRoleProvider = defaultRoleProvider;
        this.managedProvider = managedProvider;
        // Capture built-in role UIDs for isDefault() checks — uses UID-based lookup
        // since RoleDefinition does not override equals/hashCode (matches ManagedUser convention).
        this.defaultRoleUIDs = defaultRoleProvider.getAll().stream().map(RoleDefinition::getUID)
                .collect(Collectors.toUnmodifiableSet());
        // Add the default roles provider first, before all others
        super.addProvider(defaultRoleProvider);
        super.setReadyService(readyService);
        setManagedProvider(managedProvider);
        super.addProvider(managedProvider);
    }

    @Override
    protected void addProvider(Provider<RoleDefinition> provider) {
        // Prevent double-registration by OSGi service tracker —
        // both providers are already added in the constructor via super.addProvider().
        if (!provider.equals(defaultRoleProvider) && !provider.equals(managedProvider)) {
            super.addProvider(provider);
        }
    }

    @Override
    public @Nullable RoleDefinition remove(String key) {
        RoleDefinition role = get(key);
        if (role != null && isDefault(role)) {
            throw new IllegalArgumentException("Built-in role '" + key + "' cannot be removed");
        }
        return super.remove(key);
    }

    @Override
    public boolean isEditable(RoleDefinition role) {
        return managedProvider.get(role.getUID()) != null;
    }

    @Override
    public boolean isDefault(RoleDefinition role) {
        return defaultRoleUIDs.contains(role.getUID());
    }
}
