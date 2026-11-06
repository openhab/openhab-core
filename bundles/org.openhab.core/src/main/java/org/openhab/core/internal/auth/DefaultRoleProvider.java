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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.AllSelector;
import org.openhab.core.auth.ItemAction;
import org.openhab.core.auth.PageAction;
import org.openhab.core.auth.Permission;
import org.openhab.core.auth.ResourceType;
import org.openhab.core.auth.RoleDefinition;
import org.openhab.core.auth.RoleProvider;
import org.openhab.core.auth.SitemapAction;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.osgi.service.component.annotations.Component;

/**
 * A {@link RoleProvider} that supplies the built-in default roles. These roles are
 * hardcoded and never change at runtime.
 * <p>
 * The {@code administrator} role has no explicit permissions — enforcement logic
 * (in a future PR) will short-circuit to allow-all for this role.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { RoleProvider.class, DefaultRoleProvider.class })
public class DefaultRoleProvider implements RoleProvider {

    private final List<RoleDefinition> roles;

    public DefaultRoleProvider() {
        roles = List.of(new RoleDefinition("administrator", "Full system access", Set.of(), true),
                new RoleDefinition("user", "Standard user",
                        Set.of(new Permission(ResourceType.ITEM, AllSelector.INSTANCE, ItemAction.COMMAND),
                                new Permission(ResourceType.PAGE, AllSelector.INSTANCE, PageAction.READ),
                                new Permission(ResourceType.SITEMAP, AllSelector.INSTANCE, SitemapAction.READ)),
                        true));
    }

    @Override
    public Collection<RoleDefinition> getAll() {
        return roles;
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<RoleDefinition> listener) {
        // Built-in roles never change — no-op
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<RoleDefinition> listener) {
        // Built-in roles never change — no-op
    }
}
