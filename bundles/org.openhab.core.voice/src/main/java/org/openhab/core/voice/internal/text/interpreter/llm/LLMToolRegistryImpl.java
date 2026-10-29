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
package org.openhab.core.voice.internal.text.interpreter.llm;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.voice.internal.AbstractVoiceServiceRegistry;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link LLMToolRegistryImpl} is the implementation of the {@link LLMToolRegistry}.
 *
 * @author Florian Hotze - Initial contribution
 */
@Component(service = LLMToolRegistry.class, immediate = true)
@NonNullByDefault
public class LLMToolRegistryImpl extends AbstractVoiceServiceRegistry<LLMTool> implements LLMToolRegistry {
    private final Logger logger = LoggerFactory.getLogger(LLMToolRegistryImpl.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addLLMTool(LLMTool llmTool) {
        addService(llmTool);
    }

    protected void removeLLMTool(LLMTool llmTool) {
        removeService(llmTool);
    }

    @Override
    public List<LLMTool> getByIds(List<String> ids) {
        if (ids.contains("*")) {
            return stream().toList();
        }

        List<LLMTool> tools = new ArrayList<>();
        for (String id : ids) {
            LLMTool tool = services.get(id);
            if (tool == null) {
                logger.warn("LLMTool '{}' not available!", id);
            } else {
                tools.add(tool);
            }
        }
        return tools;
    }
}
