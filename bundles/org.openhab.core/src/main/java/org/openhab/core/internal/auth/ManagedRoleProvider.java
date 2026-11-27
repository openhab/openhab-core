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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Actions;
import org.openhab.core.auth.Permission;
import org.openhab.core.auth.ResourceType;
import org.openhab.core.auth.RoleDefinition;
import org.openhab.core.auth.SelectorParser;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link org.openhab.core.common.registry.ManagedProvider} for user-created
 * {@link RoleDefinition} entities, persisted in JSON DB.
 * <p>
 * Uses {@link PersistedRoleDefinition} as the storage DTO to avoid Gson interface
 * deserialization issues with the typed {@link org.openhab.core.auth.Selector} and
 * {@link org.openhab.core.auth.Action} fields in {@link Permission}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
@Component(service = ManagedRoleProvider.class, immediate = true)
public class ManagedRoleProvider extends AbstractManagedProvider<RoleDefinition, String, PersistedRoleDefinition> {

    private final Logger logger = LoggerFactory.getLogger(ManagedRoleProvider.class);

    @Activate
    public ManagedRoleProvider(final @Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return "roles";
    }

    @Override
    protected String keyToString(String key) {
        return key;
    }

    @Override
    protected @Nullable RoleDefinition toElement(String key, PersistedRoleDefinition persisted) {
        try {
            Set<Permission> permissions = new HashSet<>();
            for (PersistedPermission pp : persisted.permissions) {
                ResourceType type = ResourceType.valueOf(pp.resourceType);
                permissions
                        .add(new Permission(type, SelectorParser.parse(pp.selector), Actions.parse(type, pp.action)));
            }
            return new RoleDefinition(persisted.name, persisted.description, permissions, persisted.builtIn);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to deserialize role '{}': {}", key, e.getMessage());
            return null;
        }
    }

    @Override
    protected PersistedRoleDefinition toPersistableElement(RoleDefinition role) {
        return new PersistedRoleDefinition(role.getName(), role.getDescription(), role.getPermissions().stream().map(
                p -> new PersistedPermission(p.resourceType().name(), p.selector().expression(), p.action().name()))
                .collect(Collectors.toList()), role.isBuiltIn());
    }
}
