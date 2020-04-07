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
package org.openhab.core.automation;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.dto.RuleDTO;
import org.openhab.core.automation.dto.RuleDTOMapper;
import org.openhab.core.common.registry.AbstractManagedProvider;
import org.openhab.core.storage.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implementation of a rule provider that uses the storage service for persistence
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Ana Dimova - Persistence implementation
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 * @author Markus Rathgeb - fix mapping between element and persistable element
 */
@NonNullByDefault
@Component(service = { RuleProvider.class, ManagedRuleProvider.class })
public class ManagedRuleProvider extends AbstractManagedProvider<Rule, String, RuleDTO> implements RuleProvider {

    @Activate
    public ManagedRuleProvider(final @Reference StorageService storageService) {
        super(storageService);
    }

    @Override
    protected String getStorageName() {
        return "automation_rules";
    }

    @Override
    protected String keyToString(String key) {
        return key;
    }

    @Override
    protected @Nullable Rule toElement(String key, RuleDTO persistableElement) {
        return RuleDTOMapper.map(persistableElement);
    }

    @Override
    protected RuleDTO toPersistableElement(Rule element) {
        return RuleDTOMapper.map(element);
    }
}
