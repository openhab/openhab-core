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
package org.openhab.core.auth.authorization.internal;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Action;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthorizationService;
import org.openhab.core.auth.Permission;
import org.openhab.core.auth.PermissionEvaluator;
import org.openhab.core.auth.ResourceType;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.RoleDefinition;
import org.openhab.core.auth.RoleRegistry;
import org.openhab.core.common.registry.RegistryChangeListener;
import org.openhab.core.common.registry.RegistryChangedRunnableListener;
import org.openhab.core.config.core.ConfigParser;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AuthorizationService} that checks permissions against
 * {@link RoleDefinition}s from the {@link RoleRegistry} and delegates evaluation
 * to registered {@link PermissionEvaluator}s.
 * <p>
 * When disabled ({@code enabled=false}, the default), all permission checks return
 * {@code true}. When enabled, the administrator role short-circuits to allow-all,
 * and other roles are evaluated against their permissions.
 * <p>
 * To enable RBAC, create {@code $OPENHAB_CONF/services/org.openhab.authorization.cfg}
 * with {@code enabled=true}.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
@Component(service = AuthorizationService.class, immediate = true, configurationPid = "org.openhab.authorization", property = Constants.SERVICE_PID
        + "=org.openhab.authorization")
public class AuthorizationServiceImpl implements AuthorizationService {

    private static final String CONFIG_ENABLED = "enabled";
    private static final int MAX_CACHE_SIZE = 10_000;

    private final Logger logger = LoggerFactory.getLogger(AuthorizationServiceImpl.class);

    private final RoleRegistry roleRegistry;

    private final Map<ResourceType, PermissionEvaluator> evaluators = new ConcurrentHashMap<>();

    // Cache: ConcurrentHashMap for thread safety (REST requests are concurrent).
    // Key format: "username\0RESOURCE_TYPE\0resourceId\0action" (NUL separator — cannot appear in any field).
    // Bounded: cleared entirely when exceeding MAX_CACHE_SIZE to prevent unbounded growth.
    // Also cleared on: config change, role registry change, evaluator add/remove.
    private final Map<String, Boolean> permissionCache = new ConcurrentHashMap<>();

    private final RegistryChangeListener<RoleDefinition> registryChangeListener = new RegistryChangedRunnableListener<>(
            () -> permissionCache.clear());

    private volatile boolean enabled = false;

    @Activate
    public AuthorizationServiceImpl(@Reference RoleRegistry roleRegistry) {
        this.roleRegistry = roleRegistry;
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        roleRegistry.addRegistryChangeListener(registryChangeListener);
        modified(config);
    }

    @Modified
    protected void modified(@Nullable Map<String, Object> properties) {
        if (properties != null) {
            enabled = ConfigParser.valueAsOrElse(properties.get(CONFIG_ENABLED), Boolean.class, false);
            permissionCache.clear();
        }
    }

    @Deactivate
    protected void deactivate() {
        roleRegistry.removeRegistryChangeListener(registryChangeListener);
        permissionCache.clear();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addPermissionEvaluator(PermissionEvaluator evaluator) {
        evaluators.put(evaluator.getResourceType(), evaluator);
        permissionCache.clear();
    }

    protected void removePermissionEvaluator(PermissionEvaluator evaluator) {
        evaluators.remove(evaluator.getResourceType());
        permissionCache.clear();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean hasPermission(Authentication auth, ResourceType type, String resourceId, Action action) {
        if (!enabled) {
            return true;
        }
        if (auth.getRoles().contains(Role.ADMIN)) {
            return true;
        }
        String cacheKey = buildCacheKey(auth.getUsername(), type, resourceId, action);
        Boolean cached = permissionCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        boolean result = evaluatePermission(auth, type, resourceId, action);
        if (permissionCache.size() >= MAX_CACHE_SIZE) {
            permissionCache.clear();
        }
        permissionCache.put(cacheKey, result);
        return result;
    }

    @Override
    public <T> Collection<T> filterAuthorized(Authentication auth, Collection<T> resources, ResourceType type,
            Action action, Function<T, String> idExtractor) {
        if (!enabled) {
            return resources;
        }
        if (auth.getRoles().contains(Role.ADMIN)) {
            return resources;
        }
        return resources.stream().filter(r -> hasPermission(auth, type, idExtractor.apply(r), action)).toList();
    }

    private String buildCacheKey(String username, ResourceType type, String resourceId, Action action) {
        return username + "\0" + type.name() + "\0" + resourceId + "\0" + action.name();
    }

    private boolean evaluatePermission(Authentication auth, ResourceType type, String resourceId, Action action) {
        PermissionEvaluator evaluator = evaluators.get(type);
        if (evaluator == null) {
            logger.debug("Access denied for user '{}': no PermissionEvaluator registered for resource type {}",
                    auth.getUsername(), type);
            return false;
        }
        for (String roleName : auth.getRoles()) {
            RoleDefinition roleDef = roleRegistry.get(roleName);
            if (roleDef == null) {
                logger.trace("Skipping unknown role '{}' for user '{}'", roleName, auth.getUsername());
                continue;
            }
            for (Permission permission : roleDef.getPermissions()) {
                if (permission.resourceType() == type && evaluator.evaluate(permission, resourceId, action)) {
                    return true;
                }
            }
        }
        logger.debug("Access denied for user '{}': no permission grants {} on {} '{}'", auth.getUsername(),
                action.name(), type, resourceId);
        return false;
    }
}
