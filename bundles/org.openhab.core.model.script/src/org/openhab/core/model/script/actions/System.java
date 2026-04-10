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
package org.openhab.core.model.script.actions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.RuleManager;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.common.registry.ManagedProvider;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataProvider;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.model.script.engine.action.ActionDoc;
import org.openhab.core.thing.ThingRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * {@link System} provides DSL access to things like OSGi instances, system registries and the ability to run other
 * rules.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class System {

    /**
     * Retrieve an OSGi instance of the specified {@link Class}, if it exists.
     *
     * @param <T> the class type.
     * @param clazz the class of the instance to get.
     * @return The instance or {@code null} if the instance wasn't found.
     */
    @ActionDoc(text = "acquire an OSGi instance")
    public static @Nullable <T> T getInstance(Class<T> clazz) {
        Bundle bundle = FrameworkUtil.getBundle(clazz);
        if (bundle != null) {
            BundleContext bc = bundle.getBundleContext();
            if (bc != null) {
                ServiceReference<T> ref = bc.getServiceReference(clazz);
                if (ref != null) {
                    return bc.getService(ref);
                }
            }
        }
        return null;
    }

    /**
     * Run the rule with the specified UID.
     *
     * @param ruleUID the UID of the rule to run.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    @ActionDoc(text = "run the rule with the specified UID")
    public static Map<String, Object> runRule(String ruleUID) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        if (ruleManager.getStatus(ruleUID) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUID + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUID);
    }

    /**
     * Run the rule with the specified UID with the specified context.
     *
     * @param ruleUID the UID of the rule to run.
     * @param context the {@link Map} of {@link String} and {@link Object} pairs that constitutes the context.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    @ActionDoc(text = "run the rule with the specified UID and context")
    public static Map<String, Object> runRule(String ruleUID, Map<String, Object> context) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        if (ruleManager.getStatus(ruleUID) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUID + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUID, false, context);
    }

    /**
     * Run the rule with the specified UID with the specified context, while optionally taking conditions into
     * account.
     *
     * @param ruleUID the UID of the rule to run.
     * @param considerConditions {@code true} to not run the rule if its conditions don't qualify.
     * @param context the pairs of {@link String}s and {@link Object}s that constitutes the context. Must be in pairs,
     *            the first is the key, the second is the value.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    @ActionDoc(text = "run the rule with the specified UID, condition evaluation setting and context")
    public static Map<String, Object> runRule(String ruleUID, boolean considerConditions, Object... context) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        if (ruleManager.getStatus(ruleUID) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUID + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUID, considerConditions, parseObjectArray(context));
    }

    /**
     * Run the rule with the specified UID with the specified context, while optionally taking conditions into
     * account.
     *
     * @param ruleUID the UID of the rule to run.
     * @param considerConditions {@code true} to not run the rule if its conditions don't qualify.
     * @param context the {@link Map} of {@link String} and {@link Object} pairs that constitutes the context.
     * @return A copy of the rule context, including possible return values.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    @ActionDoc(text = "run the rule with the specified UID, condition evaluation setting and context")
    public static Map<String, Object> runRule(String ruleUID, boolean considerConditions, Map<String, Object> context) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        if (ruleManager.getStatus(ruleUID) == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUID + "' doesn't exist");
        }
        return ruleManager.runNow(ruleUID, considerConditions, context);
    }

    /**
     * Check whether the specified rule is enabled.
     *
     * @param ruleUID the UID of the rule to check.
     * @return {@code true} if the rule is enabled, {@code false} otherwise.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    @ActionDoc(text = "check whether the specified rule rule is enabled")
    public static boolean isRuleEnabled(String ruleUID) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        Boolean result = ruleManager.isEnabled(ruleUID);
        if (result == null) {
            throw new IllegalArgumentException("Rule with UID '" + ruleUID + "' doesn't exist");
        }
        return result.booleanValue();
    }

    /**
     * Set whether the specified rule is enabled.
     *
     * @param ruleUID the UID of the rule to enable or disable.
     * @param enabled {@code true} to enable the rule, {@code false} to disable the rule.
     * @throws IllegalArgumentException If a rule with the specified UID doesn't exist.
     * @throws IllegalStateException If no {@link RuleManager} instance exists.
     */
    @ActionDoc(text = "set whether the specified rule is enabled")
    public static void setRuleEnabled(String ruleUID, boolean enabled) {
        RuleManager ruleManager = ScriptServiceUtil.getRuleManager();
        if (ruleManager == null) {
            throw new IllegalStateException("RuleManager doesn't exist");
        }
        ruleManager.setEnabled(ruleUID, enabled);
    }

    /**
     *
     * @param namespace
     * @param itemName
     * @param value
     * @throws IllegalArgumentException If either value is {@code null} or {@code namespace} or
     *             {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no ManagedProvider is available.
     */
    @NonNullByDefault({})
    public static void addMetadata(String namespace, String itemName, String value) {
        addMetadata(namespace, itemName, value, (String) null);
    }

    @NonNullByDefault({})
    public static void addMetadata(String namespace, String itemName, String value, Object... configuration) {
        addMetadata(namespace, itemName, value, parseObjectArray(configuration));
    }

    /**
     *
     * @param namespace
     * @param itemName
     * @param value
     * @param configuration
     * @throws IllegalArgumentException If {@code namespace}, {@code itemName} or {@code value} is {@code null} or if
     *             {@code namespace} or {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no ManagedProvider is available.
     */
    @NonNullByDefault({})
    public static void addMetadata(String namespace, String itemName, String value,
            @Nullable Map<@NonNull String, @NonNull Object> configuration) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace cannot be null");
        }
        if (itemName == null) {
            throw new IllegalArgumentException("itemName cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        getMetadataRegistry().add(new Metadata(new MetadataKey(namespace, itemName), value, configuration));
    }

    @NonNullByDefault({})
    public static @Nullable Metadata getMetadata(String namespace, String itemName) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace cannot be null");
        }
        if (itemName == null) {
            throw new IllegalArgumentException("itemName cannot be null");
        }
        return getMetadataRegistry().get(new MetadataKey(namespace, itemName));
    }

    @NonNullByDefault({})
    public static @Nullable Metadata removeMetadata(String namespace, String itemName) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace cannot be null");
        }
        if (itemName == null) {
            throw new IllegalArgumentException("itemName cannot be null");
        }
        return getMetadataRegistry().remove(new MetadataKey(namespace, itemName));
    }

    /**
     *
     * @param namespace
     * @param itemName
     * @param value
     * @throws IllegalArgumentException If either value is {@code null} or {@code namespace} or
     *             {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no ManagedProvider is available.
     */
    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(String namespace, String itemName, String value) {
        return updateMetadata(namespace, itemName, value, null);
    }

    /**
     *
     * @param namespace
     * @param itemName
     * @param value
     * @param configuration
     * @throws IllegalArgumentException If {@code namespace}, {@code itemName} or {@code value} is {@code null} or if
     *             {@code namespace} or {@code itemName} is invalid.
     * @throws UnsupportedOperationException If the metadata namespace has a reserved {@link MetadataProvider} that is
     *             not a {@link ManagedProvider}.
     * @throws IllegalStateException If no ManagedProvider is available.
     */
    @NonNullByDefault({})
    public static @Nullable Metadata updateMetadata(String namespace, String itemName, String value,
            @Nullable Map<@NonNull String, @NonNull Object> configuration) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace cannot be null");
        }
        if (itemName == null) {
            throw new IllegalArgumentException("itemName cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        }
        return getMetadataRegistry().update(new Metadata(new MetadataKey(namespace, itemName), value, configuration));
    }

    /**
     * @return The {@link ThingRegistry}.
     */
    @ActionDoc(text = "get the thing registry")
    public static ThingRegistry getThingRegistry() {
        return ScriptServiceUtil.getThingRegistry();
    }

    /**
     * @return The {@link MetadataRegistry}.
     */
    @ActionDoc(text = "get the metadata registry")
    public static MetadataRegistry getMetadataRegistry() {
        return ScriptServiceUtil.getMetadataRegistry();
    }

    /**
     * @return The {@link ItemRegistry}.
     */
    @ActionDoc(text = "get the item registry")
    public static ItemRegistry getItemRegistry() {
        return ScriptServiceUtil.getItemRegistry();
    }

    /**
     * @return The {@link RuleRegistry}.
     */
    @ActionDoc(text = "get the rule registry")
    public static RuleRegistry getRuleRegistry() {
        return ScriptServiceUtil.getRuleRegistry();
    }

    /**
     * @return The {@link RuleManager} or {@code null}.
     */
    @ActionDoc(text = "get the rule manager")
    public static @Nullable RuleManager getRuleManager() {
        return ScriptServiceUtil.getRuleManager();
    }

    private static Map<String, Object> parseObjectArray(Object @Nullable [] objects) throws IllegalArgumentException {
        if (objects == null || objects.length == 0) {
            return Map.of();
        }
        if ((objects.length % 2) != 0) {
            throw new IllegalArgumentException("There must be an even number of objects (" + objects.length + ')');
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < objects.length; i += 2) {
            if (objects[i] instanceof String key) {
                result.put(key, objects[i + 1]);
            } else {
                throw new IllegalArgumentException("Keys must be strings: " + objects[i]);
            }
        }
        return result;
    }
}
