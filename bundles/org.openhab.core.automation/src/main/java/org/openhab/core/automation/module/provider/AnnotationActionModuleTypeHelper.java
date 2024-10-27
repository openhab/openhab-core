/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.automation.module.provider;

import static org.openhab.core.automation.internal.module.handler.AnnotationActionHandler.MODULE_RESULT;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.AnnotatedActions;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.ActionOutputs;
import org.openhab.core.automation.annotation.ActionScope;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.automation.type.Output;
import org.openhab.core.automation.util.ActionInputsHelper;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.config.core.ParameterOption;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods for {@link AnnotatedActions} {@link ModuleTypeProvider}
 *
 * @author Stefan Triller - Initial contribution
 * @author Florian Hotze - Added configuration description parameters for thing modules
 * @author Laurent Garnier - Converted into a an OSGi component
 */
@NonNullByDefault
@Component(service = AnnotationActionModuleTypeHelper.class)
public class AnnotationActionModuleTypeHelper {

    private final Logger logger = LoggerFactory.getLogger(AnnotationActionModuleTypeHelper.class);

    private static final String SELECT_SERVICE_LABEL = "Select Service Instance";
    private static final String SELECT_THING_LABEL = "Select Thing";
    public static final String CONFIG_PARAM = "config";

    private final ActionInputsHelper actionInputsHelper;

    @Activate
    public AnnotationActionModuleTypeHelper(final @Reference ActionInputsHelper actionInputsHelper) {
        this.actionInputsHelper = actionInputsHelper;
    }

    public Collection<ModuleInformation> parseAnnotations(Object actionProvider) {
        Class<?> clazz = actionProvider.getClass();
        if (clazz.isAnnotationPresent(ActionScope.class)) {
            ActionScope scope = clazz.getAnnotation(ActionScope.class);
            return parseAnnotations(scope.name(), actionProvider);
        }
        return List.of();
    }

    public Collection<ModuleInformation> parseAnnotations(String name, Object actionProvider) {
        Collection<ModuleInformation> moduleInformation = new ArrayList<>();
        Class<?> clazz = actionProvider.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(RuleAction.class)) {
                List<Input> inputs = getInputsFromAction(method);
                List<Output> outputs = getOutputsFromAction(method);

                RuleAction ruleAction = method.getAnnotation(RuleAction.class);
                String uid = name + "." + method.getName();
                Set<String> tags = new HashSet<>(Arrays.asList(ruleAction.tags()));

                ModuleInformation mi = new ModuleInformation(uid, actionProvider, method);
                mi.setLabel(ruleAction.label());
                mi.setDescription(ruleAction.description());
                mi.setVisibility(ruleAction.visibility());
                mi.setInputs(inputs);
                mi.setOutputs(outputs);
                mi.setTags(tags);

                moduleInformation.add(mi);
            }
        }
        return moduleInformation;
    }

    private List<Input> getInputsFromAction(Method method) {
        List<Input> inputs = new ArrayList<>();

        Annotation[][] annotations = method.getParameterAnnotations();
        Parameter[] params = method.getParameters();

        for (int i = 0; i < annotations.length; i++) {
            Parameter param = params[i];
            Annotation[] paramAnnotations = annotations[i];
            if (paramAnnotations.length == 0) {
                // we do not have an annotation with a name for this parameter
                inputs.add(new Input("p" + i, param.getType().getCanonicalName(), "", "", Set.of(), false, "", ""));
            } else if (paramAnnotations.length == 1) {
                Annotation a = paramAnnotations[0];
                if (a instanceof ActionInput inp) {
                    // check if a type is given, otherwise use the java type specified on the parameter
                    String type;
                    if (!"".equals(inp.type())) {
                        type = inp.type();
                    } else {
                        type = param.getType().getCanonicalName();
                    }

                    inputs.add(new Input(inp.name(), type, inp.label(), inp.description(),
                            Arrays.stream(inp.tags()).collect(Collectors.toSet()), inp.required(), inp.reference(),
                            inp.defaultValue()));
                }
            }
        }
        return inputs;
    }

    private Output getOutputFromActionOutputAnnotation(ActionOutput ruleActionOutput, @Nullable String nameOverride) {
        return new Output((nameOverride != null ? nameOverride : ruleActionOutput.name()), ruleActionOutput.type(),
                ruleActionOutput.label(), ruleActionOutput.description(),
                Arrays.stream(ruleActionOutput.tags()).collect(Collectors.toSet()), ruleActionOutput.reference(),
                ruleActionOutput.defaultValue());
    }

    private List<Output> getOutputsFromAction(Method method) {
        List<Output> outputs = new ArrayList<>();
        // ActionOutputs annotation
        if (method.isAnnotationPresent(ActionOutputs.class)) {
            for (ActionOutput ruleActionOutput : method.getAnnotation(ActionOutputs.class).value()) {
                outputs.add(getOutputFromActionOutputAnnotation(ruleActionOutput, null));
            }
            // no ActionOutputs annotation, but a Map<String, Object> return type
        } else if (method.getAnnotatedReturnType().toString()
                .equals("java.util.Map<java.lang.String, java.lang.Object>")) {
            logger.warn(
                    "Method {}::{} returns a Map<String, Object> but is not annotated with ActionOutputs. This should be fixed in the binding.",
                    method.getDeclaringClass().getSimpleName(), method.getName());
            return outputs;
            // no ActionOutputs annotation and no Map<String, Object> return type, but a single ActionOutput annotation
        } else if (method.isAnnotationPresent(ActionOutput.class)) {
            ActionOutput ruleActionOutput = method.getAnnotation(ActionOutput.class);
            if (!ruleActionOutput.name().equals(MODULE_RESULT)) {
                logger.warn(
                        "Method {}::{} has a single output but does not use the default output name in the ActionOutput annotation. This should be fixed in the binding.",
                        method.getDeclaringClass().getSimpleName(), method.getName());
            }
            outputs.add(getOutputFromActionOutputAnnotation(ruleActionOutput, MODULE_RESULT));
        }
        return outputs;
    }

    public @Nullable ActionType buildModuleType(String uid, Map<String, Set<ModuleInformation>> moduleInformation) {
        Set<ModuleInformation> mis = moduleInformation.get(uid);
        List<ConfigDescriptionParameter> configDescriptions = new ArrayList<>();

        if (mis != null && !mis.isEmpty()) {
            ModuleInformation mi = (ModuleInformation) mis.toArray()[0];

            ActionModuleKind kind = ActionModuleKind.SINGLE;
            if (mi.getConfigName() != null && mi.getThingUID() != null) {
                logger.error(
                        "ModuleType with UID {} has thingUID ({}) and multi-service ({}) property set, ignoring it.",
                        uid, mi.getConfigName(), mi.getThingUID());
                return null;
            } else if (mi.getConfigName() != null) {
                kind = ActionModuleKind.SERVICE;
            } else if (mi.getThingUID() != null) {
                kind = ActionModuleKind.THING;
            }

            ConfigDescriptionParameter configParam = buildConfigParam(mis, kind);
            if (configParam != null) {
                configDescriptions.add(configParam);
            }

            Visibility visibility = mi.getVisibility();

            if (kind == ActionModuleKind.THING) {
                // we have a Thing module, so we have to map the inputs to config description parameters for the UI
                try {
                    List<ConfigDescriptionParameter> inputConfigDescriptions = actionInputsHelper
                            .mapActionInputsToConfigDescriptionParameters(mi.getInputs());
                    configDescriptions.addAll(inputConfigDescriptions);
                } catch (IllegalArgumentException e) {
                    // we have an input without a supported type, so hide the Thing action
                    visibility = Visibility.HIDDEN;
                    logger.debug("{} Thing action {} has an input with an unsupported type, hiding it in the UI.",
                            e.getMessage(), uid);
                }
            }

            return new ActionType(uid, configDescriptions, mi.getLabel(), mi.getDescription(), mi.getTags(), visibility,
                    mi.getInputs(), mi.getOutputs());
        }
        return null;
    }

    private @Nullable ConfigDescriptionParameter buildConfigParam(Set<ModuleInformation> moduleInformations,
            ActionModuleKind kind) {
        List<ParameterOption> options = new ArrayList<>();
        if (kind == ActionModuleKind.SINGLE) {
            if (moduleInformations.size() == 1) {
                if (((ModuleInformation) moduleInformations.toArray()[0]).getConfigName() == null
                        && ((ModuleInformation) moduleInformations.toArray()[0]).getThingUID() == null) {
                    // we have a single service and thus no configuration at all
                    return null;
                }
            }
        } else if (kind == ActionModuleKind.SERVICE) {
            for (ModuleInformation mi : moduleInformations) {
                String configName = mi.getConfigName();
                options.add(new ParameterOption(configName, configName));
            }
            return ConfigDescriptionParameterBuilder.create(CONFIG_PARAM, Type.TEXT).withLabel(SELECT_SERVICE_LABEL)
                    .withOptions(options).withLimitToOptions(true).withRequired(true).build();
        } else if (kind == ActionModuleKind.THING) {
            for (ModuleInformation mi : moduleInformations) {
                String thingUID = mi.getThingUID();
                options.add(new ParameterOption(thingUID, null));
            }
            return ConfigDescriptionParameterBuilder.create(CONFIG_PARAM, Type.TEXT).withLabel(SELECT_THING_LABEL)
                    .withContext("thing").withOptions(options).withLimitToOptions(true).withRequired(true).build();
        }
        return null;
    }

    public @Nullable ModuleInformation getModuleInformationForIdentifier(Action module,
            Map<String, Set<ModuleInformation>> moduleInformation, boolean isThing) {
        Configuration c = module.getConfiguration();
        String config = (String) c.get(AnnotationActionModuleTypeHelper.CONFIG_PARAM);

        Set<ModuleInformation> mis = moduleInformation.get(module.getTypeUID());

        ModuleInformation finalMI = null;
        if (mis.size() == 1 && config == null) {
            finalMI = (ModuleInformation) mis.toArray()[0];
        } else {
            for (ModuleInformation mi : mis) {
                if (isThing) {
                    if (Objects.equals(mi.getThingUID(), config)) {
                        finalMI = mi;
                        break;
                    }
                } else {
                    if (Objects.equals(mi.getConfigName(), config)) {
                        finalMI = mi;
                        break;
                    }
                }
            }
        }
        return finalMI;
    }
}
