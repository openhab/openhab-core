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
package org.openhab.core.transform.internal;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.AbstractRegistry;
import org.openhab.core.common.registry.Provider;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.transform.ManagedTransformationProvider;
import org.openhab.core.transform.Transformation;
import org.openhab.core.transform.TransformationProvider;
import org.openhab.core.transform.TransformationRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link TransformationRegistryImpl} implements the {@link TransformationRegistry}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class TransformationRegistryImpl extends AbstractRegistry<Transformation, String, TransformationProvider>
        implements TransformationRegistry {
    private static final Pattern FILENAME_PATTERN = Pattern
            .compile("(?<filename>.+)(_(?<language>[a-z]{2}))?\\.(?<extension>[^.]*)$");

    private final LocaleProvider localeProvider;

    @Activate
    public TransformationRegistryImpl(@Reference LocaleProvider localeProvider) {
        super(TransformationProvider.class);

        this.localeProvider = localeProvider;
    }

    @Override
    public @Nullable Transformation get(String uid, @Nullable Locale locale) {
        Transformation configuration = null;

        String language = Objects.requireNonNullElse(locale, localeProvider.getLocale()).getLanguage();
        Matcher uidMatcher = CONFIG_UID_PATTERN.matcher(uid);
        if (uidMatcher.matches()) {
            // try to get localized version of the uid if no locale information is present
            if (uidMatcher.group("language") == null) {
                configuration = get(uid + ":" + language);
            }
        } else {
            // check if legacy configuration and try to get localized version
            uidMatcher = FILENAME_PATTERN.matcher(uid);
            if (uidMatcher.matches() && uidMatcher.group("language") == null) {
                // try to get a localized version
                String localizedUid = uidMatcher.group("filename") + "_" + language + "."
                        + uidMatcher.group("extension");
                configuration = get(localizedUid);
            }
        }

        return (configuration != null) ? configuration : get(uid);
    }

    @Override
    public Collection<Transformation> getTransformations(Collection<String> types) {
        return getAll().stream().filter(e -> types.contains(e.getType())).collect(Collectors.toList());
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedTransformationProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedTransformationProvider provider) {
        super.unsetManagedProvider(provider);
    }

    @Override
    protected void addProvider(Provider<Transformation> provider) {
        // overridden to make method available for testing
        super.addProvider(provider);
    }
}
