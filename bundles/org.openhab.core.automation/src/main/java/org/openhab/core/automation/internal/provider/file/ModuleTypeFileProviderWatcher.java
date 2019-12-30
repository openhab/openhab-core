/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.automation.internal.provider.file;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class is a wrapper of {@link ModuleTypeProvider}, responsible for initializing the WatchService.
 *
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = ModuleTypeProvider.class)
public class ModuleTypeFileProviderWatcher extends ModuleTypeFileProvider {

    @Override
    protected void initializeWatchService(String watchingDir) {
        WatchServiceUtil.initializeWatchService(watchingDir, this);
    }

    @Override
    protected void deactivateWatchService(String watchingDir) {
        WatchServiceUtil.deactivateWatchService(watchingDir, this);
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(parser.type=parser.module.type)")
    @Override
    public void addParser(Parser<ModuleType> parser, Map<String, String> properties) {
        super.addParser(parser, properties);
    }

    @Override
    public void removeParser(Parser<ModuleType> parser, Map<String, String> properties) {
        super.removeParser(parser, properties);
    }
}
