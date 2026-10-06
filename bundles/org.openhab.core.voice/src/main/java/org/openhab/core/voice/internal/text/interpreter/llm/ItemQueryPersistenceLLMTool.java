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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;
import org.openhab.core.voice.text.interpreter.llm.LLMToolParam;
import org.openhab.core.voice.text.interpreter.llm.LLMToolParamType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ItemQueryPersistenceLLMTool} is an {@link LLMTool} that allows LLMs to query
 * historical and future/forecasted item state data and statistics using the standard persistence service.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@Component(service = LLMTool.class, immediate = true)
public class ItemQueryPersistenceLLMTool implements LLMTool {
    public static final String ID = "item-query-persistence";

    public enum QueryType {
        STATE_AT,
        LAST_CHANGE,
        NEXT_CHANGE,
        LAST_UPDATE,
        NEXT_UPDATE,
        MINIMUM,
        MAXIMUM,
        AVERAGE,
        DELTA,
        SUM
    }

    private record TimeRange(@Nullable ZonedDateTime start, @Nullable ZonedDateTime end) {
    }

    private final ItemRegistry itemRegistry;
    private final ItemPermissionResolver itemPermissionResolver;
    private final TimeZoneProvider timeZoneProvider;

    @Activate
    public ItemQueryPersistenceLLMTool(final @Reference ItemRegistry itemRegistry,
            final @Reference ItemPermissionResolver itemPermissionResolver,
            final @Reference TimeZoneProvider timeZoneProvider) {
        this.itemRegistry = itemRegistry;
        this.itemPermissionResolver = itemPermissionResolver;
        this.timeZoneProvider = timeZoneProvider;
    }

    @Override
    public String getUID() {
        return ID;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "Query Persistence";
    }

    @Override
    public String getShortDescription(@Nullable Locale locale) {
        return "Queries historical or forecasted item states and aggregations.";
    }

    @Override
    public String getDescription(@Nullable Locale locale) {
        return """
                Query historical or forecasted item state and aggregations. \
                Requires 'state' for STATE_AT. Aggregations (MINIMUM, MAXIMUM, AVERAGE, DELTA, SUM) \
                query between 'start' and 'end' (defaulting to 'now' if only one past/future bound is given).""";
    }

    @Override
    public List<LLMToolParam> getParamDescriptions(@Nullable Locale locale) {
        List<String> queryTypeOptions = Arrays.stream(QueryType.values()).map(Enum::name).toList();
        return List.of(new LLMToolParam("itemName", LLMToolParamType.STRING, "Item name", List.of(), true),
                new LLMToolParam("queryType", LLMToolParamType.STRING, "Query type", queryTypeOptions, true),
                new LLMToolParam("start", LLMToolParamType.STRING,
                        "ISO-8601, 'now', or relative ISO-8601 offset ('-PT4H', '-P1D'). Required for STATE_AT.",
                        List.of(), false),
                new LLMToolParam("end", LLMToolParamType.STRING,
                        "ISO-8601, 'now', or relative ISO-8601 offset ('+PT12H').", List.of(), false));
    }

    @Override
    public String call(Map<String, Object> params, @Nullable Locale locale) throws LLMToolException {
        Item item = LLMToolUtil.resolveAndValidateItem(itemRegistry, itemPermissionResolver, params).item();
        QueryType queryType = parseQueryType(params);

        ZoneId zoneId = timeZoneProvider.getTimeZone();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        Locale effectiveLocale = locale != null ? locale : Locale.getDefault();

        TimeRange range = parseStartAndEnd(params, queryType, now, zoneId);
        ZonedDateTime start = range.start();
        ZonedDateTime end = range.end();

        return switch (queryType) {
            case STATE_AT -> {
                if (start == null) {
                    throw new LLMToolException("Parameter 'start' is required for query type 'STATE_AT'");
                }
                HistoricItem historicItem = PersistenceExtensions.persistedState(item, start);
                yield historicItem != null && !(historicItem.getState() instanceof UnDefType)
                        ? LLMToolUtil.formatItemState(item, historicItem.getState(), effectiveLocale, zoneId)
                                + " (recorded at "
                                + LLMToolUtil.formatTimestamp(historicItem.getTimestamp(), effectiveLocale) + ")"
                        : "No persistence data found for item '" + item.getName() + "' at the specified time.";
            }
            case LAST_CHANGE -> {
                ZonedDateTime lastChange = PersistenceExtensions.lastChange(item);
                yield lastChange != null ? LLMToolUtil.formatTimestamp(lastChange, effectiveLocale)
                        : "No state change recorded for item '" + item.getName() + "'.";
            }
            case NEXT_CHANGE -> {
                ZonedDateTime nextChange = PersistenceExtensions.nextChange(item);
                yield nextChange != null ? LLMToolUtil.formatTimestamp(nextChange, effectiveLocale)
                        : "No future state change scheduled for item '" + item.getName() + "'.";
            }
            case LAST_UPDATE -> {
                ZonedDateTime lastUpdate = PersistenceExtensions.lastUpdate(item);
                yield lastUpdate != null ? LLMToolUtil.formatTimestamp(lastUpdate, effectiveLocale)
                        : "No update recorded for item '" + item.getName() + "'.";
            }
            case NEXT_UPDATE -> {
                ZonedDateTime nextUpdate = PersistenceExtensions.nextUpdate(item);
                yield nextUpdate != null ? LLMToolUtil.formatTimestamp(nextUpdate, effectiveLocale)
                        : "No future update scheduled for item '" + item.getName() + "'.";
            }
            case MINIMUM -> {
                HistoricItem minItem;
                if (start != null && end != null) {
                    minItem = PersistenceExtensions.minimumBetween(item, start, end);
                } else if (start != null) {
                    minItem = PersistenceExtensions.minimumSince(item, start);
                } else if (end != null) {
                    minItem = PersistenceExtensions.minimumUntil(item, end);
                } else {
                    throw new LLMToolException("At least 'start' or 'end' must be specified for MINIMUM");
                }
                yield minItem != null && !(minItem.getState() instanceof UnDefType)
                        ? LLMToolUtil.formatItemState(item, minItem.getState(), effectiveLocale, zoneId) + " (at "
                                + LLMToolUtil.formatTimestamp(minItem.getTimestamp(), effectiveLocale) + ")"
                        : "No minimum data found for item '" + item.getName() + "' in the specified timeframe.";
            }
            case MAXIMUM -> {
                HistoricItem maxItem;
                if (start != null && end != null) {
                    maxItem = PersistenceExtensions.maximumBetween(item, start, end);
                } else if (start != null) {
                    maxItem = PersistenceExtensions.maximumSince(item, start);
                } else if (end != null) {
                    maxItem = PersistenceExtensions.maximumUntil(item, end);
                } else {
                    throw new LLMToolException("At least 'start' or 'end' must be specified for MAXIMUM");
                }
                yield maxItem != null && !(maxItem.getState() instanceof UnDefType)
                        ? LLMToolUtil.formatItemState(item, maxItem.getState(), effectiveLocale, zoneId) + " (at "
                                + LLMToolUtil.formatTimestamp(maxItem.getTimestamp(), effectiveLocale) + ")"
                        : "No maximum data found for item '" + item.getName() + "' in the specified timeframe.";
            }
            case AVERAGE -> {
                State avgState;
                if (start != null && end != null) {
                    avgState = PersistenceExtensions.averageBetween(item, start, end);
                } else if (start != null) {
                    avgState = PersistenceExtensions.averageSince(item, start);
                } else if (end != null) {
                    avgState = PersistenceExtensions.averageUntil(item, end);
                } else {
                    throw new LLMToolException("At least 'start' or 'end' must be specified for AVERAGE");
                }
                yield avgState != null && !(avgState instanceof UnDefType)
                        ? LLMToolUtil.formatItemState(item, avgState, effectiveLocale, zoneId)
                        : "No average data found for item '" + item.getName() + "' in the specified timeframe.";
            }
            case DELTA -> {
                State deltaState;
                if (start != null && end != null) {
                    deltaState = PersistenceExtensions.deltaBetween(item, start, end);
                } else if (start != null) {
                    deltaState = PersistenceExtensions.deltaSince(item, start);
                } else if (end != null) {
                    deltaState = PersistenceExtensions.deltaUntil(item, end);
                } else {
                    throw new LLMToolException("At least 'start' or 'end' must be specified for DELTA");
                }
                yield deltaState != null && !(deltaState instanceof UnDefType)
                        ? LLMToolUtil.formatItemState(item, deltaState, effectiveLocale, zoneId)
                        : "No delta data found for item '" + item.getName() + "' in the specified timeframe.";
            }
            case SUM -> {
                State sumState;
                if (start != null && end != null) {
                    sumState = PersistenceExtensions.sumBetween(item, start, end);
                } else if (start != null) {
                    sumState = PersistenceExtensions.sumSince(item, start);
                } else if (end != null) {
                    sumState = PersistenceExtensions.sumUntil(item, end);
                } else {
                    throw new LLMToolException("At least 'start' or 'end' must be specified for SUM");
                }
                yield sumState != null && !(sumState instanceof UnDefType)
                        ? LLMToolUtil.formatItemState(item, sumState, effectiveLocale, zoneId)
                        : "No sum data found for item '" + item.getName() + "' in the specified timeframe.";
            }
        };
    }

    private QueryType parseQueryType(Map<String, Object> params) throws LLMToolException {
        Object queryTypeObj = params.get("queryType");
        if (!(queryTypeObj instanceof String queryTypeStr)) {
            throw new LLMToolException("Missing or invalid required parameter 'queryType'");
        }

        try {
            return QueryType.valueOf(queryTypeStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new LLMToolException("Unknown queryType: " + queryTypeStr, e);
        }
    }

    private TimeRange parseStartAndEnd(Map<String, Object> params, QueryType queryType, ZonedDateTime now,
            ZoneId zoneId) throws LLMToolException {
        if (queryType == QueryType.LAST_CHANGE || queryType == QueryType.NEXT_CHANGE
                || queryType == QueryType.LAST_UPDATE || queryType == QueryType.NEXT_UPDATE) {
            return new TimeRange(null, null);
        }

        Object startObj = params.get("start");
        Object endObj = params.get("end");

        if (queryType == QueryType.STATE_AT) {
            if (!(startObj instanceof String startStr) || startStr.isBlank()) {
                throw new LLMToolException("Parameter 'start' is required for query type 'STATE_AT'");
            }
            return new TimeRange(LLMToolUtil.parseTime(startStr, now, zoneId), null);
        }

        ZonedDateTime startZdt = null;
        if (startObj instanceof String startStr && !startStr.isBlank()) {
            startZdt = LLMToolUtil.parseTime(startStr, now, zoneId);
        }

        ZonedDateTime endZdt = null;
        if (endObj instanceof String endStr && !endStr.isBlank()) {
            endZdt = LLMToolUtil.parseTime(endStr, now, zoneId);
        }

        if (startZdt == null && endZdt == null) {
            throw new LLMToolException(
                    "At least 'start' or 'end' must be specified for query type '" + queryType + "'");
        }

        if (startZdt != null && endZdt != null) {
            if (!startZdt.isBefore(endZdt)) {
                throw new LLMToolException("Parameter 'start' must be before 'end'");
            }
            return new TimeRange(startZdt, endZdt);
        }

        if (startZdt != null) {
            if (startZdt.isAfter(now)) {
                throw new LLMToolException(
                        "Parameter 'end' must be specified when 'start' is in the future for query type '" + queryType
                                + "'");
            }
            return new TimeRange(startZdt, null);
        }

        // Only endZdt is provided
        if (endZdt.isBefore(now)) {
            throw new LLMToolException(
                    "Parameter 'start' must be specified when 'end' is in the past for query type '" + queryType + "'");
        }
        return new TimeRange(null, endZdt);
    }
}
