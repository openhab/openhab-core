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
package org.openhab.core.voice.internal.text.interpreter.llm;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;
import org.openhab.core.voice.text.interpreter.llm.LLMToolParam;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link DateTimeLLMTool} is an {@link LLMTool} that returns the current system date and time.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@Component(service = LLMTool.class, immediate = true)
public class DateTimeLLMTool implements LLMTool {
    private static final String ID = "get-date-time";

    private final TimeZoneProvider timeZoneProvider;

    @Activate
    public DateTimeLLMTool(final @Reference TimeZoneProvider timeZoneProvider) {
        this.timeZoneProvider = timeZoneProvider;
    }

    @Override
    public String getUID() {
        return ID;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "Get Date and Time";
    }

    @Override
    public String getShortDescription(@Nullable Locale locale) {
        return "Returns the current date and time.";
    }

    @Override
    public String getDescription(@Nullable Locale locale) {
        return "This tool returns the current date and time in a human-readable format.";
    }

    @Override
    public List<LLMToolParam> getParamDescriptions(@Nullable Locale locale) {
        return List.of();
    }

    @Override
    public String call(Map<String, Object> params, @Nullable Locale locale) throws LLMToolException {
        ZonedDateTime now = ZonedDateTime.now(timeZoneProvider.getTimeZone());
        Locale effectiveLocale = locale != null ? locale : Locale.getDefault();
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                .withLocale(effectiveLocale);
        return now.format(formatter);
    }
}
