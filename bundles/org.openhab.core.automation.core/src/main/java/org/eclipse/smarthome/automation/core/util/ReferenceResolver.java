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
package org.eclipse.smarthome.automation.core.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.config.core.Configuration;
import org.slf4j.Logger;

/**
 * Resolves Module references. They can be
 * <ul>
 * <li>Module configuration property to Rule Configuration property</li>
 * <li>Module configuration property to Composite Module configuration property</li>
 * <li>Module inputs to Composite Module inputs</li>
 * <li>Module inputs to Composite Module Configuration</li>
 * </ul>
 *
 * Module 'A' Configuration properties can have references to either CompositeModule Configuration properties or Rule
 * Configuration properties depending where Module 'A' is placed. <br/>
 * Note. If Module 'A' is child of CompositeModule - it cannot have direct configuration references to the Rule that is
 * holding the CompositeModule.
 * <ul>
 * <li>Single reference configuration value where whole configuration property value is replaced(if found) with the
 * referenced value <br/>
 * 'configurationProperty': '${singleReference}'</li>
 * <li>Complex reference configuration value where only reference parts are replaced in the whole configuration property
 * value. <br/>
 * 'configurationProperty': '{key1: ${complexReference1}, key2: ${complexReference2}'</li>
 * </ul>
 *
 * Given Module 'A' is child of CompositeModule then its inputs can have '${singleReferences}' to CompositeModule.
 * <ul>
 * <li>Single reference to CompositeModule inputs where whole input value is replaced with the referenced value <br/>
 * 'childInput' : '${compositeModuleInput}'</li>
 * <li>Single reference to CompositeModule configuration where whole input value is replaced with the referenced value
 * <br/>
 * 'childInput' : '${compositeModuleConfiguration}'</li>
 * </ul>
 *
 * @author Vasil Ilchev - Initial Contribution
 * @author Ana Dimova - new reference syntax: list[index], map["key"], bean.field
 */
public class ReferenceResolver {

    /**
     * Updates (changes) configuration properties of module base on given context (it can be CompositeModule
     * Configuration or Rule Configuration).
     * For example: 1) If a module configuration property has a value '${name}' the method looks for such key in context
     * and if found - replace the module's configuration value as it is.
     *
     * 2) If a module configuration property has complex value 'Hello ${firstName} ${lastName}' the method tries to
     * parse it and replace (if values are found) referenced parts in module's configuration value. Will
     * try to find values for ${firstName} and ${lastName} in the given context and replace them. References that are
     * not found in the context - are not replaced.
     *
     * @param module module that is directly part of Rule or part of CompositeModule
     * @param context containing Rule configuration or Composite configuration values.
     */
    public static void updateConfiguration(Configuration config, Map<String, ?> context, Logger logger) {
        for (String configKey : config.keySet()) {
            Object o = config.get(configKey);
            if (o instanceof String) {
                Object result = resolveProperty(config, context, logger, configKey, (String) o);
                config.put(configKey, result);
            } else if (o instanceof List) {
                ArrayList<Object> resultList = new ArrayList<>();
                List<?> list = (List<?>) o;
                for (Object obj : list) {
                    if (obj instanceof String) {
                        resultList.add(resolveProperty(config, context, logger, configKey, (String) obj));
                    }
                }
                config.put(configKey, resultList);
            }
        }
    }

    private static Object resolveProperty(Configuration config, Map<String, ?> context, Logger logger, String configKey,
            String childConfigPropertyValue) {
        if (isReference(childConfigPropertyValue)) {
            Object result = resolveReference(childConfigPropertyValue, context);
            if (result != null) {
                return result;
            }
        } else if (containsPattern(childConfigPropertyValue)) {
            Object result = resolvePattern(childConfigPropertyValue, context, logger);
            if (result != null) {
                return result;
            }
        }
        return childConfigPropertyValue;
    }

    /**
     * Resolves Composite child module's references to CompositeModule context (inputs and configuration).
     *
     * @param module Composite Module's child module.
     * @param compositeContext Composite Module's context
     * @return context for given module ready for execution.
     */
    public static Map<String, Object> getCompositeChildContext(Module module, Map<String, ?> compositeContext) {
        Map<String, Object> resultContext = new HashMap<>();
        Map<String, String> inputs = null;

        if (module instanceof Condition) {
            inputs = ((Condition) module).getInputs();
        } else if (module instanceof Action) {
            inputs = ((Action) module).getInputs();
        }

        if (inputs != null) {
            for (Entry<String, String> input : inputs.entrySet()) {
                final String inputName = input.getKey();
                final String inputValue = input.getValue();
                if (isReference(inputValue)) {
                    final Object result = resolveReference(inputValue, compositeContext);
                    resultContext.put(inputName, result);
                }
            }
        }
        return resultContext;
    }

    /**
     * Resolves single reference '${singleReference}' from given context.
     *
     * @param reference single reference expression for resolving
     * @param context contains the values that will be used for reference resolving
     * @return resolved value.
     */
    public static Object resolveReference(String reference, Map<String, ?> context) {
        Object result = reference;
        if (isReference(reference)) {
            final String trimmedVal = reference.trim();
            String key = trimmedVal.substring(2, trimmedVal.length() - 1);
            result = context.get(key);// ${substring}
        }
        return result;
    }

    /**
     * Tries to resolve complex references e.g. 'Hello ${firstName} ${lastName}'..'{key1: ${reference1}, key2:
     * ${reference2}}'..etc.
     *
     * References are keys in the context map (without the '${' prefix and '}' suffix).
     *
     * If value is found in the given context it overrides the reference part in the configuration value. For example:
     *
     * <pre>
     * configuration {
     * ..
     *   configProperty: 'Hello ${firstName} ${lastName}'
     * ..
     * }
     * </pre>
     *
     * And context that has value for '${lastName}':
     *
     * <pre>
     * ..
     *   firstName: MyFirstName
     * ..
     *   lastName: MyLastName
     * ..
     * </pre>
     *
     * Result will be:
     *
     * <pre>
     * configuration {
     * ..
     * configProperty: 'Hello MyFirstName MyLastName'
     * ..
     * }
     * </pre>
     *
     * References for which values are not found in the context - remain as they are in the configuration property. (It
     * will not stop resolving the remaining references(if there are) in the configuration property value)
     *
     * @param reference a pattern expression for resolving
     * @param context contains the values that will be used for reference resolving
     * @return the resolved reference.
     */
    private static String resolvePattern(String reference, Map<String, ?> context, Logger logger) {
        final StringBuilder sb = new StringBuilder();
        int previous = 0;
        for (int start, end; (start = reference.indexOf("${", previous)) != -1; previous = end + 1) {
            sb.append(reference.substring(previous, start));
            end = reference.indexOf('}', start + 2);
            if (end == -1) {
                previous = start;
                String msg = "Couldn't parse referenced key: " + reference.substring(start)
                        + ": expected reference syntax-> ${referencedKey}";
                logger.warn(msg);
                throw new IllegalArgumentException(msg);
            }
            final String referencedKey = reference.substring(start + 2, end);
            final Object referencedValue = context.get(referencedKey);

            if (referencedValue != null) {
                sb.append(referencedValue);
            } else {
                String msg = "Cannot find reference for ${ " + referencedKey + " } , it will remain the same.";
                logger.warn(msg);
                sb.append("${" + referencedKey + "}");
            }
        }
        sb.append(reference.substring(previous));
        return sb.toString();
    }

    /**
     * Determines whether given Text is '${reference}'.
     *
     * @param value the value for evaluation
     * @return True if this value is a '${reference}', false otherwise.
     */
    private static boolean isReference(String value) {
        String trimmedVal = value == null ? null : value.trim();
        // starts with '${' and contains it only once contains '}' only once - last char reference is not empty '${}'
        return trimmedVal != null && trimmedVal.lastIndexOf("${") == 0
                && trimmedVal.indexOf('}') == trimmedVal.length() - 1 && trimmedVal.length() > 3;
    }

    /**
     * Determines whether given Text is '...${reference}...'.
     *
     * @param value the value for evaluation
     * @return True if this value is a '...${reference}...', false otherwise.
     */
    private static boolean containsPattern(String value) {
        return value != null && value.trim().contains("${") && value.trim().indexOf("${") < value.trim().indexOf("}");
    }

    /**
     * Gets the end of current token of reference path.
     *
     * @param ref reference path used to access value in bean or map objects
     * @param startIndex starting point to check for next tokens
     * @return end of current token.
     */
    public static int getNextRefToken(String ref, int startIndex) {
        int idx1 = ref.indexOf('[', startIndex);
        int idx2 = ref.indexOf('.', startIndex);
        if ((idx1 != -1) && ((idx2 == -1) || (idx1 < idx2))) {
            return idx1;
        } else if ((idx2 != -1) && ((idx1 == -1) || (idx2 < idx1))) {
            return idx2;
        }
        return -1;
    }

    /**
     * Splits a given reference to tokens.<br>
     * The reference must have the following syntax: list[index], map["key"], bean.field.<br>
     * It is possible to chain references in one bigger expression. For example: {@code list[1].name.values}.
     *
     * @param reference the reference that should be split
     * @return array of the tokens in the reference
     */
    public static String[] splitReferenceToTokens(String reference) throws IllegalArgumentException {
        if (reference == null) {
            return null;
        }
        final String regex = "\\[{1}\\\"{1}.+\\\"{1}\\]{1}|\\[{1}\\d+\\]{1}|[^\\[\\]\\.][A-Za-z0-9_-]+[^\\]\\[\\.]";
        final Pattern pattern = Pattern.compile(regex);
        final List<String> result = new ArrayList<>();
        final Matcher matcher = pattern.matcher(reference);
        while (matcher.find()) {
            String token = matcher.group();
            if (token.startsWith("[\"")) {
                token = token.substring(2, token.length() - 2);
            } else if (token.startsWith("[")) {
                token = token.substring(1, token.length() - 1);
            }
            if (token.isEmpty()) {
                throw new IllegalArgumentException("Invalid reference syntax in reference: " + reference);
            }
            result.add(token);
        }
        return result.stream().toArray(String[]::new);
    }

    /**
     * Gets an object by given hierarchical path of tokens.
     *
     * @param object bean, map or list
     * @param tokens a sequence of field names, indexes, or keys that represent the hierarchical path to the required
     *            object
     * @return the value of the object to witch the hierarchical path is pointing.
     *
     * @throws IllegalArgumentException if one of the tokens point to field that is not existing or the object is null
     * @throws SecurityException If a security manager, <i>s</i>, is present and any of the following
     *             conditions is met:
     *             <ul>
     *             <li>the caller's class loader is not the same as the class loader of this
     *             class and invocation of {@link SecurityManager#checkPermission
     *             s.checkPermission} method with
     *             {@code RuntimePermission("accessDeclaredMembers")} denies access to the
     *             declared field</li>
     *             <li>the caller's class loader is not the same as or an ancestor of the
     *             class loader for the current class and invocation of
     *             {@link SecurityManager#checkPackageAccess s.checkPackageAccess()} denies
     *             access to the package of this class</li>
     *             </ul>
     * @throws ArrayIndexOutOfBoundsException if one of the tokens represent a invalid index in the list.
     * @throws NullPointerException if the path references something in a non existing map entry.
     * @throws NumberFormatException if one of the tokens is accessing a list and the token that represent the
     *             index can't be converted to integer.
     */
    public static Object resolveComplexDataReference(Object object, String... tokens)
            throws IllegalArgumentException, SecurityException {
        if (object == null) {
            throw new IllegalArgumentException("Object is null.");
        }
        if (tokens == null) {
            return object;
        }
        try {
            Object obj = object;
            for (String token : tokens) {
                if (obj instanceof Map) {
                    obj = getValueFromMap(((Map<?, ?>) obj), token);
                } else if (obj instanceof List) {
                    obj = getValueFromList(((List<?>) obj), Integer.parseInt(token));
                } else {
                    final Class<?> objClass = obj.getClass();
                    obj = getValueFromBean(objClass, obj, token);
                }
            }
            return obj;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Invalid reference path. A field from the reference path doesn't exist",
                    e);
        }
    }

    private static Object getValueFromMap(Map<?, ?> map, String key) {
        return map.get(key);
    }

    private static Object getValueFromList(List<?> list, int index) {
        return list.get(index);
    }

    private static Object getValueFromBean(Class<?> objClass, Object bean, String fieldName)
            throws NoSuchFieldException, SecurityException {
        try {
            Field f = objClass.getDeclaredField(fieldName);
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            return f.get(bean);
        } catch (IllegalAccessException e) {
            // Should never happen because we set the field to accessible.
            return null;
        }
    }
}
