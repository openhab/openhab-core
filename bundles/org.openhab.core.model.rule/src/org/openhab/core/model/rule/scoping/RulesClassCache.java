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
package org.openhab.core.model.rule.scoping;

import java.util.HashMap;

import org.eclipse.xtext.common.types.access.impl.Primitives;
import org.openhab.core.model.script.engine.action.ActionService;
import org.openhab.core.thing.binding.ThingActions;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class cache used by the {@link RulesClassFinder} for resolving classes in DSL rules.
 *
 * It allows for removing and updating classes in the cache when add-ons are installed or updated.
 *
 * @author Wouter Born - Initial contribution
 */
@Component
public class RulesClassCache extends HashMap<String, Class<?>> {

    private static final long serialVersionUID = 1L;

    private static RulesClassCache instance;

    private final Logger logger = LoggerFactory.getLogger(RulesClassCache.class);

    @Activate
    public RulesClassCache() {
        super(500);
        for (Class<?> primitiveType : Primitives.ALL_PRIMITIVE_TYPES) {
            put(primitiveType.getName(), primitiveType);
        }

        if (instance != null) {
            throw new IllegalStateException("RulesClassCache should only be activated once!");
        }
        instance = this;
    }

    @Deactivate
    public void deactivate() {
        clear();
        instance = null;
    }

    public static RulesClassCache getInstance() {
        return instance;
    }

    private void updateCacheEntry(Object object) {
        String key = object.getClass().getName();
        put(key, object.getClass());
        logger.debug("Updated cache entry: {}", key);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addActionService(ActionService actionService) {
        updateCacheEntry(actionService);
    }

    public void removeActionService(ActionService actionService) {
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addThingActions(ThingActions thingActions) {
        updateCacheEntry(thingActions);
    }

    public void removeThingActions(ThingActions thingActions) {
    }
}
