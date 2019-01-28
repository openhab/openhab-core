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
package org.eclipse.smarthome.core.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

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
public class BindingInfoRegistry {

    private final Collection<BindingInfoProvider> bindingInfoProviders = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addBindingInfoProvider(BindingInfoProvider bindingInfoProvider) {
        if (bindingInfoProvider != null) {
            this.bindingInfoProviders.add(bindingInfoProvider);
        }
    }

    protected void removeBindingInfoProvider(BindingInfoProvider bindingInfoProvider) {
        if (bindingInfoProvider != null) {
            this.bindingInfoProviders.remove(bindingInfoProvider);
        }
    }

    /**
     * Returns the binding information for the specified binding ID, or {@code null} if no binding information could be
     * found.
     *
     * @param id the ID to be looked for (could be null or empty)
     * @return a binding information object (could be null)
     */
    public BindingInfo getBindingInfo(String id) {
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
    public BindingInfo getBindingInfo(String id, Locale locale) {
        for (BindingInfoProvider bindingInfoProvider : this.bindingInfoProviders) {
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
    public Set<BindingInfo> getBindingInfos(Locale locale) {
        Set<BindingInfo> allBindingInfos = new LinkedHashSet<>(10);

        for (BindingInfoProvider bindingInfoProvider : this.bindingInfoProviders) {
            Set<BindingInfo> bindingInfos = bindingInfoProvider.getBindingInfos(locale);
            allBindingInfos.addAll(bindingInfos);
        }

        return Collections.unmodifiableSet(allBindingInfos);
    }

}
