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
package org.eclipse.smarthome.model.rule.jvmmodel;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingRegistry;
import org.eclipse.smarthome.core.thing.ThingRegistryChangeListener;
import org.eclipse.smarthome.model.core.ModelRepository;
import org.eclipse.smarthome.model.script.engine.action.ActionService;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link RulesThingRefresher} is responsible for reloading rules resources every time a thing is added or removed.
 *
 * @author Maoliang Huang - Initial contribution
 *
 */
@Component(service = {})
public class RulesThingRefresher extends RulesRefresher implements ThingRegistryChangeListener {

    private ThingRegistry thingRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    public void setThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry = thingRegistry;
        this.thingRegistry.addRegistryChangeListener(this);
    }

    public void unsetThingRegistry(ThingRegistry thingRegistry) {
        this.thingRegistry.removeRegistryChangeListener(this);
        this.thingRegistry = null;
    }

    @Override
    public void added(Thing element) {
        scheduleRuleRefresh();
    }

    @Override
    public void removed(Thing element) {
        scheduleRuleRefresh();
    }

    @Override
    public void updated(Thing oldElement, Thing element) {

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
