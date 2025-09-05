/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.dto.SerializationException;

/**
 * {@link AbstractYamlRuleProvider} is an abstract class that contains some methods common for {@link YamlRuleProvider}
 * and {@link YamlRuleTemplateProvider}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractYamlRuleProvider<@NonNull E> extends AbstractProvider<E> {

    /**
     * Extracts and returns the module IDs from any number of {@link Collection}s of either {@link Module}s or
     * {@link YamlModuleDTO}s.
     *
     * @param moduleCollections the collections of {@link Module}s or {@link YamlModuleDTO}s.
     * @return The resulting {@link Set} of IDs.
     */
    protected Set<String> extractModuleIds(@Nullable Collection<?> @Nullable... moduleCollections) {
        Set<String> result = new HashSet<>();
        if (moduleCollections == null || moduleCollections.length == 0) {
            return result;
        }
        String id;
        for (@Nullable
        Collection<?> modules : moduleCollections) {
            if (modules != null) {
                for (Object object : modules) {
                    id = null;
                    if (object instanceof Module module) {
                        id = module.getId();
                    } else if (object instanceof YamlModuleDTO moduleDto) {
                        id = moduleDto.id;
                    }
                    if (id != null && !id.isBlank()) {
                        result.add(id);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Creates a {@link List} of modules of the specified class from a {@link List} of module DTOs. Modules without an
     * ID will be assigned a unique generated ID.
     *
     * @param <T> the type of the resulting {@link Module}s.
     * @param <D> the type of the supplied DTOs.
     * @param dtos the {@link List} of DTOs.
     * @param otherModuleIds the {@link Set} of {@link String}s containing already in-use module IDs.
     * @param targetClazz the {@link Module} class to create.
     * @return The {@link List} of created {@link Module}s.
     * @throws SerializationException If the mapping fails.
     */
    @SuppressWarnings("unchecked")
    protected <T extends Module, D extends YamlModuleDTO> List<T> mapModules(List<D> dtos, Set<String> otherModuleIds,
            Class<T> targetClazz) throws SerializationException {
        List<T> modules = new ArrayList<>(dtos.size());
        int id = 0;
        boolean generateIds = dtos.stream().anyMatch(m -> m.id == null || m.id.isBlank());
        while (generateIds) {
            for (;;) {
                String ids = Integer.toString(++id);
                if (!dtos.stream().anyMatch(m -> ids.equals(m.id))
                        && (otherModuleIds == null || !otherModuleIds.stream().anyMatch(i -> ids.equals(i)))) {
                    break;
                }
            }
            final String ids2 = Integer.toString(id);
            dtos.stream().filter(m -> m.id == null || m.id.isBlank()).findFirst().ifPresent(m -> m.id = ids2);
            generateIds = dtos.stream().anyMatch(m -> m.id == null || m.id.isBlank());
        }

        for (D dto : dtos) {
            try {
                if (targetClazz.isAssignableFrom(Condition.class) && dto instanceof YamlConditionDTO cDto) {
                    modules.add((T) ModuleBuilder.createCondition().withId(dto.id).withTypeUID(dto.type)
                            .withConfiguration(new Configuration(dto.config)).withInputs(cDto.inputs)
                            .withLabel(dto.label).withDescription(dto.description).build());
                } else if (targetClazz.isAssignableFrom(Action.class) && dto instanceof YamlActionDTO aDto) {
                    modules.add((T) ModuleBuilder.createAction().withId(dto.id).withTypeUID(dto.type)
                            .withConfiguration(new Configuration(dto.config)).withInputs(aDto.inputs)
                            .withLabel(dto.label).withDescription(dto.description).build());
                } else if (targetClazz.isAssignableFrom(Trigger.class)) {
                    modules.add((T) ModuleBuilder.createTrigger().withId(dto.id).withTypeUID(dto.type)
                            .withConfiguration(new Configuration(dto.config)).withLabel(dto.label)
                            .withDescription(dto.description).build());
                } else {
                    throw new IllegalArgumentException("Invalid combination of target and dto classes: "
                            + targetClazz.getSimpleName() + " <-> " + dto.getClass().getSimpleName());
                }
            } catch (RuntimeException e) {
                throw new SerializationException("Failed to process YAML rule or rule template "
                        + targetClazz.getSimpleName() + ": \"" + dto + "\": " + e.getMessage(), e);
            }
        }

        return modules;
    }
}
