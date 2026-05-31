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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistry;
import org.openhab.core.voice.text.interpreter.llm.LLMToolRegistryListener;
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
public class LLMToolRegistryImpl implements LLMToolRegistry {

    private final Logger logger = LoggerFactory.getLogger(LLMToolRegistryImpl.class);

    private final Map<String, LLMTool> llmTools = new ConcurrentHashMap<>();
    private final Set<LLMToolRegistryListener> listeners = new CopyOnWriteArraySet<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addLLMTool(LLMTool llmTool) {
        this.llmTools.put(llmTool.getId(), llmTool);
        listeners.forEach(l -> l.onLLMToolAdded(llmTool));
    }

    protected void removeLLMTool(LLMTool llmTool) {
        this.llmTools.remove(llmTool.getId());
        listeners.forEach(l -> l.onLLMToolRemoved(llmTool));
    }

    @Override
    public List<LLMTool> getLLMToolsByIds(List<String> ids) {
        List<LLMTool> tools = new ArrayList<>();
        for (String id : ids) {
            LLMTool tool = llmTools.get(id);
            if (tool == null) {
                logger.warn("LLMTool '{}' not available!", id);
            } else {
                tools.add(tool);
            }
        }
        return tools;
    }

    @Override
    public Collection<LLMTool> getLLMTools() {
        return Collections.unmodifiableCollection(llmTools.values());
    }

    @Override
    public @Nullable LLMTool getLLMTool(String id) {
        return llmTools.get(id);
    }

    @Override
    public void addLLMToolRegistryListener(LLMToolRegistryListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeLLMToolRegistryListener(LLMToolRegistryListener listener) {
        listeners.remove(listener);
    }
}
