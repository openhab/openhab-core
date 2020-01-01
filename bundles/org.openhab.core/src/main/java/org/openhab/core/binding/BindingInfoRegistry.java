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
package org.openhab.core.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link BindingInfoRegistry} provides access to {@link BindingInfo} objects.
 * It tracks {@link BindingInfoProvider} <i>OSGi</i> services to collect all {@link BindingInfo} objects.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Michael Grammling - Initial contribution, added locale support
 */
@Component(immediate = true, service = BindingInfoRegistry.class)
@NonNullByDefault
public class BindingInfoRegistry {

    private final Collection<BindingInfoProvider> bindingInfoProviders = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addBindingInfoProvider(BindingInfoProvider bindingInfoProvider) {
        if (bindingInfoProvider != null) {
            bindingInfoProviders.add(bindingInfoProvider);
        }
    }

    protected void removeBindingInfoProvider(BindingInfoProvider bindingInfoProvider) {
        if (bindingInfoProvider != null) {
            bindingInfoProviders.remove(bindingInfoProvider);
        }
    }

    /**
     * Returns the binding information for the specified binding ID, or {@code null} if no binding information could be
     * found.
     *
     * @param id the ID to be looked for (could be null or empty)
     * @return a binding information object (could be null)
     */
    public @Nullable BindingInfo getBindingInfo(@Nullable String id) {
        return getBindingInfo(id, null);
    }

    /**
     * Returns the binding information for the specified binding ID and locale (language),
     * or {@code null} if no binding information could be found.
     *
     * @param id the ID to be looked for (could be null or empty)
     * @param locale the locale to be used for the binding information (could be null)
     * @return a localized binding information object (could be null)
     */
    public @Nullable BindingInfo getBindingInfo(@Nullable String id, @Nullable Locale locale) {
        for (BindingInfoProvider bindingInfoProvider : bindingInfoProviders) {
            BindingInfo bindingInfo = bindingInfoProvider.getBindingInfo(id, locale);
            if (bindingInfo != null) {
                return bindingInfo;
            }
        }
        return null;
    }

    /**
     * Returns all binding information this registry contains.
     *
     * @return a set of all binding information this registry contains (not null, could be empty)
     */
    public Set<BindingInfo> getBindingInfos() {
        return getBindingInfos(null);
    }

    /**
     * Returns all binding information in the specified locale (language) this registry contains.
     *
     * @param locale the locale to be used for the binding information (could be null)
     * @return a localized set of all binding information this registry contains
     *         (not null, could be empty)
     */
    public Set<BindingInfo> getBindingInfos(@Nullable Locale locale) {
        Set<BindingInfo> allBindingInfos = new LinkedHashSet<>(bindingInfoProviders.size());
        for (BindingInfoProvider bindingInfoProvider : bindingInfoProviders) {
            Set<BindingInfo> bindingInfos = bindingInfoProvider.getBindingInfos(locale);
            allBindingInfos.addAll(bindingInfos);
        }
        return Collections.unmodifiableSet(allBindingInfos);
    }
}
