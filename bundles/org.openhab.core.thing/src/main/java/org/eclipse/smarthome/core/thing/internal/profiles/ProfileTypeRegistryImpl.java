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
package org.eclipse.smarthome.core.thing.internal.profiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.smarthome.core.thing.profiles.ProfileType;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeProvider;
import org.eclipse.smarthome.core.thing.profiles.ProfileTypeRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * {@link ProfileTypeRegistry} implementation.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
@Component(service = ProfileTypeRegistry.class)
public class ProfileTypeRegistryImpl implements ProfileTypeRegistry {

    private final List<ProfileTypeProvider> profileTypeProviders = new CopyOnWriteArrayList<>();

    @Override
    public List<ProfileType> getProfileTypes() {
        return getProfileTypes(null);
    }

    @Override
    public List<ProfileType> getProfileTypes(Locale locale) {
        List<ProfileType> profileTypes = new ArrayList<>();
        for (ProfileTypeProvider profileTypeProvider : profileTypeProviders) {
            profileTypes.addAll(profileTypeProvider.getProfileTypes(locale));
        }
        return Collections.unmodifiableList(profileTypes);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addProfileTypeProvider(ProfileTypeProvider profileTypeProvider) {
        profileTypeProviders.add(profileTypeProvider);
    }

    protected void removeProfileTypeProvider(ProfileTypeProvider profileTypeProvider) {
        profileTypeProviders.remove(profileTypeProvider);
    }

}
