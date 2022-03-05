/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
import org.openhab.core.transform.ManagedTransformationConfigurationProvider;
import org.openhab.core.transform.TransformationConfiguration;
import org.openhab.core.transform.TransformationConfigurationProvider;
import org.openhab.core.transform.TransformationConfigurationRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * The {@link TransformationConfigurationRegistryImpl} implements the {@link TransformationConfigurationRegistry}
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true)
public class TransformationConfigurationRegistryImpl
        extends AbstractRegistry<TransformationConfiguration, String, TransformationConfigurationProvider>
        implements TransformationConfigurationRegistry {
    private static final Pattern FILENAME_PATTERN = Pattern
            .compile("(?<filename>.+)(_(?<language>[a-z]{2}))?\\.(?<extension>[^.]*)$");

    private final LocaleProvider localeProvider;

    @Activate
    public TransformationConfigurationRegistryImpl(@Reference LocaleProvider localeProvider) {
        super(TransformationConfigurationProvider.class);

        this.localeProvider = localeProvider;
    }

    @Override
    public @Nullable TransformationConfiguration get(String uid, @Nullable Locale locale) {
        TransformationConfiguration configuration = null;

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
    public Collection<TransformationConfiguration> getConfigurations(Collection<String> types) {
        return getAll().stream().filter(e -> types.contains(e.getType())).collect(Collectors.toList());
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setManagedProvider(ManagedTransformationConfigurationProvider provider) {
        super.setManagedProvider(provider);
    }

    protected void unsetManagedProvider(ManagedTransformationConfigurationProvider provider) {
        super.unsetManagedProvider(provider);
    }

    @Override
    protected void addProvider(Provider<TransformationConfiguration> provider) {
        // overridden to make method available for testing
        super.addProvider(provider);
    }
}
