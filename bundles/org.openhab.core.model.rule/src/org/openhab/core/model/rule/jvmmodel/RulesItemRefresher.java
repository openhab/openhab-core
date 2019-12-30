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
package org.openhab.core.model.rule.jvmmodel;

import java.util.Collection;

import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.ItemRegistryChangeListener;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link RulesItemRefresher} is responsible for reloading rules resources every time an item is added or removed.
 *
 * @author Oliver Libutzki - Initial contribution
 * @author Kai Kreuzer - added delayed execution
 * @author Maoliang Huang - refactor
 */
@Component(service = {})
public class RulesItemRefresher extends RulesRefresher implements ItemRegistryChangeListener {

    private ItemRegistry itemRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        this.itemRegistry.addRegistryChangeListener(this);
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry.removeRegistryChangeListener(this);
        this.itemRegistry = null;
    }

    @Override
    public void added(Item element) {
        scheduleRuleRefresh();
    }

    @Override
    public void removed(Item element) {
        scheduleRuleRefresh();
    }

    @Override
    public void updated(Item oldElement, Item element) {

    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        scheduleRuleRefresh();
    }

    @Override
    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
    public void setModelRepository(ModelRepository modelRepository) {
        super.setModelRepository(modelRepository);
    }

    @Override
    public void unsetModelRepository(ModelRepository modelRepository) {
        super.unsetModelRepository(modelRepository);
    }

    @Override
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addActionService(ActionService actionService) {
        super.addActionService(actionService);
    }

    @Override
    protected void removeActionService(ActionService actionService) {
        super.removeActionService(actionService);
    }

}
