/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.voice.internal.text;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.common.registry.AbstractServiceRegistry;
import org.openhab.core.voice.text.HumanLanguageInterpreter;
import org.openhab.core.voice.text.HumanLanguageInterpreterRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HumanLanguageInterpreterRegistryImpl} is the implementation of the
 * {@link HumanLanguageInterpreterRegistry}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(immediate = true)
@NonNullByDefault
public class HumanLanguageInterpreterRegistryImpl extends AbstractServiceRegistry<HumanLanguageInterpreter, String>
        implements HumanLanguageInterpreterRegistry {
    private final Logger logger = LoggerFactory.getLogger(HumanLanguageInterpreterRegistryImpl.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addHumanLanguageInterpreter(HumanLanguageInterpreter humanLanguageInterpreter) {
        addService(humanLanguageInterpreter);
    }

    protected void removeHumanLanguageInterpreter(HumanLanguageInterpreter humanLanguageInterpreter) {
        removeService(humanLanguageInterpreter);
    }

    @Override
    public List<HumanLanguageInterpreter> getByIds(List<String> ids) {
        List<HumanLanguageInterpreter> interpreters = new ArrayList<>();
        for (String id : ids) {
            HumanLanguageInterpreter interpreter = services.get(id);
            if (interpreter == null) {
                logger.warn("HumanLanguageInterpreter '{}' not available!", id);
            } else {
                interpreters.add(interpreter);
            }
        }
        return interpreters;
    }
}
