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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.RegistryChangeListener;
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
public class LLMToolRegistryImpl implements LLMToolRegistry {
    private final Logger logger = LoggerFactory.getLogger(LLMToolRegistryImpl.class);

    private final Map<String, LLMTool> llmTools = new ConcurrentHashMap<>();
    private final Set<RegistryChangeListener<LLMTool>> listeners = new CopyOnWriteArraySet<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addLLMTool(LLMTool llmTool) {
        this.llmTools.put(llmTool.getUID(), llmTool);
        listeners.forEach(l -> l.added(llmTool));
    }

    protected void removeLLMTool(LLMTool llmTool) {
        this.llmTools.remove(llmTool.getUID());
        listeners.forEach(l -> l.removed(llmTool));
    }

    @Override
    public List<LLMTool> getByIds(List<String> ids) {
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
    public Collection<LLMTool> getAll() {
        return new HashSet<>(llmTools.values());
    }

    @Override
    public Stream<LLMTool> stream() {
        return getAll().stream();
    }

    @Override
    public @Nullable LLMTool get(String id) {
        return llmTools.get(id);
    }

    @Override
    public void addRegistryChangeListener(RegistryChangeListener<LLMTool> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeRegistryChangeListener(RegistryChangeListener<LLMTool> listener) {
        listeners.remove(listener);
    }

    @Override
    public LLMTool add(LLMTool element) {
        throw new UnsupportedOperationException("LLMToolRegistry does not support adding elements manually.");
    }

    @Override
    public @Nullable LLMTool update(LLMTool element) {
        throw new UnsupportedOperationException("LLMToolRegistry does not support updating elements manually.");
    }

    @Override
    public @Nullable LLMTool remove(String key) {
        throw new UnsupportedOperationException("LLMToolRegistry does not support removing elements manually.");
    }
}
