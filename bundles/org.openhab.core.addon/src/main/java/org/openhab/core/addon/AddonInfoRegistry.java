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
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BinaryOperator;
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
    public void addAddonInfoProvider(AddonInfoProvider addonInfoProvider) {
        addonInfoProviders.add(addonInfoProvider);
    }

    public void removeAddonInfoProvider(AddonInfoProvider addonInfoProvider) {
        addonInfoProviders.remove(addonInfoProvider);
    }

    /**
     * Returns the add-on information for the specified add-on UID, or {@code null} if no add-on information could be
     * found.
     *
     * @param uid the UID to be looked
     * @return a add-on information object (could be null)
     */
    public @Nullable AddonInfo getAddonInfo(String uid) {
        return getAddonInfo(uid, null);
    }

    /**
     * Returns the add-on information for the specified add-on UID and locale (language),
     * or {@code null} if no add-on information could be found.
     * <p>
     * If more than one provider provides information for the specified add-on UID and locale,
     * it returns a new {@link AddonInfo} containing merged information from all such providers.
     *
     * @param uid the UID to be looked for
     * @param locale the locale to be used for the add-on information (could be null)
     * @return a localized add-on information object (could be null)
     */
    public @Nullable AddonInfo getAddonInfo(String uid, @Nullable Locale locale) {
        return addonInfoProviders.stream().map(p -> p.getAddonInfo(uid, locale)).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(a -> a == null ? "" : a.getUID(),
                        Collectors.collectingAndThen(Collectors.reducing(mergeAddonInfos), Optional::get)))
                .get(uid);
    }

    /**
     * A {@link BinaryOperator} to merge the field values from two {@link AddonInfo} objects into a third such object.
     * <p>
     * If the first object has a non-null field value the result object takes the first value, or if the second object
     * has a non-null field value the result object takes the second value. Otherwise the field remains null.
     * 
     * @param a the first {@link AddonInfo} (could be null)
     * @param b the second {@link AddonInfo} (could be null)
     * @return a new {@link AddonInfo} containing the combined field values (could be null)
     */
    private static BinaryOperator<@Nullable AddonInfo> mergeAddonInfos = (a, b) -> {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        }
        AddonInfo.Builder builder = AddonInfo.builder(a);
        if (a.getDescription().isEmpty()) {
            builder.withDescription(b.getDescription());
        }
        if (a.getConnection() == null && b.getConnection() != null) {
            builder.withConnection(b.getConnection());
        }
        Set<String> countries = new HashSet<>(a.getCountries());
        countries.addAll(b.getCountries());
        if (!countries.isEmpty()) {
            builder.withCountries(countries.stream().toList());
        }
        String aConfigDescriptionURI = a.getConfigDescriptionURI();
        if (aConfigDescriptionURI == null || aConfigDescriptionURI.isEmpty() && b.getConfigDescriptionURI() != null) {
            builder.withConfigDescriptionURI(b.getConfigDescriptionURI());
        }
        if (a.getSourceBundle() == null && b.getSourceBundle() != null) {
            builder.withSourceBundle(b.getSourceBundle());
        }
        String defaultServiceId = a.getType() + "." + a.getId();
        if (defaultServiceId.equals(a.getServiceId()) && !defaultServiceId.equals(b.getServiceId())) {
            builder.withServiceId(b.getServiceId());
        }
        String defaultUID = a.getType() + Addon.ADDON_SEPARATOR + a.getId();
        if (defaultUID.equals(a.getUID()) && !defaultUID.equals(b.getUID())) {
            builder.withUID(b.getUID());
        }
        Set<AddonDiscoveryMethod> discoveryMethods = new HashSet<>(a.getDiscoveryMethods());
        discoveryMethods.addAll(b.getDiscoveryMethods());
        if (!discoveryMethods.isEmpty()) {
            builder.withDiscoveryMethods(discoveryMethods.stream().toList());
        }
        return builder.build();
    };

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
