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
package org.openhab.core.config.core.internal.i18n;

import java.net.URI;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigOptionProvider;
import org.openhab.core.config.core.ParameterOption;
import org.osgi.service.component.annotations.Component;

/**
 * {@link ConfigOptionProvider} that provides a list of
 *
 * @author Simon Kaufmann - Initial contribution
 * @author Erdoan Hadzhiyusein - Added time zone
 */
@NonNullByDefault
@Component(immediate = true)
public class I18nConfigOptionsProvider implements ConfigOptionProvider {

    private static final String NO_OFFSET_FORMAT = "(GMT) %s";
    private static final String NEGATIVE_OFFSET_FORMAT = "(GMT%d:%02d) %s";
    private static final String POSITIVE_OFFSET_FORMAT = "(GMT+%d:%02d) %s";

    @Override
    public @Nullable Collection<ParameterOption> getParameterOptions(URI uri, String param, @Nullable String context,
            @Nullable Locale locale) {
        if ("system:i18n".equals(uri.toString())) {
            Locale translation = locale != null ? locale : Locale.getDefault();
            return processParamType(param, locale, translation);
        }
        return null;
    }

    private @Nullable Collection<ParameterOption> processParamType(String param, @Nullable Locale locale,
            Locale translation) {
        switch (param) {
            case "language":
                return getAvailable(locale,
                        l -> new ParameterOption(l.getLanguage(), l.getDisplayLanguage(translation)));
            case "region":
                return getAvailable(locale, l -> new ParameterOption(l.getCountry(), l.getDisplayCountry(translation)));
            case "variant":
                return getAvailable(locale, l -> new ParameterOption(l.getVariant(), l.getDisplayVariant(translation)));
            case "timezone":
                Comparator<TimeZone> byOffset = (t1, t2) -> {
                    return t1.getRawOffset() - t2.getRawOffset();
                };
                Comparator<TimeZone> byID = (t1, t2) -> {
                    return t1.getID().compareTo(t2.getID());
                };
                return ZoneId.getAvailableZoneIds().stream().map(TimeZone::getTimeZone)
                        .sorted(byOffset.thenComparing(byID)).map(tz -> {
                            return new ParameterOption(tz.getID(), getTimeZoneRepresentation(tz));
                        }).collect(Collectors.toList());
            default:
                return null;
        }
    }

    private static String getTimeZoneRepresentation(TimeZone tz) {
        long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset()) - TimeUnit.HOURS.toMinutes(hours);
        minutes = Math.abs(minutes);
        final String result;
        if (hours > 0) {
            result = String.format(POSITIVE_OFFSET_FORMAT, hours, minutes, tz.getID());
        } else if (hours < 0) {
            result = String.format(NEGATIVE_OFFSET_FORMAT, hours, minutes, tz.getID());
        } else {
            result = String.format(NO_OFFSET_FORMAT, tz.getID());
        }
        return result;
    }

    private Collection<ParameterOption> getAvailable(@Nullable Locale locale,
            Function<Locale, ParameterOption> mapFunction) {
        return Arrays.stream(Locale.getAvailableLocales()) //
                .map(mapFunction) //
                .distinct() //
                .filter(po -> !po.getValue().isEmpty()) //
                .sorted(Comparator.comparing(a -> a.getLabel())) //
                .collect(Collectors.toList());
    }
}
