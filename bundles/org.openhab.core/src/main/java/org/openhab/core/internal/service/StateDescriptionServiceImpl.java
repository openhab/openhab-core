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
package org.openhab.core.internal.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.internal.types.StateDescriptionFragmentImpl;
import org.openhab.core.service.StateDescriptionService;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * This service contains different {@link StateDescriptionFragmentProvider}s and provides a getStateDescription method
 * that returns a single {@link StateDescription} using all of the providers.
 *
 * @author Lyubomir Papazov - Initial contribution
 */
@NonNullByDefault
@Component
public class StateDescriptionServiceImpl implements StateDescriptionService {

    private final Set<StateDescriptionFragmentProvider> stateDescriptionFragmentProviders = Collections.synchronizedSet(
            new TreeSet<StateDescriptionFragmentProvider>(new Comparator<StateDescriptionFragmentProvider>() {
                @Override
                public int compare(StateDescriptionFragmentProvider provider1,
                        StateDescriptionFragmentProvider provider2) {
                    return provider2.getRank().compareTo(provider1.getRank());
                }
            }));

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void addStateDescriptionFragmentProvider(StateDescriptionFragmentProvider provider) {
        stateDescriptionFragmentProviders.add(provider);
    }

    public void removeStateDescriptionFragmentProvider(StateDescriptionFragmentProvider provider) {
        stateDescriptionFragmentProviders.remove(provider);
    }

    @Override
    public @Nullable StateDescription getStateDescription(String itemName, @Nullable Locale locale) {
        StateDescriptionFragment stateDescriptionFragment = getMergedStateDescriptionFragments(itemName, locale);
        return stateDescriptionFragment != null ? stateDescriptionFragment.toStateDescription() : null;
    }

    private @Nullable StateDescriptionFragment getMergedStateDescriptionFragments(String itemName,
            @Nullable Locale locale) {
        StateDescriptionFragmentImpl result = null;
        for (StateDescriptionFragmentProvider provider : stateDescriptionFragmentProviders) {
            StateDescriptionFragment fragment = provider.getStateDescriptionFragment(itemName, locale);
            if (fragment == null) {
                continue;
            }

            // we pick up the first valid StateDescriptionFragment here:
            if (result == null) {
                // create a deep copy of the first found fragment before merging other fragments into it
                result = new StateDescriptionFragmentImpl((StateDescriptionFragmentImpl) fragment);
            } else {
                result.merge(fragment);
            }
        }
        return result;
    }
}
