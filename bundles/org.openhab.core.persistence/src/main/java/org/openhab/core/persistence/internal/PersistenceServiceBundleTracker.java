/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.persistence.internal;

import java.util.Dictionary;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.persistence.PersistenceService;
import org.openhab.core.service.AbstractServiceBundleTracker;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link PersistenceServiceBundleTracker} tracks bundles that provide {@link PersistenceService} and sets the
 * {@link #READY_MARKER} when all registered bundles are active
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class PersistenceServiceBundleTracker extends AbstractServiceBundleTracker {
    public static final ReadyMarker READY_MARKER = new ReadyMarker("persistence", "services");

    @Activate
    public PersistenceServiceBundleTracker(final @Reference ReadyService readyService, BundleContext bc) {
        super(readyService, bc, READY_MARKER);
    }

    @Override
    protected boolean isRelevantBundle(Bundle bundle) {
        Dictionary<String, String> headers = bundle.getHeaders();
        String provideCapability = headers.get("Provide-Capability");
        return provideCapability != null && provideCapability.contains(PersistenceService.class.getName());
    }
}
