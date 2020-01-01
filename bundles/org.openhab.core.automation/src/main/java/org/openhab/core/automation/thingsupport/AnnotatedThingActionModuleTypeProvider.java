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
package org.openhab.core.automation.thingsupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.handler.BaseModuleHandlerFactory;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.handler.ModuleHandlerFactory;
import org.openhab.core.automation.internal.module.handler.AnnotationActionHandler;
import org.openhab.core.automation.module.provider.AnnotationActionModuleTypeHelper;
import org.openhab.core.automation.module.provider.ModuleInformation;
import org.openhab.core.automation.module.provider.i18n.ModuleTypeI18nService;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * ModuleTypeProvider that collects actions for {@link ThingHandler}s
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
@Component(service = { ModuleTypeProvider.class, ModuleHandlerFactory.class })
public class AnnotatedThingActionModuleTypeProvider extends BaseModuleHandlerFactory implements ModuleTypeProvider {

    private final Collection<ProviderChangeListener<ModuleType>> changeListeners = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<ModuleInformation>> moduleInformation = new ConcurrentHashMap<>();
    private final AnnotationActionModuleTypeHelper helper = new AnnotationActionModuleTypeHelper();

    private @NonNullByDefault({}) ModuleTypeI18nService moduleTypeI18nService;

    @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    public void addAnnotatedThingActions(ThingActions annotatedThingActions) {
        if (annotatedThingActions.getClass().isAnnotationPresent(ThingActionsScope.class)) {
            ThingActionsScope scope = annotatedThingActions.getClass().getAnnotation(ThingActionsScope.class);
            Collection<ModuleInformation> moduleInformations = helper.parseAnnotations(scope.name(),
                    annotatedThingActions);

            String thingUID = annotatedThingActions.getThingHandler().getThing().getUID().getAsString();

            for (ModuleInformation mi : moduleInformations) {
                mi.setThingUID(thingUID);

                ModuleType oldType = null;
                if (this.moduleInformation.containsKey(mi.getUID())) {
                    oldType = helper.buildModuleType(mi.getUID(), this.moduleInformation);
                    Set<ModuleInformation> availableModuleConfigs = this.moduleInformation.get(mi.getUID());
                    availableModuleConfigs.add(mi);
                } else {
                    Set<ModuleInformation> configs = ConcurrentHashMap.newKeySet();
                    configs.add(mi);
                    this.moduleInformation.put(mi.getUID(), configs);
                }

                ModuleType mt = helper.buildModuleType(mi.getUID(), this.moduleInformation);
                if (mt != null) {
                    for (ProviderChangeListener<ModuleType> l : changeListeners) {
                        if (oldType != null) {
                            l.updated(this, oldType, mt);
                        } else {
                            l.added(this, mt);
                        }
                    }
                }
            }
        }
    }

    public void removeAnnotatedThingActions(ThingActions annotatedThingActions, Map<String, Object> properties) {
        Collection<ModuleInformation> moduleInformations = helper.parseAnnotations(annotatedThingActions);

        String thingUID = annotatedThingActions.getThingHandler().getThing().getUID().getAsString();

        for (ModuleInformation mi : moduleInformations) {
            mi.setThingUID(thingUID);
            ModuleType oldType = null;

            Set<ModuleInformation> availableModuleConfigs = this.moduleInformation.get(mi.getUID());
            if (availableModuleConfigs != null) {
                if (availableModuleConfigs.size() > 1) {
                    oldType = helper.buildModuleType(mi.getUID(), this.moduleInformation);
                    availableModuleConfigs.remove(mi);
                } else {
                    this.moduleInformation.remove(mi.getUID());
                }

                ModuleType mt = helper.buildModuleType(mi.getUID(), this.moduleInformation);
                // localize moduletype -> remove from map
                for (ProviderChangeListener<ModuleType> l : changeListeners) {
                    if (oldType != null) {
                        l.updated(this, oldType, mt);
                    } else {
                        l.removed(this, mt);
                    }
                }
            }
        }
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        changeListeners.add(listener);
    }

    @Override
    public Collection<ModuleType> getAll() {
        Collection<ModuleType> moduleTypes = new ArrayList<>();
        for (String moduleUID : moduleInformation.keySet()) {
            ModuleType mt = helper.buildModuleType(moduleUID, this.moduleInformation);
            if (mt != null) {
                moduleTypes.add(mt);
            }
        }
        return moduleTypes;
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        changeListeners.remove(listener);
    }

    @Override
    public Collection<String> getTypes() {
        return moduleInformation.keySet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> T getModuleType(String UID, @Nullable Locale locale) {
        return (T) localizeModuleType(UID, locale);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> Collection<T> getModuleTypes(@Nullable Locale locale) {
        List<T> result = new ArrayList<>();
        for (Entry<String, Set<ModuleInformation>> entry : moduleInformation.entrySet()) {
            ModuleType localizedModuleType = localizeModuleType(entry.getKey(), locale);
            if (localizedModuleType != null) {
                result.add((T) localizedModuleType);
            }
        }
        return result;
    }

    private @Nullable ModuleType localizeModuleType(String uid, @Nullable Locale locale) {
        Set<ModuleInformation> mis = moduleInformation.get(uid);
        if (mis != null && !mis.isEmpty()) {
            ModuleInformation mi = mis.iterator().next();

            Bundle bundle = FrameworkUtil.getBundle(mi.getActionProvider().getClass());
            ModuleType mt = helper.buildModuleType(uid, moduleInformation);

            ModuleType localizedModuleType = moduleTypeI18nService.getModuleTypePerLocale(mt, locale, bundle);
            return localizedModuleType;
        }
        return null;
    }

    @Override
    protected @Nullable ModuleHandler internalCreate(Module module, String ruleUID) {
        if (module instanceof Action) {
            Action actionModule = (Action) module;

            if (moduleInformation.containsKey(actionModule.getTypeUID())) {
                ModuleInformation finalMI = helper.getModuleInformationForIdentifier(actionModule, moduleInformation,
                        true);

                if (finalMI != null) {
                    ActionType moduleType = helper.buildModuleType(module.getTypeUID(), this.moduleInformation);
                    return new AnnotationActionHandler(actionModule, moduleType, finalMI.getMethod(),
                            finalMI.getActionProvider());
                }
            }
        }
        return null;
    }

    @Reference
    protected void setModuleTypeI18nService(ModuleTypeI18nService moduleTypeI18nService) {
        this.moduleTypeI18nService = moduleTypeI18nService;
    }

    protected void unsetModuleTypeI18nService(ModuleTypeI18nService moduleTypeI18nService) {
        this.moduleTypeI18nService = null;
    }
}
