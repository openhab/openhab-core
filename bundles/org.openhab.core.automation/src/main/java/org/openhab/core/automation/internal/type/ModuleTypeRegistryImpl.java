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
package org.openhab.core.automation.internal.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.CompositeActionType;
import org.openhab.core.automation.type.CompositeConditionType;
import org.openhab.core.automation.type.CompositeTriggerType;
import org.openhab.core.automation.type.ConditionType;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.automation.type.ModuleTypeRegistry;
import org.openhab.core.automation.type.TriggerType;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.osgi.service.component.annotations.Component;

/**
 * The implementation of {@link ModuleTypeRegistry} that is registered as a service.
 *
 * @author Yordan Mihaylov - Initial contribution
 * @author Kai Kreuzer - refactored (managed) provider and registry implementation
 */
@NonNullByDefault
@Component(service = ModuleTypeRegistry.class, immediate = true)
public class ModuleTypeRegistryImpl extends AbstractRegistry<ModuleType, String, ModuleTypeProvider>
        implements ModuleTypeRegistry {

    public ModuleTypeRegistryImpl() {
        super(ModuleTypeProvider.class);
    }

    @Override
    protected void addProvider(Provider<ModuleType> provider) {
        if (provider instanceof ModuleTypeProvider) {
            super.addProvider(provider);
        }
    }

    @Override
    public @Nullable ModuleType get(String typeUID) {
        return get(typeUID, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ModuleType> @Nullable T get(String moduleTypeUID, @Nullable Locale locale) {
        Entry<Provider<ModuleType>, ModuleType> mType = getValueAndProvider(moduleTypeUID);
        if (mType == null) {
            return null;
        } else {
            ModuleType mt = locale == null ? mType.getValue()
                    : ((ModuleTypeProvider) mType.getKey()).getModuleType(mType.getValue().getUID(), locale);
            return (T) createCopy(mt);
        }
    }

    @Override
    public <T extends ModuleType> Collection<T> getByTag(@Nullable String moduleTypeTag) {
        return getByTag(moduleTypeTag, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ModuleType> Collection<T> getByTag(@Nullable String moduleTypeTag, @Nullable Locale locale) {
        Collection<T> result = new ArrayList<>(20);
        forEach((provider, mType) -> {
            ModuleType mt = locale == null ? mType
                    : ((ModuleTypeProvider) provider).getModuleType(mType.getUID(), locale);
            if (mt != null && (moduleTypeTag == null || mt.getTags().contains(moduleTypeTag))) {
                @Nullable
                T mtCopy = (T) createCopy(mt);
                if (mtCopy != null) {
                    result.add(mtCopy);
                }
            }
        });
        return result;
    }

    @Override
    public <T extends ModuleType> Collection<T> getByTags(String... tags) {
        return getByTags(null, tags);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ModuleType> Collection<T> getByTags(@Nullable Locale locale, String... tags) {
        Set<String> tagSet = new HashSet<>(Arrays.asList(tags));
        Collection<T> result = new ArrayList<>(20);
        forEach((provider, mType) -> {
            ModuleType mt = locale == null ? mType
                    : ((ModuleTypeProvider) provider).getModuleType(mType.getUID(), locale);
            if (mt != null && (mt.getTags().containsAll(tagSet))) {
                @Nullable
                T mtCopy = (T) createCopy(mt);
                if (mtCopy != null) {
                    result.add(mtCopy);
                }
            }
        });
        return result;
    }

    @Override
    public Collection<TriggerType> getTriggers(@Nullable Locale locale, String... tags) {
        Collection<ModuleType> moduleTypes = getByTags(locale, tags);
        Collection<TriggerType> triggerTypes = new ArrayList<>();
        for (ModuleType mt : moduleTypes) {
            if (mt instanceof TriggerType) {
                triggerTypes.add((TriggerType) mt);
            }
        }
        return triggerTypes;
    }

    @Override
    public Collection<TriggerType> getTriggers(String... tags) {
        Collection<ModuleType> moduleTypes = getByTags(tags);
        Collection<TriggerType> triggerTypes = new ArrayList<>();
        for (ModuleType mt : moduleTypes) {
            if (mt instanceof TriggerType) {
                triggerTypes.add((TriggerType) mt);
            }
        }
        return triggerTypes;
    }

    @Override
    public Collection<ConditionType> getConditions(String... tags) {
        Collection<ModuleType> moduleTypes = getByTags(tags);
        Collection<ConditionType> conditionTypes = new ArrayList<>();
        for (ModuleType mt : moduleTypes) {
            if (mt instanceof ConditionType) {
                conditionTypes.add((ConditionType) mt);
            }
        }
        return conditionTypes;
    }

    @Override
    public Collection<ConditionType> getConditions(@Nullable Locale locale, String... tags) {
        Collection<ModuleType> moduleTypes = getByTags(locale, tags);
        Collection<ConditionType> conditionTypes = new ArrayList<>();
        for (ModuleType mt : moduleTypes) {
            if (mt instanceof ConditionType) {
                conditionTypes.add((ConditionType) mt);
            }
        }
        return conditionTypes;
    }

    @Override
    public Collection<ActionType> getActions(String... tags) {
        Collection<ModuleType> moduleTypes = getByTags(tags);
        Collection<ActionType> actionTypes = new ArrayList<>();
        for (ModuleType mt : moduleTypes) {
            if (mt instanceof ActionType) {
                actionTypes.add((ActionType) mt);
            }
        }
        return actionTypes;
    }

    @Override
    public Collection<ActionType> getActions(@Nullable Locale locale, String... tags) {
        Collection<ModuleType> moduleTypes = getByTags(locale, tags);
        Collection<ActionType> actionTypes = new ArrayList<>();
        for (ModuleType mt : moduleTypes) {
            if (mt instanceof ActionType) {
                actionTypes.add((ActionType) mt);
            }
        }
        return actionTypes;
    }

    private @Nullable ModuleType createCopy(@Nullable ModuleType mType) {
        if (mType == null) {
            return null;
        }
        ModuleType result;
        if (mType instanceof CompositeTriggerType) {
            CompositeTriggerType m = (CompositeTriggerType) mType;
            result = new CompositeTriggerType(mType.getUID(), mType.getConfigurationDescriptions(), mType.getLabel(),
                    mType.getDescription(), mType.getTags(), mType.getVisibility(), m.getOutputs(),
                    new ArrayList<>(m.getChildren()));

        } else if (mType instanceof TriggerType) {
            TriggerType m = (TriggerType) mType;
            result = new TriggerType(mType.getUID(), mType.getConfigurationDescriptions(), mType.getLabel(),
                    mType.getDescription(), mType.getTags(), mType.getVisibility(), m.getOutputs());

        } else if (mType instanceof CompositeConditionType) {
            CompositeConditionType m = (CompositeConditionType) mType;
            result = new CompositeConditionType(mType.getUID(), mType.getConfigurationDescriptions(), mType.getLabel(),
                    mType.getDescription(), mType.getTags(), mType.getVisibility(), m.getInputs(),
                    new ArrayList<>(m.getChildren()));

        } else if (mType instanceof ConditionType) {
            ConditionType m = (ConditionType) mType;
            result = new ConditionType(mType.getUID(), mType.getConfigurationDescriptions(), mType.getLabel(),
                    mType.getDescription(), mType.getTags(), mType.getVisibility(), m.getInputs());

        } else if (mType instanceof CompositeActionType) {
            CompositeActionType m = (CompositeActionType) mType;
            result = new CompositeActionType(mType.getUID(), mType.getConfigurationDescriptions(), mType.getLabel(),
                    mType.getDescription(), mType.getTags(), mType.getVisibility(), m.getInputs(), m.getOutputs(),
                    new ArrayList<>(m.getChildren()));

        } else if (mType instanceof ActionType) {
            ActionType m = (ActionType) mType;
            result = new ActionType(mType.getUID(), mType.getConfigurationDescriptions(), mType.getLabel(),
                    mType.getDescription(), mType.getTags(), mType.getVisibility(), m.getInputs(), m.getOutputs());
        } else {
            throw new IllegalArgumentException("Invalid template type:" + mType);
        }
        return result;
    }

}
