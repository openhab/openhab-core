/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.core.addon;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link AddonInfoRegistry} provides access to {@link AddonInfo} objects.
 * It tracks {@link AddonInfoProvider} <i>OSGi</i> services to collect all {@link AddonInfo} objects.
 *
 * @author Dennis Nobel - Initial contribution
 * @author Michael Grammling - Initial contribution, added locale support
 */
@Component(immediate = true, service = AddonInfoRegistry.class)
@NonNullByDefault
public class AddonInfoRegistry {

    private final Collection<AddonInfoProvider> addonInfoProviders = new CopyOnWriteArrayList<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addAddonInfoProvider(AddonInfoProvider addonInfoProvider) {
        addonInfoProviders.add(addonInfoProvider);
    }

    protected void removeAddonInfoProvider(AddonInfoProvider addonInfoProvider) {
        addonInfoProviders.remove(addonInfoProvider);
    }

    /**
     * Returns the add-on information for the specified add-on ID, or {@code null} if no add-on information could be
     * found.
     *
     * @param id the ID to be looked
     * @return a add-on information object (could be null)
     */
    public @Nullable AddonInfo getAddonInfo(String id) {
        return getAddonInfo(id, null);
    }

    /**
     * Returns the add-on information for the specified add-on ID and locale (language),
     * or {@code null} if no add-on information could be found.
     *
     * @param id the ID to be looked for
     * @param locale the locale to be used for the add-on information (could be null)
     * @return a localized add-on information object (could be null)
     */
    public @Nullable AddonInfo getAddonInfo(String id, @Nullable Locale locale) {
        return addonInfoProviders.stream().map(p -> p.getAddonInfo(id, locale)).filter(Objects::nonNull).findAny()
                .orElse(null);
    }

    /**
     * Returns all add-on information this registry contains.
     *
     * @return a set of all add-on information this registry contains (not null, could be empty)
     */
    public Set<AddonInfo> getAddonInfos() {
        return getAddonInfos(null);
    }

    /**
     * Returns all add-on information in the specified locale (language) this registry contains.
     *
     * @param locale the locale to be used for the add-on information (could be null)
     * @return a localized set of all add-on information this registry contains
     *         (not null, could be empty)
     */
    public Set<AddonInfo> getAddonInfos(@Nullable Locale locale) {
        return addonInfoProviders.stream().map(provider -> provider.getAddonInfos(locale)).flatMap(Set::stream)
                .collect(Collectors.toUnmodifiableSet());
    }
}
