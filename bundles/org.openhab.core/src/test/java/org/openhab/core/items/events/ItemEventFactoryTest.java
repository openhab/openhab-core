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
package org.openhab.core.items.events;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import javax.measure.MetricPrefix;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.events.Event;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.items.Item;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.NextPreviousType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.RewindFastforwardType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.StringListType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.TimeSeries.Entry;
import org.openhab.core.types.TimeSeries.Policy;
import org.openhab.core.types.UnDefType;

import com.google.gson.Gson;

import tech.units.indriya.unit.Units;

/**
 * {@link ItemEventFactoryTest} tests the {@link ItemEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ItemEventFactoryTest {
    @Mock
    @NonNullByDefault({})
    private TimeZoneProvider timeZoneProvider;
    private @NonNullByDefault({}) ItemEventFactory factory;

    private static final String ITEM_NAME = "ItemA";
    private static final Item ITEM = new SwitchItem(ITEM_NAME);
    private static final String GROUP_NAME = "GroupA";
    private static final String SOURCE = "binding:type:id:channel";

    private static final String ITEM_ADDED_EVENT_TYPE = ItemAddedEvent.TYPE;
    private static final String GROUPITEM_CHANGED_EVENT_TYPE = GroupItemStateChangedEvent.TYPE;

    private static final String ITEM_ADDED_EVENT_TOPIC = "openhab/items/" + ITEM_NAME + "/added";
    private static final String GROUPITEM_STATE_CHANGED_EVENT_TOPIC = "openhab/items/" + GROUP_NAME + "/" + ITEM_NAME
            + "/statechanged";

    private static final String ITEM_ADDED_EVENT_PAYLOAD = new Gson().toJson(ItemDTOMapper.map(ITEM));

    private static final State RAW_ITEM_STATE = new RawType(new byte[] { 1, 2, 3, 4, 5 }, RawType.DEFAULT_MIME_TYPE);
    private static final State NEW_RAW_ITEM_STATE = new RawType(new byte[] { 5, 4, 3, 2, 1 },
            RawType.DEFAULT_MIME_TYPE);

    private record CommandTestCase(String itemName, Command command, @Nullable String source, String expectedPayload,
            Command expectedCommand, @Nullable ZoneId zone) {
    }

    private record StateTestCase(String itemName, State state, @Nullable String source, String expectedPayload,
            State expectedState, @Nullable ZoneId zone) {
    }

    private record StateChangedTestCase(String itemName, State oldState, State newState, ZonedDateTime lastStateUpdate,
            ZonedDateTime lastStateChange, @Nullable String source, String expectedPayload, State expectedOldState,
            State expectedNewState, @Nullable ZoneId zone) {
    }

    private record TimeSeriesTestEntry(Instant timestamp, State state, State expectedState) {
    }

    private record TimeSeriesTestCase(String itemName, Policy policy, List<TimeSeriesTestEntry> entries,
            String expectedPayload, @Nullable String source, @Nullable ZoneId zone) {
    }

    @BeforeEach
    public void init() {
        when(timeZoneProvider.getTimeZone()).thenReturn(ZoneId.of("MIT", ZoneId.SHORT_IDS));
        factory = new ItemEventFactory(timeZoneProvider);
    }

    public static final List<CommandTestCase> COMMAND_TEST_SOURCE = List.of(
            new CommandTestCase("ItemA", OnOffType.ON, SOURCE, "{\"type\":\"OnOff\",\"value\":\"ON\"}", OnOffType.ON,
                    null),
            new CommandTestCase("ItemB", OnOffType.OFF, SOURCE, "{\"type\":\"OnOff\",\"value\":\"OFF\"}", OnOffType.OFF,
                    null),
            new CommandTestCase("ItemC", DateTimeType.valueOf("2012-04-08T18:23:12FJT"), SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"2012-04-08T18:23:12+12:00[Pacific/Fiji]\"}",
                    DateTimeType.valueOf("2012-04-08T18:23:12FJT"), null),
            new CommandTestCase("ItemD", DateTimeType.valueOf("?2012-04-08T18:23:12FJT"), SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"?2012-04-08T18:23:12+12:00[Pacific/Fiji]\"}",
                    DateTimeType.valueOf("2012-04-08T08:23:12+02:00[CET]"), ZoneId.of("CET")),
            new CommandTestCase("ItemE", DateTimeType.valueOf("?2012-04-08T18:23:12FJT"), SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"?2012-04-08T18:23:12+12:00[Pacific/Fiji]\"}",
                    DateTimeType.valueOf("2012-04-08T08:23:12+02:00[Asia/Kathmandu]"), ZoneId.of("Asia/Kathmandu")),
            new CommandTestCase("ItemF", DateTimeType.valueOf("2012-04-08T18:23:12+00:00[Australia/Eucla]"), SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"2012-04-09T03:08:12+08:45[Australia/Eucla]\"}",
                    DateTimeType.valueOf("2012-04-09T03:08:12+08:45[Australia/Eucla]"), ZoneId.of("America/Manaus")),
            new CommandTestCase("ItemG", new DecimalType("92.942", Locale.ROOT), SOURCE,
                    "{\"type\":\"Decimal\",\"value\":\"92.942\"}", new DecimalType(92.942), null),
            new CommandTestCase("ItemH", new PercentType("13", Locale.ROOT), SOURCE,
                    "{\"type\":\"Percent\",\"value\":\"13\"}", new PercentType(13), null),
            new CommandTestCase("ItemI", HSBType.GREEN, SOURCE, "{\"type\":\"HSB\",\"value\":\"120,100,100\"}",
                    HSBType.GREEN, null),
            new CommandTestCase("ItemJ", IncreaseDecreaseType.INCREASE, SOURCE,
                    "{\"type\":\"IncreaseDecrease\",\"value\":\"INCREASE\"}", IncreaseDecreaseType.INCREASE, null),
            new CommandTestCase("ItemK", NextPreviousType.PREVIOUS, SOURCE,
                    "{\"type\":\"NextPrevious\",\"value\":\"PREVIOUS\"}", NextPreviousType.PREVIOUS, null),
            new CommandTestCase("ItemL", OpenClosedType.CLOSED, SOURCE,
                    "{\"type\":\"OpenClosed\",\"value\":\"CLOSED\"}", OpenClosedType.CLOSED, null),
            new CommandTestCase("ItemM", PlayPauseType.PLAY, SOURCE, "{\"type\":\"PlayPause\",\"value\":\"PLAY\"}",
                    PlayPauseType.PLAY, null),
            new CommandTestCase("ItemN", new PointType("52.4791061,62.1830008,385"), SOURCE,
                    "{\"type\":\"Point\",\"value\":\"52.4791061,62.1830008,385\"}",
                    new PointType("52.4791061,62.1830008,385"), null),
            new CommandTestCase("ItemO", new QuantityType<>("366m", Locale.ROOT), SOURCE,
                    "{\"type\":\"Quantity\",\"value\":\"366 m\"}", new QuantityType<>(366, SIUnits.METRE), null),
            new CommandTestCase("ItemP", RefreshType.REFRESH, SOURCE, "{\"type\":\"Refresh\",\"value\":\"REFRESH\"}",
                    RefreshType.REFRESH, null),
            new CommandTestCase("ItemQ", RewindFastforwardType.FASTFORWARD, SOURCE,
                    "{\"type\":\"RewindFastforward\",\"value\":\"FASTFORWARD\"}", RewindFastforwardType.FASTFORWARD,
                    null),
            new CommandTestCase("ItemR", StopMoveType.MOVE, SOURCE, "{\"type\":\"StopMove\",\"value\":\"MOVE\"}",
                    StopMoveType.MOVE, null),
            new CommandTestCase("ItemS", UpDownType.DOWN, SOURCE, "{\"type\":\"UpDown\",\"value\":\"DOWN\"}",
                    UpDownType.DOWN, null),
            new CommandTestCase("ItemT", StringListType.valueOf("Foo,Bar,Foreva"), SOURCE,
                    "{\"type\":\"StringList\",\"value\":\"Foo,Bar,Foreva\"}",
                    new StringListType(List.of("Foo", "Bar", "Foreva")), null),
            new CommandTestCase("ItemU", StringType.valueOf("Foobar"), SOURCE,
                    "{\"type\":\"String\",\"value\":\"Foobar\"}", new StringType("Foobar"), null));

    public static final List<StateTestCase> STATE_TEST_SOURCE = List.of(
            new StateTestCase("ItemA", OnOffType.OFF, SOURCE, "{\"type\":\"OnOff\",\"value\":\"OFF\"}", OnOffType.OFF,
                    null),
            new StateTestCase("ItemB", OnOffType.ON, SOURCE, "{\"type\":\"OnOff\",\"value\":\"ON\"}", OnOffType.ON,
                    null),
            new StateTestCase("ItemC", new DateTimeType("?2019-11-11T13:50Z").toZone(ZoneId.of("Africa/Addis_Ababa")),
                    SOURCE, "{\"type\":\"DateTime\",\"value\":\"2019-11-11T16:50:00+03:00[Africa/Addis_Ababa]\"}",
                    DateTimeType.valueOf("2019-11-11T16:50:00+03:00[Africa/Addis_Ababa]"), null),
            new StateTestCase("ItemD", DateTimeType.valueOf("?2019-11-11T13:50:23FJT"), SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"?2019-11-11T13:50:23+13:00[Pacific/Fiji]\"}",
                    DateTimeType.valueOf("2019-11-11T01:50:23+01:00[CET]"), ZoneId.of("CET")),
            new StateTestCase("ItemE", DateTimeType.valueOf("?2019-11-11T18:23:12FJT"), SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"?2019-11-11T18:23:12+13:00[Pacific/Fiji]\"}",
                    DateTimeType.valueOf("2019-11-11T11:08:12+05:45[Asia/Kathmandu]"), ZoneId.of("Asia/Kathmandu")),
            new StateTestCase("ItemF", DateTimeType.valueOf("2015-11-11T18:23:12+00:00[Australia/Eucla]"), SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"2015-11-12T03:08:12+08:45[Australia/Eucla]\"}",
                    DateTimeType.valueOf("2015-11-12T03:08:12+08:45[Australia/Eucla]"), ZoneId.of("America/Manaus")),
            new StateTestCase("ItemG", new DecimalType("666.666E6", Locale.ROOT), SOURCE,
                    "{\"type\":\"Decimal\",\"value\":\"666666000\"}", new DecimalType(666666000), null),
            new StateTestCase("ItemH", new PercentType("99", Locale.ROOT), SOURCE,
                    "{\"type\":\"Percent\",\"value\":\"99\"}", new PercentType(99), null),
            new StateTestCase("ItemI", HSBType.BLUE, SOURCE, "{\"type\":\"HSB\",\"value\":\"240,100,100\"}",
                    HSBType.BLUE, null),
            new StateTestCase("ItemJ",
                    new RawType(new byte[] { (byte) 0xe5, (byte) 0x6b, (byte) 0xf3, (byte) 0x24 },
                            "application/octet-stream"),
                    SOURCE,
                    "{\"type\":\"Raw\",\"value\":\"data:application/octet-stream;base64,5WvzJA\\u003d\\u003d\"}",
                    RawType.valueOf("data:application/octet-stream;base64,5WvzJA=="), null),
            new StateTestCase("ItemK", UnDefType.NULL, SOURCE, "{\"type\":\"UnDef\",\"value\":\"NULL\"}",
                    UnDefType.NULL, null),
            new StateTestCase("ItemL", UnDefType.UNDEF, SOURCE, "{\"type\":\"UnDef\",\"value\":\"UNDEF\"}",
                    UnDefType.UNDEF, null),
            new StateTestCase("ItemM", OpenClosedType.OPEN, SOURCE, "{\"type\":\"OpenClosed\",\"value\":\"OPEN\"}",
                    OpenClosedType.OPEN, null),
            new StateTestCase("ItemN", PlayPauseType.PAUSE, SOURCE, "{\"type\":\"PlayPause\",\"value\":\"PAUSE\"}",
                    PlayPauseType.PAUSE, null),
            new StateTestCase("ItemO", new PointType("52.4791061,62.1830008,385"), SOURCE,
                    "{\"type\":\"Point\",\"value\":\"52.4791061,62.1830008,385\"}",
                    new PointType("52.4791061,62.1830008,385"), null),
            new StateTestCase("ItemP", new QuantityType<>("366kPa", Locale.ROOT), SOURCE,
                    "{\"type\":\"Quantity\",\"value\":\"366 kPa\"}",
                    new QuantityType<>(366, Units.PASCAL.prefix(MetricPrefix.KILO)), null),
            new StateTestCase("ItemQ", RewindFastforwardType.REWIND, SOURCE,
                    "{\"type\":\"RewindFastforward\",\"value\":\"REWIND\"}", RewindFastforwardType.REWIND, null),
            new StateTestCase("ItemR", UpDownType.UP, SOURCE, "{\"type\":\"UpDown\",\"value\":\"UP\"}", UpDownType.UP,
                    null),
            new StateTestCase("ItemS", StringListType.valueOf("Foo,Bar,Neva"), SOURCE,
                    "{\"type\":\"StringList\",\"value\":\"Foo,Bar,Neva\"}",
                    new StringListType(List.of("Foo", "Bar", "Neva")), null),
            new StateTestCase("ItemT", StringType.valueOf("Boofar"), SOURCE,
                    "{\"type\":\"String\",\"value\":\"Boofar\"}", new StringType("Boofar"), null));

    public static final List<StateChangedTestCase> STATE_CHANGED_TEST_SOURCE = List.of(new StateChangedTestCase("ItemA",
            OnOffType.OFF, OnOffType.ON, ZonedDateTime.parse("2026-02-02T17:28:34-04:00"),
            ZonedDateTime.parse("2026-02-02T17:24:12-04:00"), SOURCE,
            "{\"type\":\"OnOff\",\"value\":\"ON\",\"oldType\":\"OnOff\",\"oldValue\":\"OFF\",\"lastStateUpdate\":\"2026-02-02T17:28:34-04:00\",\"lastStateChange\":\"2026-02-02T17:24:12-04:00\"}",
            OnOffType.OFF, OnOffType.ON, null),
            new StateChangedTestCase("ItemB", new DateTimeType("?2019-11-11T09:40Z").toZone(ZoneId.of("Africa/Cairo")),
                    new DateTimeType("?2019-11-11T13:50Z").toZone(ZoneId.of("Africa/Addis_Ababa")),
                    ZonedDateTime.parse("2026-02-02T17:28:34-04:00"), ZonedDateTime.parse("2026-02-02T17:24:12-04:00"),
                    SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"2019-11-11T16:50:00+03:00[Africa/Addis_Ababa]\",\"oldType\":\"DateTime\",\"oldValue\":\"2019-11-11T11:40:00+02:00[Africa/Cairo]\",\"lastStateUpdate\":\"2026-02-02T17:28:34-04:00\",\"lastStateChange\":\"2026-02-02T17:24:12-04:00\"}",
                    DateTimeType.valueOf("2019-11-11T11:40:00+02:00[Africa/Cairo]"),
                    DateTimeType.valueOf("2019-11-11T16:50:00+03:00[Africa/Addis_Ababa]"), null),
            new StateChangedTestCase("ItemC", DateTimeType.valueOf("?2019-11-11T10:20:27FJT"),
                    DateTimeType.valueOf("?2019-11-11T13:50:23FJT"), ZonedDateTime.parse("2026-02-02T13:48:34-08:00"),
                    ZonedDateTime.parse("2026-02-02T13:34:12-08:00"), SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"?2019-11-11T13:50:23+13:00[Pacific/Fiji]\",\"oldType\":\"DateTime\",\"oldValue\":\"?2019-11-11T10:20:27+13:00[Pacific/Fiji]\",\"lastStateUpdate\":\"2026-02-02T13:48:34-08:00\",\"lastStateChange\":\"2026-02-02T13:34:12-08:00\"}",
                    DateTimeType.valueOf("2019-11-10T22:20:27+01:00[CET]"),
                    DateTimeType.valueOf("2019-11-11T01:50:23+01:00[CET]"), ZoneId.of("CET")),
            new StateChangedTestCase("ItemD", DateTimeType.valueOf("?2019-11-11T09:23:12FJT"),
                    DateTimeType.valueOf("?2019-11-11T18:23:12FJT"), ZonedDateTime.parse("2026-02-02T13:48:34+08:45"),
                    ZonedDateTime.parse("2026-02-02T13:34:12+08:45"), SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"?2019-11-11T18:23:12+13:00[Pacific/Fiji]\",\"oldType\":\"DateTime\",\"oldValue\":\"?2019-11-11T09:23:12+13:00[Pacific/Fiji]\",\"lastStateUpdate\":\"2026-02-02T13:48:34+08:45\",\"lastStateChange\":\"2026-02-02T13:34:12+08:45\"}",
                    DateTimeType.valueOf("2019-11-11T02:08:12+05:45[Asia/Kathmandu]"),
                    DateTimeType.valueOf("2019-11-11T11:08:12+05:45[Asia/Kathmandu]"), ZoneId.of("Asia/Kathmandu")),
            new StateChangedTestCase("ItemE", DateTimeType.valueOf("2015-11-11T13:00:01+00:00[Australia/Eucla]"),
                    DateTimeType.valueOf("2015-11-11T18:23:12+00:00[Australia/Eucla]"),
                    ZonedDateTime.parse("2026-02-02T13:48:34+08:45"), ZonedDateTime.parse("2026-02-02T13:34:12+08:45"),
                    SOURCE,
                    "{\"type\":\"DateTime\",\"value\":\"2015-11-12T03:08:12+08:45[Australia/Eucla]\",\"oldType\":\"DateTime\",\"oldValue\":\"2015-11-11T21:45:01+08:45[Australia/Eucla]\",\"lastStateUpdate\":\"2026-02-02T13:48:34+08:45\",\"lastStateChange\":\"2026-02-02T13:34:12+08:45\"}",
                    DateTimeType.valueOf("2015-11-11T21:45:01+08:45[Australia/Eucla]"),
                    DateTimeType.valueOf("2015-11-12T03:08:12+08:45[Australia/Eucla]"), ZoneId.of("America/Manaus")),
            new StateChangedTestCase("ItemF", new DecimalType("333.333E6", Locale.ROOT),
                    new DecimalType("666.666E6", Locale.ROOT), ZonedDateTime.parse("2026-02-02T15:36:58-05:00"),
                    ZonedDateTime.parse("2026-02-02T14:34:03-05:00"), SOURCE,
                    "{\"type\":\"Decimal\",\"value\":\"666666000\",\"oldType\":\"Decimal\",\"oldValue\":\"333333000\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    new DecimalType(333333000), new DecimalType(666666000), null),
            new StateChangedTestCase("ItemG", new PercentType("13", Locale.ROOT), new PercentType("99", Locale.ROOT),
                    ZonedDateTime.parse("2026-02-02T15:36:58-05:00"), ZonedDateTime.parse("2026-02-02T14:34:03-05:00"),
                    SOURCE,
                    "{\"type\":\"Percent\",\"value\":\"99\",\"oldType\":\"Percent\",\"oldValue\":\"13\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    new PercentType(13), new PercentType(99), null),
            new StateChangedTestCase("ItemH", HSBType.RED, HSBType.BLUE,
                    ZonedDateTime.parse("2026-02-02T15:36:58-05:00"), ZonedDateTime.parse("2026-02-02T14:34:03-05:00"),
                    SOURCE,
                    "{\"type\":\"HSB\",\"value\":\"240,100,100\",\"oldType\":\"HSB\",\"oldValue\":\"0,100,100\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    HSBType.RED, HSBType.BLUE, null),
            new StateChangedTestCase("ItemI",
                    new RawType(new byte[] { (byte) 0x23, (byte) 0xb3, (byte) 0x13, (byte) 0xb4 },
                            "application/octet-stream"),
                    new RawType(new byte[] { (byte) 0xe5, (byte) 0x6b, (byte) 0xf3, (byte) 0x24 },
                            "application/octet-stream"),
                    ZonedDateTime.parse("2026-02-02T15:36:58-05:00"), ZonedDateTime.parse("2026-02-02T14:34:03-05:00"),
                    SOURCE,
                    "{\"type\":\"Raw\",\"value\":\"data:application/octet-stream;base64,5WvzJA\\u003d\\u003d\",\"oldType\":\"Raw\",\"oldValue\":\"data:application/octet-stream;base64,I7MTtA\\u003d\\u003d\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    RawType.valueOf("data:application/octet-stream;base64,I7MTtA=="),
                    RawType.valueOf("data:application/octet-stream;base64,5WvzJA=="), null),
            new StateChangedTestCase("ItemJ", UnDefType.NULL, UnDefType.UNDEF,
                    ZonedDateTime.parse("2026-02-02T15:36:58-05:00"), ZonedDateTime.parse("2026-02-02T14:34:03-05:00"),
                    SOURCE,
                    "{\"type\":\"UnDef\",\"value\":\"UNDEF\",\"oldType\":\"UnDef\",\"oldValue\":\"NULL\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    UnDefType.NULL, UnDefType.UNDEF, null),
            new StateChangedTestCase("ItemK", UnDefType.UNDEF, OpenClosedType.OPEN,
                    ZonedDateTime.parse("2026-02-02T15:36:58-05:00"), ZonedDateTime.parse("2026-02-02T14:34:03-05:00"),
                    SOURCE,
                    "{\"type\":\"OpenClosed\",\"value\":\"OPEN\",\"oldType\":\"UnDef\",\"oldValue\":\"UNDEF\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    UnDefType.UNDEF, OpenClosedType.OPEN, null),
            new StateChangedTestCase("ItemL", PlayPauseType.PLAY, PlayPauseType.PAUSE,
                    ZonedDateTime.parse("2026-02-02T15:36:58-05:00"), ZonedDateTime.parse("2026-02-02T14:34:03-05:00"),
                    SOURCE,
                    "{\"type\":\"PlayPause\",\"value\":\"PAUSE\",\"oldType\":\"PlayPause\",\"oldValue\":\"PLAY\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    PlayPauseType.PLAY, PlayPauseType.PAUSE, null),
            new StateChangedTestCase("ItemM", new PointType("52.4791061,62.1830008,385"),
                    new PointType("50.54639,30.235,132"), ZonedDateTime.parse("2026-02-02T15:36:58-05:00"),
                    ZonedDateTime.parse("2026-02-02T14:34:03-05:00"), SOURCE,
                    "{\"type\":\"Point\",\"value\":\"50.54639,30.235,132\",\"oldType\":\"Point\",\"oldValue\":\"52.4791061,62.1830008,385\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    new PointType("52.4791061,62.1830008,385"), new PointType("50.54639,30.235,132"), null),
            new StateChangedTestCase("ItemN", new QuantityType<>("400Pa", Locale.ROOT),
                    new QuantityType<>("366kPa", Locale.ROOT), ZonedDateTime.parse("2026-02-02T15:36:58-05:00"),
                    ZonedDateTime.parse("2026-02-02T14:34:03-05:00"), SOURCE,
                    "{\"type\":\"Quantity\",\"value\":\"366 kPa\",\"oldType\":\"Quantity\",\"oldValue\":\"400 Pa\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    new QuantityType<>(400, Units.PASCAL),
                    new QuantityType<>(366, Units.PASCAL.prefix(MetricPrefix.KILO)), null),
            new StateChangedTestCase("ItemO", RewindFastforwardType.FASTFORWARD, RewindFastforwardType.REWIND,
                    ZonedDateTime.parse("2026-02-02T15:36:58-05:00"), ZonedDateTime.parse("2026-02-02T14:34:03-05:00"),
                    SOURCE,
                    "{\"type\":\"RewindFastforward\",\"value\":\"REWIND\",\"oldType\":\"RewindFastforward\",\"oldValue\":\"FASTFORWARD\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    RewindFastforwardType.FASTFORWARD, RewindFastforwardType.REWIND, null),
            new StateChangedTestCase("ItemP", UpDownType.DOWN, UpDownType.UP,
                    ZonedDateTime.parse("2026-02-02T15:36:58-05:00"), ZonedDateTime.parse("2026-02-02T14:34:03-05:00"),
                    SOURCE,
                    "{\"type\":\"UpDown\",\"value\":\"UP\",\"oldType\":\"UpDown\",\"oldValue\":\"DOWN\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    UpDownType.DOWN, UpDownType.UP, null),
            new StateChangedTestCase("ItemQ", StringListType.valueOf("Foo,Bar"), StringListType.valueOf("Foo,Bar,Neva"),
                    ZonedDateTime.parse("2026-02-02T15:36:58-05:00"), ZonedDateTime.parse("2026-02-02T14:34:03-05:00"),
                    SOURCE,
                    "{\"type\":\"StringList\",\"value\":\"Foo,Bar,Neva\",\"oldType\":\"StringList\",\"oldValue\":\"Foo,Bar\",\"lastStateUpdate\":\"2026-02-02T15:36:58-05:00\",\"lastStateChange\":\"2026-02-02T14:34:03-05:00\"}",
                    new StringListType(List.of("Foo", "Bar")), new StringListType(List.of("Foo", "Bar", "Neva")), null),
            new StateChangedTestCase("ItemR", StringType.valueOf("Foobar"), StringType.valueOf("Boofar"),
                    ZonedDateTime.parse("2026-02-03T17:28:34-04:00"), ZonedDateTime.parse("2026-02-03T17:24:12-04:00"),
                    SOURCE,
                    "{\"type\":\"String\",\"value\":\"Boofar\",\"oldType\":\"String\",\"oldValue\":\"Foobar\",\"lastStateUpdate\":\"2026-02-03T17:28:34-04:00\",\"lastStateChange\":\"2026-02-03T17:24:12-04:00\"}",
                    StringType.valueOf("Foobar"), StringType.valueOf("Boofar"), null));

    public static final List<TimeSeriesTestCase> TIMESERIES_TEST_SOURCE = List.of(
            new TimeSeriesTestCase("ItemA", Policy.ADD,
                    List.of(new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:34Z"), OnOffType.OFF,
                            OnOffType.OFF),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:35Z"), OnOffType.ON, OnOffType.ON),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:36Z"),
                                    new DateTimeType("?2019-11-11T13:50Z").toZone(ZoneId.of("Africa/Addis_Ababa")),
                                    DateTimeType.valueOf("2019-11-11T16:50:00+03:00[Africa/Addis_Ababa]")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:37Z"),
                                    DateTimeType.valueOf("?2019-11-11T13:50:23FJT"),
                                    DateTimeType.valueOf("2019-11-11T14:50:23+14:00[Pacific/Apia]")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:38Z"),
                                    DateTimeType.valueOf("?2019-11-11T18:23:12GET"),
                                    DateTimeType.valueOf("2019-11-12T04:23:12+14:00[Pacific/Apia]")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:39Z"),
                                    DateTimeType.valueOf("2015-11-11T18:23:12+00:00[Australia/Eucla]"),
                                    DateTimeType.valueOf("2015-11-12T03:08:12+08:45[Australia/Eucla]")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:40Z"),
                                    new DecimalType("666.666E6", Locale.ROOT), new DecimalType(666666000)),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:41Z"),
                                    new PercentType("99", Locale.ROOT), new PercentType(99)),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:42Z"), HSBType.BLUE, HSBType.BLUE),
                            new TimeSeriesTestEntry(
                                    Instant.parse("2026-02-03T10:28:43Z"),
                                    new RawType(new byte[] { (byte) 0xe5, (byte) 0x6b, (byte) 0xf3, (byte) 0x24 },
                                            "application/octet-stream"),
                                    RawType.valueOf("data:application/octet-stream;base64,5WvzJA==")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:44Z"), UnDefType.NULL,
                                    UnDefType.NULL),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:45Z"), UnDefType.UNDEF,
                                    UnDefType.UNDEF),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:46Z"), OpenClosedType.OPEN,
                                    OpenClosedType.OPEN),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:47Z"), PlayPauseType.PAUSE,
                                    PlayPauseType.PAUSE),
                            new TimeSeriesTestEntry(
                                    Instant.parse("2026-02-03T10:28:48Z"), new PointType("52.4791061,62.1830008,385"),
                                    new PointType("52.4791061,62.1830008,385")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:49Z"),
                                    new QuantityType<>("366kPa", Locale.ROOT),
                                    new QuantityType<>(366, Units.PASCAL.prefix(MetricPrefix.KILO))),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:50Z"), RewindFastforwardType.REWIND,
                                    RewindFastforwardType.REWIND),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:51Z"), UpDownType.UP,
                                    UpDownType.UP),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:52Z"),
                                    StringListType.valueOf("Foo,Bar,Neva"),
                                    new StringListType(List.of("Foo", "Bar", "Neva"))),
                            new TimeSeriesTestEntry(
                                    Instant.parse("2026-02-03T10:28:53Z"), StringType.valueOf("Boofar"),
                                    new StringType("Boofar"))),
                    "{\"timeSeries\":[{\"type\":\"OnOff\",\"value\":\"OFF\",\"timestamp\":\"2026-02-03T10:28:34Z\"},{\"type\":\"OnOff\",\"value\":\"ON\",\"timestamp\":\"2026-02-03T10:28:35Z\"},{\"type\":\"DateTime\",\"value\":\"2019-11-11T16:50:00+03:00[Africa/Addis_Ababa]\",\"timestamp\":\"2026-02-03T10:28:36Z\"},{\"type\":\"DateTime\",\"value\":\"?2019-11-11T13:50:23+13:00[Pacific/Fiji]\",\"timestamp\":\"2026-02-03T10:28:37Z\"},{\"type\":\"DateTime\",\"value\":\"?2019-11-11T18:23:12+04:00[Asia/Tbilisi]\",\"timestamp\":\"2026-02-03T10:28:38Z\"},{\"type\":\"DateTime\",\"value\":\"2015-11-12T03:08:12+08:45[Australia/Eucla]\",\"timestamp\":\"2026-02-03T10:28:39Z\"},{\"type\":\"Decimal\",\"value\":\"666666000\",\"timestamp\":\"2026-02-03T10:28:40Z\"},{\"type\":\"Percent\",\"value\":\"99\",\"timestamp\":\"2026-02-03T10:28:41Z\"},{\"type\":\"HSB\",\"value\":\"240,100,100\",\"timestamp\":\"2026-02-03T10:28:42Z\"},{\"type\":\"Raw\",\"value\":\"data:application/octet-stream;base64,5WvzJA\\u003d\\u003d\",\"timestamp\":\"2026-02-03T10:28:43Z\"},{\"type\":\"UnDef\",\"value\":\"NULL\",\"timestamp\":\"2026-02-03T10:28:44Z\"},{\"type\":\"UnDef\",\"value\":\"UNDEF\",\"timestamp\":\"2026-02-03T10:28:45Z\"},{\"type\":\"OpenClosed\",\"value\":\"OPEN\",\"timestamp\":\"2026-02-03T10:28:46Z\"},{\"type\":\"PlayPause\",\"value\":\"PAUSE\",\"timestamp\":\"2026-02-03T10:28:47Z\"},{\"type\":\"Point\",\"value\":\"52.4791061,62.1830008,385\",\"timestamp\":\"2026-02-03T10:28:48Z\"},{\"type\":\"Quantity\",\"value\":\"366 kPa\",\"timestamp\":\"2026-02-03T10:28:49Z\"},{\"type\":\"RewindFastforward\",\"value\":\"REWIND\",\"timestamp\":\"2026-02-03T10:28:50Z\"},{\"type\":\"UpDown\",\"value\":\"UP\",\"timestamp\":\"2026-02-03T10:28:51Z\"},{\"type\":\"StringList\",\"value\":\"Foo,Bar,Neva\",\"timestamp\":\"2026-02-03T10:28:52Z\"},{\"type\":\"String\",\"value\":\"Boofar\",\"timestamp\":\"2026-02-03T10:28:53Z\"}],\"policy\":\"ADD\"}",
                    SOURCE, null),
            new TimeSeriesTestCase("ItemB", Policy.REPLACE,
                    List.of(new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:34Z"), OnOffType.OFF,
                            OnOffType.OFF),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:35Z"), OnOffType.ON, OnOffType.ON),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:36Z"),
                                    new DateTimeType("?2019-11-11T13:50Z").toZone(ZoneId.of("Africa/Addis_Ababa")),
                                    DateTimeType.valueOf("2019-11-11T16:50:00+03:00[Africa/Addis_Ababa]")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:37Z"),
                                    DateTimeType.valueOf("?2019-11-11T13:50:23FJT"),
                                    DateTimeType.valueOf("2019-11-10T20:50:23-04:00[America/Manaus]")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:38Z"),
                                    DateTimeType.valueOf("?2019-11-11T18:23:12GET"),
                                    DateTimeType.valueOf("2019-11-11T10:23:12-04:00[America/Manaus]")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:39Z"),
                                    DateTimeType.valueOf("2015-11-11T18:23:12+00:00[Australia/Eucla]"),
                                    DateTimeType.valueOf("2015-11-12T03:08:12+08:45[Australia/Eucla]")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:40Z"),
                                    new DecimalType("666.666E6", Locale.ROOT), new DecimalType(666666000)),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:41Z"),
                                    new PercentType("99", Locale.ROOT), new PercentType(99)),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:42Z"), HSBType.BLUE, HSBType.BLUE),
                            new TimeSeriesTestEntry(
                                    Instant.parse("2026-02-03T10:28:43Z"),
                                    new RawType(new byte[] { (byte) 0xe5, (byte) 0x6b, (byte) 0xf3, (byte) 0x24 },
                                            "application/octet-stream"),
                                    RawType.valueOf("data:application/octet-stream;base64,5WvzJA==")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:44Z"), UnDefType.NULL,
                                    UnDefType.NULL),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:45Z"), UnDefType.UNDEF,
                                    UnDefType.UNDEF),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:46Z"), OpenClosedType.OPEN,
                                    OpenClosedType.OPEN),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:47Z"), PlayPauseType.PAUSE,
                                    PlayPauseType.PAUSE),
                            new TimeSeriesTestEntry(
                                    Instant.parse("2026-02-03T10:28:48Z"), new PointType("52.4791061,62.1830008,385"),
                                    new PointType("52.4791061,62.1830008,385")),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:49Z"),
                                    new QuantityType<>("366kPa", Locale.ROOT),
                                    new QuantityType<>(366, Units.PASCAL.prefix(MetricPrefix.KILO))),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:50Z"), RewindFastforwardType.REWIND,
                                    RewindFastforwardType.REWIND),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:51Z"), UpDownType.UP,
                                    UpDownType.UP),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:52Z"),
                                    StringListType.valueOf("Foo,Bar,Neva"),
                                    new StringListType(List.of("Foo", "Bar", "Neva"))),
                            new TimeSeriesTestEntry(Instant.parse("2026-02-03T10:28:53Z"), StringType.valueOf("Boofar"),
                                    new StringType("Boofar"))),
                    "{\"timeSeries\":[{\"type\":\"OnOff\",\"value\":\"OFF\",\"timestamp\":\"2026-02-03T10:28:34Z\"},{\"type\":\"OnOff\",\"value\":\"ON\",\"timestamp\":\"2026-02-03T10:28:35Z\"},{\"type\":\"DateTime\",\"value\":\"2019-11-11T16:50:00+03:00[Africa/Addis_Ababa]\",\"timestamp\":\"2026-02-03T10:28:36Z\"},{\"type\":\"DateTime\",\"value\":\"?2019-11-11T13:50:23+13:00[Pacific/Fiji]\",\"timestamp\":\"2026-02-03T10:28:37Z\"},{\"type\":\"DateTime\",\"value\":\"?2019-11-11T18:23:12+04:00[Asia/Tbilisi]\",\"timestamp\":\"2026-02-03T10:28:38Z\"},{\"type\":\"DateTime\",\"value\":\"2015-11-12T03:08:12+08:45[Australia/Eucla]\",\"timestamp\":\"2026-02-03T10:28:39Z\"},{\"type\":\"Decimal\",\"value\":\"666666000\",\"timestamp\":\"2026-02-03T10:28:40Z\"},{\"type\":\"Percent\",\"value\":\"99\",\"timestamp\":\"2026-02-03T10:28:41Z\"},{\"type\":\"HSB\",\"value\":\"240,100,100\",\"timestamp\":\"2026-02-03T10:28:42Z\"},{\"type\":\"Raw\",\"value\":\"data:application/octet-stream;base64,5WvzJA\\u003d\\u003d\",\"timestamp\":\"2026-02-03T10:28:43Z\"},{\"type\":\"UnDef\",\"value\":\"NULL\",\"timestamp\":\"2026-02-03T10:28:44Z\"},{\"type\":\"UnDef\",\"value\":\"UNDEF\",\"timestamp\":\"2026-02-03T10:28:45Z\"},{\"type\":\"OpenClosed\",\"value\":\"OPEN\",\"timestamp\":\"2026-02-03T10:28:46Z\"},{\"type\":\"PlayPause\",\"value\":\"PAUSE\",\"timestamp\":\"2026-02-03T10:28:47Z\"},{\"type\":\"Point\",\"value\":\"52.4791061,62.1830008,385\",\"timestamp\":\"2026-02-03T10:28:48Z\"},{\"type\":\"Quantity\",\"value\":\"366 kPa\",\"timestamp\":\"2026-02-03T10:28:49Z\"},{\"type\":\"RewindFastforward\",\"value\":\"REWIND\",\"timestamp\":\"2026-02-03T10:28:50Z\"},{\"type\":\"UpDown\",\"value\":\"UP\",\"timestamp\":\"2026-02-03T10:28:51Z\"},{\"type\":\"StringList\",\"value\":\"Foo,Bar,Neva\",\"timestamp\":\"2026-02-03T10:28:52Z\"},{\"type\":\"String\",\"value\":\"Boofar\",\"timestamp\":\"2026-02-03T10:28:53Z\"}],\"policy\":\"REPLACE\"}",
                    SOURCE, ZoneId.of("America/Manaus")));

    @ParameterizedTest
    @FieldSource("COMMAND_TEST_SOURCE")
    public void testCreateEventItemCommandEvent(CommandTestCase testCase) throws Exception {
        String topic = "openhab/items/" + testCase.itemName + "/command";
        ZoneId zone = testCase.zone;
        if (zone != null) {
            when(timeZoneProvider.getTimeZone()).thenReturn(zone);
        }
        ItemCommandEvent commandEvent = ItemEventFactory.createCommandEvent(testCase.itemName, testCase.command,
                testCase.source);
        Event event = factory.createEvent(commandEvent.getType(), commandEvent.getTopic(), commandEvent.getPayload(),
                commandEvent.getSource());

        assertEquals(ItemCommandEvent.class, event.getClass());
        ItemCommandEvent itemCommandEvent = (ItemCommandEvent) event;
        assertEquals(ItemCommandEvent.TYPE, itemCommandEvent.getType());
        assertEquals(topic, itemCommandEvent.getTopic());
        assertEquals(testCase.expectedPayload, itemCommandEvent.getPayload());
        assertEquals(testCase.itemName, itemCommandEvent.getItemName());
        assertEquals(testCase.source, itemCommandEvent.getSource());
        assertEquals(testCase.command.getClass(), itemCommandEvent.getItemCommand().getClass());
        assertEquals(testCase.expectedCommand.toFullString(), itemCommandEvent.getItemCommand().toFullString());
        assertEquals(testCase.expectedCommand, itemCommandEvent.getItemCommand());
    }

    @ParameterizedTest
    @FieldSource("STATE_TEST_SOURCE")
    public void testCreateEventItemStateEvent(StateTestCase testCase) throws Exception {
        String topic = "openhab/items/" + testCase.itemName + "/state";
        ZoneId zone = testCase.zone;
        if (zone != null) {
            when(timeZoneProvider.getTimeZone()).thenReturn(zone);
        }
        ItemStateEvent stateEvent = ItemEventFactory.createStateEvent(testCase.itemName, testCase.state,
                testCase.source);
        Event event = factory.createEvent(stateEvent.getType(), stateEvent.getTopic(), stateEvent.getPayload(),
                stateEvent.getSource());

        assertEquals(ItemStateEvent.class, event.getClass());
        ItemStateEvent itemStateEvent = (ItemStateEvent) event;
        assertEquals(ItemStateEvent.TYPE, itemStateEvent.getType());
        assertEquals(topic, itemStateEvent.getTopic());
        assertEquals(testCase.expectedPayload, itemStateEvent.getPayload());
        assertEquals(testCase.itemName, itemStateEvent.getItemName());
        assertEquals(testCase.source, itemStateEvent.getSource());
        assertEquals(testCase.state.getClass(), itemStateEvent.getItemState().getClass());
        assertEquals(testCase.expectedState.toFullString(), itemStateEvent.getItemState().toFullString());
        assertEquals(testCase.expectedState, itemStateEvent.getItemState());

        ZonedDateTime zdt = ZonedDateTime.parse("2026-05-17T12:00:00+01:00[CET]");
        ItemStateUpdatedEvent stateUpdatedEvent = ItemEventFactory.createStateUpdatedEvent(testCase.itemName,
                testCase.state, zdt, testCase.source);
        event = factory.createEvent(stateUpdatedEvent.getType(), stateUpdatedEvent.getTopic(),
                stateUpdatedEvent.getPayload(), stateUpdatedEvent.getSource());
        topic += "updated";

        assertEquals(ItemStateUpdatedEvent.class, event.getClass());
        ItemStateUpdatedEvent itemStateUpdatedEvent = (ItemStateUpdatedEvent) event;
        assertEquals(ItemStateUpdatedEvent.TYPE, itemStateUpdatedEvent.getType());
        assertEquals(topic, itemStateUpdatedEvent.getTopic());
        assertEquals(testCase.itemName, itemStateUpdatedEvent.getItemName());
        assertEquals(testCase.state.getClass(), itemStateUpdatedEvent.getItemState().getClass());
        assertEquals(testCase.expectedState.toFullString(), itemStateUpdatedEvent.getItemState().toFullString());
        assertEquals(testCase.expectedState, itemStateUpdatedEvent.getItemState());
        assertEquals(zdt, itemStateUpdatedEvent.lastStateUpdate);

        ItemStatePredictedEvent statePredictedEvent = ItemEventFactory.createStatePredictedEvent(testCase.itemName,
                testCase.state, true);
        event = factory.createEvent(statePredictedEvent.getType(), statePredictedEvent.getTopic(),
                statePredictedEvent.getPayload(), statePredictedEvent.getSource());
        topic = "openhab/items/" + testCase.itemName + "/statepredicted";

        assertEquals(ItemStatePredictedEvent.class, event.getClass());
        ItemStatePredictedEvent itemStatePredictedEvent = (ItemStatePredictedEvent) event;
        assertEquals(ItemStatePredictedEvent.TYPE, itemStatePredictedEvent.getType());
        assertEquals(topic, itemStatePredictedEvent.getTopic());
        assertEquals(testCase.itemName, itemStatePredictedEvent.getItemName());
        assertEquals(testCase.state.getClass(), itemStatePredictedEvent.getPredictedState().getClass());
        assertEquals(testCase.expectedState.toFullString(), itemStatePredictedEvent.getPredictedState().toFullString());
        assertEquals(testCase.expectedState, itemStatePredictedEvent.getPredictedState());
        assertTrue(itemStatePredictedEvent.isConfirmation);

        String groupName = "TestGroup";
        GroupStateUpdatedEvent groupStateUpdatedEvent = ItemEventFactory.createGroupStateUpdatedEvent(groupName,
                testCase.itemName, testCase.state, zdt, testCase.source);
        event = factory.createEvent(groupStateUpdatedEvent.getType(), groupStateUpdatedEvent.getTopic(),
                groupStateUpdatedEvent.getPayload(), groupStateUpdatedEvent.getSource());
        topic = "openhab/items/TestGroup/" + testCase.itemName + "/stateupdated";

        assertEquals(GroupStateUpdatedEvent.class, event.getClass());
        groupStateUpdatedEvent = (GroupStateUpdatedEvent) event;
        assertEquals(GroupStateUpdatedEvent.TYPE, groupStateUpdatedEvent.getType());
        assertEquals(topic, groupStateUpdatedEvent.getTopic());
        assertEquals(testCase.itemName, groupStateUpdatedEvent.getMemberName());
        assertEquals(testCase.state.getClass(), groupStateUpdatedEvent.getItemState().getClass());
        assertEquals(testCase.expectedState.toFullString(), groupStateUpdatedEvent.getItemState().toFullString());
        assertEquals(testCase.expectedState, groupStateUpdatedEvent.getItemState());
        assertEquals(zdt, groupStateUpdatedEvent.lastStateUpdate);
        assertEquals(groupName, groupStateUpdatedEvent.getItemName());
    }

    @ParameterizedTest
    @FieldSource("STATE_CHANGED_TEST_SOURCE")
    public void testCreateEventItemStateChangedEvent(StateChangedTestCase testCase) throws Exception {
        String topic = "openhab/items/" + testCase.itemName + "/statechanged";
        ZoneId zone = testCase.zone;
        if (zone != null) {
            when(timeZoneProvider.getTimeZone()).thenReturn(zone);
        }
        ItemStateChangedEvent stateChangedEvent = ItemEventFactory.createStateChangedEvent(testCase.itemName,
                testCase.newState, testCase.oldState, testCase.lastStateUpdate, testCase.lastStateChange,
                testCase.source);
        Event event = factory.createEvent(stateChangedEvent.getType(), stateChangedEvent.getTopic(),
                stateChangedEvent.getPayload(), stateChangedEvent.getSource());

        assertEquals(ItemStateChangedEvent.class, event.getClass());
        ItemStateChangedEvent itemStateChangedEvent = (ItemStateChangedEvent) event;
        assertEquals(ItemStateChangedEvent.TYPE, itemStateChangedEvent.getType());
        assertEquals(topic, itemStateChangedEvent.getTopic());
        assertEquals(testCase.expectedPayload, itemStateChangedEvent.getPayload());
        assertEquals(testCase.itemName, itemStateChangedEvent.getItemName());
        assertEquals(testCase.source, itemStateChangedEvent.getSource());
        assertEquals(testCase.oldState.getClass(), itemStateChangedEvent.getOldItemState().getClass());
        assertEquals(testCase.newState.getClass(), itemStateChangedEvent.getItemState().getClass());
        assertEquals(testCase.expectedOldState.toFullString(), itemStateChangedEvent.getOldItemState().toFullString());
        assertEquals(testCase.expectedNewState.toFullString(), itemStateChangedEvent.getItemState().toFullString());
        assertEquals(testCase.expectedOldState, itemStateChangedEvent.getOldItemState());
        assertEquals(testCase.expectedNewState, itemStateChangedEvent.getItemState());
        assertEquals(testCase.lastStateChange, itemStateChangedEvent.getLastStateChange());
        assertEquals(testCase.lastStateUpdate, itemStateChangedEvent.getLastStateUpdate());

        String groupName = "TestGroup";
        topic = "openhab/items/TestGroup/" + testCase.itemName + "/statechanged";
        GroupItemStateChangedEvent groupStateChangedEvent = ItemEventFactory.createGroupStateChangedEvent(groupName,
                testCase.itemName, testCase.newState, testCase.oldState, testCase.lastStateUpdate,
                testCase.lastStateChange);
        event = factory.createEvent(groupStateChangedEvent.getType(), groupStateChangedEvent.getTopic(),
                groupStateChangedEvent.getPayload(), groupStateChangedEvent.getSource());

        assertEquals(GroupItemStateChangedEvent.class, event.getClass());
        GroupItemStateChangedEvent groupItemStateChangedEvent = (GroupItemStateChangedEvent) event;
        assertEquals(GroupItemStateChangedEvent.TYPE, groupItemStateChangedEvent.getType());
        assertEquals(topic, groupItemStateChangedEvent.getTopic());
        assertEquals(testCase.expectedPayload, groupItemStateChangedEvent.getPayload());
        assertEquals(groupName, groupItemStateChangedEvent.getItemName());
        assertEquals(testCase.itemName, groupItemStateChangedEvent.getMemberName());
        assertNull(groupItemStateChangedEvent.getSource());
        assertEquals(testCase.oldState.getClass(), groupItemStateChangedEvent.getOldItemState().getClass());
        assertEquals(testCase.newState.getClass(), groupItemStateChangedEvent.getItemState().getClass());
        assertEquals(testCase.expectedOldState.toFullString(),
                groupItemStateChangedEvent.getOldItemState().toFullString());
        assertEquals(testCase.expectedNewState.toFullString(),
                groupItemStateChangedEvent.getItemState().toFullString());
        assertEquals(testCase.expectedOldState, groupItemStateChangedEvent.getOldItemState());
        assertEquals(testCase.expectedNewState, groupItemStateChangedEvent.getItemState());
        assertEquals(testCase.lastStateChange, groupItemStateChangedEvent.getLastStateChange());
        assertEquals(testCase.lastStateUpdate, groupItemStateChangedEvent.getLastStateUpdate());
    }

    @ParameterizedTest
    @FieldSource("TIMESERIES_TEST_SOURCE")
    public void testCreateTimeSeriesEvent(TimeSeriesTestCase testCase) throws Exception {
        String topic = "openhab/items/" + testCase.itemName + "/timeseries";
        ZoneId zone = testCase.zone;
        if (zone != null) {
            when(timeZoneProvider.getTimeZone()).thenReturn(zone);
        }
        TimeSeries timeSeries = new TimeSeries(testCase.policy);
        for (TimeSeriesTestEntry entry : testCase.entries) {
            timeSeries.add(entry.timestamp, entry.state);
        }
        ItemTimeSeriesEvent timeSeriesEvent = ItemEventFactory.createTimeSeriesEvent(testCase.itemName, timeSeries,
                testCase.source);
        Event event = factory.createEvent(timeSeriesEvent.getType(), timeSeriesEvent.getTopic(),
                timeSeriesEvent.getPayload(), timeSeriesEvent.getSource());

        assertEquals(ItemTimeSeriesEvent.class, event.getClass());
        ItemTimeSeriesEvent itemTimeSeriesEvent = (ItemTimeSeriesEvent) event;
        assertEquals(ItemTimeSeriesEvent.TYPE, itemTimeSeriesEvent.getType());
        assertEquals(topic, itemTimeSeriesEvent.getTopic());
        assertEquals(testCase.itemName, itemTimeSeriesEvent.getItemName());
        assertEquals(testCase.expectedPayload, itemTimeSeriesEvent.getPayload());
        assertNull(itemTimeSeriesEvent.getSource());
        timeSeries = itemTimeSeriesEvent.getTimeSeries();
        assertEquals(timeSeries.size(), testCase.entries.size());
        TimeSeriesTestEntry testCaseEntry;
        for (Entry entry : timeSeries.getStates().toList()) {
            testCaseEntry = testCase.entries.stream().filter(e -> e.timestamp.equals(entry.timestamp())).findAny()
                    .orElseThrow();
            assertEquals(testCaseEntry.state.getClass(), entry.state().getClass());
            assertEquals(testCaseEntry.expectedState.toFullString(), entry.state().toFullString());
            assertEquals(testCaseEntry.expectedState, entry.state());
        }

        ItemTimeSeriesUpdatedEvent timeSeriesUpdatedEvent = ItemEventFactory
                .createTimeSeriesUpdatedEvent(testCase.itemName, timeSeries, testCase.source);
        event = factory.createEvent(timeSeriesUpdatedEvent.getType(), timeSeriesUpdatedEvent.getTopic(),
                timeSeriesUpdatedEvent.getPayload(), timeSeriesUpdatedEvent.getSource());
        topic += "updated";

        assertEquals(ItemTimeSeriesUpdatedEvent.class, event.getClass());
        ItemTimeSeriesUpdatedEvent itemTimeSeriesUpdatedEvent = (ItemTimeSeriesUpdatedEvent) event;
        assertEquals(ItemTimeSeriesUpdatedEvent.TYPE, itemTimeSeriesUpdatedEvent.getType());
        assertEquals(topic, itemTimeSeriesUpdatedEvent.getTopic());
        assertEquals(testCase.itemName, itemTimeSeriesUpdatedEvent.getItemName());
        assertNull(itemTimeSeriesUpdatedEvent.getSource());
        timeSeries = itemTimeSeriesUpdatedEvent.getTimeSeries();
        assertEquals(timeSeries.size(), testCase.entries.size());
        for (Entry entry : timeSeries.getStates().toList()) {
            testCaseEntry = testCase.entries.stream().filter(e -> e.timestamp.equals(entry.timestamp())).findAny()
                    .orElseThrow();
            assertEquals(testCaseEntry.state.getClass(), entry.state().getClass());
            assertEquals(testCaseEntry.expectedState.toFullString(), entry.state().toFullString());
            assertEquals(testCaseEntry.expectedState, entry.state());
        }
    }

    @Test
    public void testCreateEventItemAddedEvent() throws Exception {
        Event event = factory.createEvent(ITEM_ADDED_EVENT_TYPE, ITEM_ADDED_EVENT_TOPIC, ITEM_ADDED_EVENT_PAYLOAD,
                null);

        assertEquals(ItemAddedEvent.class, event.getClass());
        ItemAddedEvent itemAddedEvent = (ItemAddedEvent) event;
        assertEquals(ITEM_ADDED_EVENT_TYPE, itemAddedEvent.getType());
        assertEquals(ITEM_ADDED_EVENT_TOPIC, itemAddedEvent.getTopic());
        assertEquals(ITEM_ADDED_EVENT_PAYLOAD, itemAddedEvent.getPayload());
        assertNotNull(itemAddedEvent.getItem());
        assertEquals(ITEM_NAME, itemAddedEvent.getItem().name);
        assertEquals(CoreItemFactory.SWITCH, itemAddedEvent.getItem().type);
    }

    @Test
    public void testCreateAddedEvent() {
        ItemAddedEvent event = ItemEventFactory.createAddedEvent(ITEM);

        assertEquals(ItemAddedEvent.TYPE, event.getType());
        assertEquals(ITEM_ADDED_EVENT_TOPIC, event.getTopic());
        assertNotNull(event.getItem());
        assertEquals(ITEM_NAME, event.getItem().name);
        assertEquals(CoreItemFactory.SWITCH, event.getItem().type);
    }

    @Test
    public void testCreateGroupStateChangedEventRawType() throws Exception {
        ZonedDateTime lastStateUpdate = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime lastStateChange = ZonedDateTime.now(ZoneId.systemDefault()).minusMinutes(1);
        GroupItemStateChangedEvent giEventSource = ItemEventFactory.createGroupStateChangedEvent(GROUP_NAME, ITEM_NAME,
                NEW_RAW_ITEM_STATE, RAW_ITEM_STATE, lastStateUpdate, lastStateChange);

        Event giEventParsed = factory.createEvent(giEventSource.getType(), giEventSource.getTopic(),
                giEventSource.getPayload(), giEventSource.getSource());

        assertEquals(GroupItemStateChangedEvent.class, giEventParsed.getClass());
        GroupItemStateChangedEvent groupItemStateChangedEvent = (GroupItemStateChangedEvent) giEventParsed;

        assertEquals(GROUPITEM_CHANGED_EVENT_TYPE, groupItemStateChangedEvent.getType());
        assertEquals(GROUPITEM_STATE_CHANGED_EVENT_TOPIC, groupItemStateChangedEvent.getTopic());
        assertEquals(giEventSource.getPayload(), groupItemStateChangedEvent.getPayload());
        assertEquals(GROUP_NAME, groupItemStateChangedEvent.getItemName());
        assertEquals(ITEM_NAME, groupItemStateChangedEvent.getMemberName());
        assertNull(groupItemStateChangedEvent.getSource());
        assertEquals(NEW_RAW_ITEM_STATE, groupItemStateChangedEvent.getItemState());
        assertEquals(RAW_ITEM_STATE, groupItemStateChangedEvent.getOldItemState());
        assertEquals(lastStateUpdate, groupItemStateChangedEvent.getLastStateUpdate());
        assertEquals(lastStateChange, groupItemStateChangedEvent.getLastStateChange());
    }
}
