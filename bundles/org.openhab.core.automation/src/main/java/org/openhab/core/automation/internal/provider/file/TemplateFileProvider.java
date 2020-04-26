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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.RuleTemplateProvider;
import org.openhab.core.automation.template.TemplateProvider;

/**
 * This class is implementation of {@link TemplateProvider}. It extends functionality of {@link AbstractFileProvider}
 * for importing the {@link RuleTemplate}s from local files.
 *
 * @author Ana Dimova - Initial contribution
 */
@NonNullByDefault
public abstract class TemplateFileProvider extends AbstractFileProvider<RuleTemplate> implements RuleTemplateProvider {

    public TemplateFileProvider() {
        super("templates");
    }

    @Override
    protected String getUID(RuleTemplate providedObject) {
        return providedObject.getUID();
    }

    @Override
    public Collection<RuleTemplate> getAll() {
        return getTemplates(null);
    }

    @Override
    public @Nullable RuleTemplate getTemplate(String UID, @Nullable Locale locale) {
        return providedObjectsHolder.get(UID);
    }

    @Override
    public Collection<RuleTemplate> getTemplates(@Nullable Locale locale) {
        Collection<RuleTemplate> values = providedObjectsHolder.values();
        if (values.isEmpty()) {
            return Collections.emptyList();
        }
        return new LinkedList<>(values);
    }
}
