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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.Action;
import org.openhab.core.auth.Permission;
import org.openhab.core.auth.PermissionEvaluator;
import org.openhab.core.auth.ResourceType;
import org.osgi.service.component.annotations.Component;

/**
 * No-op {@link PermissionEvaluator} for {@link ResourceType#SITEMAP}. Always allows access.
 * This stub will be replaced by a real evaluator in Phase 1.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
@Component(service = PermissionEvaluator.class, immediate = true)
public class NoOpSitemapPermissionEvaluator implements PermissionEvaluator {

    @Override
    public ResourceType getResourceType() {
        return ResourceType.SITEMAP;
    }

    @Override
    public boolean evaluate(Permission permission, String resourceId, Action action) {
        return true;
    }
}
