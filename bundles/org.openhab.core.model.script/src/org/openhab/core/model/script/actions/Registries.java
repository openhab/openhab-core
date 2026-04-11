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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.model.script.ScriptServiceUtil;
import org.openhab.core.model.script.engine.action.ActionDoc;
import org.openhab.core.thing.ThingRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * {@link Registries} provides DSL access to things like OSGi instances, system registries and the ability to run other
 * rules.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class Registries {

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
}
