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
package org.openhab.core.automation.module.script.internal;

import java.util.Collection;
import java.util.Dictionary;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.module.script.ScriptEngineFactory;
import org.openhab.core.service.BaseServiceBundleTracker;
import org.openhab.core.service.ReadyMarker;
import org.openhab.core.service.ReadyService;
import org.openhab.core.service.StartLevelService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ScriptEngineFactoryBundleTracker} tracks bundles that provide {@link ScriptEngineFactory} and sets the
 * {@link #READY_MARKER} when all registered bundles are active
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class ScriptEngineFactoryBundleTracker extends BaseServiceBundleTracker {
    public static final ReadyMarker READY_MARKER = new ReadyMarker("automation", "scriptEngineFactories");

    @Activate
    public ScriptEngineFactoryBundleTracker(final @Reference ReadyService readyService, BundleContext bc) {
        super(readyService, READY_MARKER, bc);
    }

    @Override
    protected int minimumStartLevel() {
        return StartLevelService.STARTLEVEL_MODEL;
    }

    @Override
    protected boolean isRelevantBundle(Bundle bundle) {
        Dictionary<String, String> headers = bundle.getHeaders();
        String provideCapability = headers.get("Provide-Capability");
        return provideCapability != null && provideCapability.contains(ScriptEngineFactory.class.getName());
    }

    @Override
    protected boolean evaluateReady() {
        boolean ready = super.evaluateReady();
        if (ready) {
            try {
                Collection<ServiceReference<ScriptEngineFactory>> refs = context
                        .getServiceReferences(ScriptEngineFactory.class, null);
                for (ServiceReference<ScriptEngineFactory> ref : refs) {
                    ScriptEngineFactory engineFactory = context.getService(ref);
                    try {
                        if (!engineFactory.isReady()) {
                            return false;
                        }
                    } finally {
                        context.ungetService(ref);
                    }
                }
            } catch (InvalidSyntaxException e) {
                // Cannot happen when the filter is null
            }
        }
        return ready;
    }
}
