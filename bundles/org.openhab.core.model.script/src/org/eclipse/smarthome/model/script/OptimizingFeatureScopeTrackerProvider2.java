/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.model.script;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.typesystem.internal.FeatureScopeTracker;
import org.eclipse.xtext.xbase.typesystem.internal.IFeatureScopeTracker;
import org.eclipse.xtext.xbase.typesystem.internal.OptimizingFeatureScopeTrackerProvider;

/**
 * {@link OptimizingFeatureScopeTrackerProvider} implementation
 *
 * ...with a workaround for https://github.com/eclipse/xtext-extras/issues/144
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class OptimizingFeatureScopeTrackerProvider2 extends OptimizingFeatureScopeTrackerProvider {

    @Override
    public IFeatureScopeTracker track(EObject root) {
        return new FeatureScopeTracker() {
        };
    }

}
