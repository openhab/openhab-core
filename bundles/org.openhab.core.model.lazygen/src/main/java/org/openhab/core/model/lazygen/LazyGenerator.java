/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.model.lazygen;

import org.eclipse.emf.mwe.core.WorkflowContext;
import org.eclipse.emf.mwe.core.issues.Issues;
import org.eclipse.emf.mwe.core.monitor.ProgressMonitor;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.generator.Generator;

/**
 *
 * @author Holger Schill, Simon Kaufmann - Initial contribution
 */
@NonNullByDefault
public class LazyGenerator extends Generator {

    @Nullable
    LazyLanguageConfig langConfig = null;

    public void addLazyLanguage(LazyLanguageConfig langConfig) {
        this.langConfig = langConfig;
        super.addLanguage(langConfig);
    }

    @Override
    protected void invokeInternal(@NonNullByDefault({}) WorkflowContext ctx,
            @NonNullByDefault({}) ProgressMonitor monitor, @NonNullByDefault({}) Issues issues) {
        super.checkConfigurationInternal(issues);
        super.invokeInternal(ctx, monitor, issues);
    }

    @Override
    protected void checkConfigurationInternal(@NonNullByDefault({}) Issues issues) {
    }
}
