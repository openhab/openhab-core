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
package org.eclipse.smarthome.automation.core;

import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleProvider;
import org.eclipse.smarthome.automation.core.dto.RuleDTOMapper;
import org.eclipse.smarthome.automation.dto.RuleDTO;
import org.eclipse.smarthome.core.common.registry.AbstractManagedProvider;
import org.eclipse.smarthome.core.storage.StorageService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Implementation of a rule provider that uses the storage service for persistence
 *
 * @author Yordan Mihaylov - Initial Contribution
 * @author Ana Dimova - Persistence implementation
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 * @author Markus Rathgeb - fix mapping between element and persistable element
 */
@Component(service = { RuleProvider.class, ManagedRuleProvider.class })
public class ManagedRuleProvider extends AbstractManagedProvider<Rule, String, RuleDTO> implements RuleProvider {

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC)
    @Override
    protected void setStorageService(final StorageService storageService) {
        super.setStorageService(storageService);
    }

    @Override
    protected void unsetStorageService(final StorageService storageService) {
        super.unsetStorageService(storageService);
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
    protected Rule toElement(String key, RuleDTO persistableElement) {
        return RuleDTOMapper.map(persistableElement);
    }

    @Override
    protected RuleDTO toPersistableElement(Rule element) {
        return RuleDTOMapper.map(element);
    }
}
