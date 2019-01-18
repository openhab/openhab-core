/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.model.lazygen;

import org.eclipse.emf.mwe.core.WorkflowContext;
import org.eclipse.emf.mwe.core.issues.Issues;
import org.eclipse.emf.mwe.core.monitor.ProgressMonitor;
import org.eclipse.xtext.generator.Generator;

/**
 *
 * @author Holger Schill, Simon Kaufmann - Initial contribution and API
 *
 */
public class LazyGenerator extends Generator {

    LazyLanguageConfig langConfig = null;

    public void addLazyLanguage(LazyLanguageConfig langConfig) {
        this.langConfig = langConfig;
        super.addLanguage(langConfig);
    }

    @Override
    protected void invokeInternal(WorkflowContext ctx, ProgressMonitor monitor, Issues issues) {
        super.checkConfigurationInternal(issues);
        super.invokeInternal(ctx, monitor, issues);
    }

    @Override
    protected void checkConfigurationInternal(Issues issues) {
    }

}
