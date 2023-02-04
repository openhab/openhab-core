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
package org.openhab.core.thing.binding.generic.converter;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.PlayPauseType;
import org.openhab.core.library.types.PointType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.binding.generic.ChannelHandlerContent;
import org.openhab.core.thing.binding.generic.ChannelTransformation;
import org.openhab.core.thing.binding.generic.ChannelValueConverterConfig;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

/**
 * The {@link ConverterTest} is a test class for state converters
 *
 * @author Jan N. Klug - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class ConverterTest {

    private @Mock @NonNullByDefault({}) Consumer<String> sendValueMock;

    private @Mock @NonNullByDefault({}) Consumer<State> updateStateMock;

    private @Mock @NonNullByDefault({}) Consumer<Command> postCommandMock;

    @Test
    public void numberItemConverter() {
        NumberChannelHandler converter = new NumberChannelHandler(updateStateMock, postCommandMock, sendValueMock,
                new ChannelTransformation(null), new ChannelTransformation(null), new ChannelValueConverterConfig());

        // without unit
        Assertions.assertEquals(Optional.of(new DecimalType(1234)), converter.toState("1234"));

        // unit in transformation result
        Assertions.assertEquals(Optional.of(new QuantityType<>(100, SIUnits.CELSIUS)), converter.toState("100°C"));

        // no valid value
        Assertions.assertEquals(Optional.of(UnDefType.UNDEF), converter.toState("W"));
        Assertions.assertEquals(Optional.of(UnDefType.UNDEF), converter.toState(""));
    }

    @Test
    public void numberItemConverterWithUnit() {
        ChannelValueConverterConfig channelConfig = new ChannelValueConverterConfig();
        channelConfig.unit = "W";
        NumberChannelHandler converter = new NumberChannelHandler(updateStateMock, postCommandMock, sendValueMock,
                new ChannelTransformation(null), new ChannelTransformation(null), channelConfig);

        // without unit
        Assertions.assertEquals(Optional.of(new QuantityType<>(500, Units.WATT)), converter.toState("500"));

        // no valid value
        Assertions.assertEquals(Optional.of(UnDefType.UNDEF), converter.toState("foo"));
        Assertions.assertEquals(Optional.of(UnDefType.UNDEF), converter.toState(""));
    }

    @Test
    public void stringTypeConverter() {
        GenericChannelHandler converter = createConverter(StringType::new);
        Assertions.assertEquals(Optional.of(new StringType("Test")), converter.toState("Test"));
    }

    @Test
    public void decimalTypeConverter() {
        GenericChannelHandler converter = createConverter(DecimalType::new);
        Assertions.assertEquals(Optional.of(new DecimalType(15.6)), converter.toState("15.6"));
    }

    @Test
    public void pointTypeConverter() {
        GenericChannelHandler converter = createConverter(PointType::new);
        Assertions.assertEquals(
                Optional.of(new PointType(new DecimalType(51.1), new DecimalType(7.2), new DecimalType(100))),
                converter.toState("51.1, 7.2, 100"));
    }

    @Test
    public void playerItemTypeConverter() {
        ChannelValueConverterConfig cfg = new ChannelValueConverterConfig();
        cfg.playValue = "PLAY";
        ChannelHandlerContent content = new ChannelHandlerContent("PLAY".getBytes(StandardCharsets.UTF_8), "UTF-8",
                null);
        PlayerChannelHandler converter = new PlayerChannelHandler(updateStateMock, postCommandMock, sendValueMock,
                new ChannelTransformation(null), new ChannelTransformation(null), cfg);
        converter.process(content);
        converter.process(content);

        Mockito.verify(postCommandMock).accept(PlayPauseType.PLAY);
        Mockito.verify(updateStateMock, Mockito.never()).accept(ArgumentMatchers.any());
    }

    @Test
    public void colorItemTypeRGBConverter() {
        ChannelValueConverterConfig cfg = new ChannelValueConverterConfig();
        cfg.colorMode = ColorChannelHandler.ColorMode.RGB;
        ChannelHandlerContent content = new ChannelHandlerContent("123,34,47".getBytes(StandardCharsets.UTF_8), "UTF-8",
                null);
        ColorChannelHandler converter = new ColorChannelHandler(updateStateMock, postCommandMock, sendValueMock,
                new ChannelTransformation(null), new ChannelTransformation(null), cfg);

        converter.process(content);
        Mockito.verify(updateStateMock).accept(HSBType.fromRGB(123, 34, 47));
    }

    @Test
    public void colorItemTypeHSBConverter() {
        ChannelValueConverterConfig cfg = new ChannelValueConverterConfig();
        cfg.colorMode = ColorChannelHandler.ColorMode.HSB;
        ChannelHandlerContent content = new ChannelHandlerContent("123,34,47".getBytes(StandardCharsets.UTF_8), "UTF-8",
                null);
        ColorChannelHandler converter = new ColorChannelHandler(updateStateMock, postCommandMock, sendValueMock,
                new ChannelTransformation(null), new ChannelTransformation(null), cfg);

        converter.process(content);
        Mockito.verify(updateStateMock).accept(new HSBType("123,34,47"));
    }

    @Test
    public void rollerSHutterConverter() {
        ChannelValueConverterConfig cfg = new ChannelValueConverterConfig();
        RollershutterChannelHandler converter = new RollershutterChannelHandler(updateStateMock, postCommandMock,
                sendValueMock, new ChannelTransformation(null), new ChannelTransformation(null), cfg);

        // test 0 and 100
        ChannelHandlerContent content = new ChannelHandlerContent("0".getBytes(StandardCharsets.UTF_8), "UTF-8", null);
        converter.process(content);
        Mockito.verify(updateStateMock).accept(PercentType.ZERO);
        content = new ChannelHandlerContent("100".getBytes(StandardCharsets.UTF_8), "UTF-8", null);
        converter.process(content);
        Mockito.verify(updateStateMock).accept(PercentType.HUNDRED);

        // test under/over-range (expect two times total for zero/100
        content = new ChannelHandlerContent("-1".getBytes(StandardCharsets.UTF_8), "UTF-8", null);
        converter.process(content);
        Mockito.verify(updateStateMock, Mockito.times(2)).accept(PercentType.ZERO);
        content = new ChannelHandlerContent("105".getBytes(StandardCharsets.UTF_8), "UTF-8", null);
        converter.process(content);
        Mockito.verify(updateStateMock, Mockito.times(2)).accept(PercentType.HUNDRED);

        // test value
        content = new ChannelHandlerContent("67".getBytes(StandardCharsets.UTF_8), "UTF-8", null);
        converter.process(content);
        Mockito.verify(updateStateMock).accept(new PercentType(67));
    }

    public GenericChannelHandler createConverter(Function<String, State> fcn) {
        return new GenericChannelHandler(fcn, updateStateMock, postCommandMock, sendValueMock,
                new ChannelTransformation(null), new ChannelTransformation(null), new ChannelValueConverterConfig());
    }
}
