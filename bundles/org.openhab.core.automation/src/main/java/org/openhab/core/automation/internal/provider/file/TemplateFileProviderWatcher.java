/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import org.openhab.core.automation.Module;
import org.openhab.core.automation.parser.Parser;
import org.openhab.core.automation.parser.ValidationException;
import org.openhab.core.automation.parser.ValidationException.ObjectType;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.RuleTemplateProvider;
import org.openhab.core.automation.template.TemplateProvider;
import org.openhab.core.service.WatchService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This class is a wrapper of multiple {@link TemplateProvider}s, responsible for initializing the WatchService.
 *
 * @author Ana Dimova - Initial contribution
 * @author Arne Seime - Added template validation support
 */
@NonNullByDefault
@Component(immediate = true, service = RuleTemplateProvider.class)
public class TemplateFileProviderWatcher extends TemplateFileProvider {

    private final WatchService watchService;

    @Activate
    public TemplateFileProviderWatcher(
            @Reference(target = WatchService.CONFIG_WATCHER_FILTER) WatchService watchService) {
        this.watchService = watchService;
    }

    @Override
    protected void initializeWatchService(String watchingDir) {
        WatchServiceUtil.initializeWatchService(watchingDir, this, watchService);
    }

    @Override
    protected void deactivateWatchService(String watchingDir) {
        WatchServiceUtil.deactivateWatchService(watchingDir, this);
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target = "(parser.type=parser.template)")
    @Override
    public void addParser(Parser<RuleTemplate> parser, Map<String, String> properties) {
        super.addParser(parser, properties);
    }

    @Override
    public void removeParser(Parser<RuleTemplate> parser, Map<String, String> properties) {
        super.removeParser(parser, properties);
    }

    @SuppressWarnings("null")
    @Override
    protected void validateObject(RuleTemplate template) throws ValidationException {
        String s;
        if ((s = template.getUID()) == null || s.isBlank()) {
            throw new ValidationException(ObjectType.TEMPLATE, null, "UID cannot be blank");
        }
        if ((s = template.getLabel()) == null || s.isBlank()) {
            throw new ValidationException(ObjectType.TEMPLATE, template.getUID(), "Label cannot be blank");
        }
        if (template.getModules(Module.class).isEmpty()) {
            throw new ValidationException(ObjectType.TEMPLATE, template.getUID(), "There must be at least one module");
        }
    }
}
