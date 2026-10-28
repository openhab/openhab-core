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
package org.openhab.core.model.sitemap.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.sitemap.sitemap.ModelChart;
import org.openhab.core.model.sitemap.sitemap.ModelCondition;
import org.openhab.core.model.sitemap.sitemap.ModelMapping;
import org.openhab.core.model.sitemap.sitemap.ModelSwitch;
import org.openhab.core.model.sitemap.sitemap.ModelText;
import org.openhab.core.model.sitemap.sitemap.SitemapModel;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * Tests for the sitemap DSL syntax: parsing, validation and serialization (round trip) of the values handled by the
 * custom value converters and the validator.
 * <p>
 * Models are loaded as isolated models, so no sitemap is registered in the sitemap registry.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class SitemapDslTest extends JavaOSGiTest {

    private static final String SITEMAP_MODEL_TYPE = "sitemap";

    private final List<String> loadedModelNames = new ArrayList<>();

    private @NonNullByDefault({}) ModelRepository modelRepository;

    @BeforeEach
    public void setUp() {
        modelRepository = getService(ModelRepository.class);
        assertNotNull(modelRepository);
    }

    @AfterEach
    public void tearDown() {
        loadedModelNames.forEach(modelRepository::removeModel);
        loadedModelNames.clear();
    }

    @ParameterizedTest
    @ValueSource(strings = { "light", "light-bulb", "123", "007", "123:456", "123:a-b", "a:123", "a-b:c-d", "1a",
            "12-34", "material:lightbulb", "iconify:mdi:light-bulb", "oh:123:456" })
    public void iconRoundTrip(String icon) {
        SitemapModel model = parse("sitemap test { Text label=\"Test\" icon=" + icon + " }");
        assertEquals(icon, firstText(model).getIcon());

        String serialized = serialize(model);
        assertTrue(compact(serialized).contains("icon=" + icon), "Icon should be serialized unquoted: " + serialized);

        assertEquals(icon, firstText(parse(serialized)).getIcon());
    }

    @ParameterizedTest
    @ValueSource(strings = { "light", "123", "123:456", "material:lightbulb" })
    public void staticIconRoundTrip(String icon) {
        SitemapModel model = parse("sitemap test { Text label=\"Test\" staticIcon=" + icon + " }");
        assertEquals(icon, firstText(model).getStaticIcon());

        String serialized = serialize(model);
        assertTrue(compact(serialized).contains("staticIcon=" + icon),
                "Static icon should be serialized unquoted: " + serialized);

        assertEquals(icon, firstText(parse(serialized)).getStaticIcon());
    }

    @ParameterizedTest
    @ValueSource(strings = { "light", "123", "123:456", "material:lightbulb" })
    public void quotedIconIsAccepted(String icon) {
        SitemapModel model = parse("sitemap test { Text label=\"Test\" icon=\"" + icon + "\" }");
        assertEquals(icon, firstText(model).getIcon());
    }

    @Test
    public void iconWithFileExtensionIsAccepted() {
        // for backward compatibility
        parse("sitemap test { Text label=\"Test\" icon=\"light-bulb.png\" }");
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "my icon", "a:b:c:d", "a::b", ":a", "a:", "-a", "a:-b", "a b:c" })
    public void invalidQuotedIconIsRejected(String icon) {
        assertParseFails("sitemap test { Text label=\"Test\" icon=\"" + icon + "\" }");
    }

    @ParameterizedTest
    @ValueSource(strings = { "h", "D", "3h", "2D", "P1D", "PT1H", "P1Y2M3DT4H5M6S", "P1D-P2D", "1h-2h", "h-D", "-P2D",
            "-1h", "-D" })
    public void periodRoundTrip(String period) {
        SitemapModel model = parse("sitemap test { Chart item=Temp period=" + period + " }");
        assertEquals(period, firstChart(model).getPeriod());

        String serialized = serialize(model);
        assertTrue(compact(serialized).contains("period=" + period),
                "Period should be serialized unquoted: " + serialized);

        assertEquals(period, firstChart(parse(serialized)).getPeriod());
    }

    @ParameterizedTest
    @ValueSource(strings = { "P1D", "-P2D", "1h-2h" })
    public void quotedPeriodIsAccepted(String period) {
        SitemapModel model = parse("sitemap test { Chart item=Temp period=\"" + period + "\" }");
        assertEquals(period, firstChart(model).getPeriod());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "P", "-", "PT", "P1", "P1DT", "P1D-", "-P", "P-P1D", "P1D-P", "P1D--P2D", "--P2D",
            "-P1D-P2D", "abc", "1", "30m" })
    public void invalidQuotedPeriodIsRejected(String period) {
        assertParseFails("sitemap test { Chart item=Temp period=\"" + period + "\" }");
    }

    @ParameterizedTest
    @ValueSource(strings = { "P", "PT", "P1", "abc", "-P1D-P2D" })
    public void invalidUnquotedPeriodIsRejected(String period) {
        assertParseFails("sitemap test { Chart item=Temp period=" + period + " }");
    }

    @ParameterizedTest
    @ValueSource(strings = { "ON", "ON-OFF", "my_cmd", "1a", "1-2", "1", "-1", "007", "1.5", "-1.5", "0.50" })
    public void commandRoundTrip(String cmd) {
        SitemapModel model = parse("sitemap test { Switch item=Light mappings=[" + cmd + "=\"Label\"] }");
        assertEquals(cmd, firstMapping(model).getCmd());

        String serialized = serialize(model);
        assertFalse(serialized.contains("\"" + cmd + "\""), "Command should be serialized unquoted: " + serialized);

        assertEquals(cmd, firstMapping(parse(serialized)).getCmd());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ON:OFF", "1:0", "-1:1", "1.5:-1.5" })
    public void commandWithReleaseCommandRoundTrip(String cmds) {
        String cmd = cmds.substring(0, cmds.indexOf(':'));
        String releaseCmd = cmds.substring(cmds.indexOf(':') + 1);

        SitemapModel model = parse("sitemap test { Switch item=Light mappings=[" + cmds + "=\"Label\"] }");
        assertEquals(cmd, firstMapping(model).getCmd());
        assertEquals(releaseCmd, firstMapping(model).getReleaseCmd());

        SitemapModel reparsed = parse(serialize(model));
        assertEquals(cmd, firstMapping(reparsed).getCmd());
        assertEquals(releaseCmd, firstMapping(reparsed).getReleaseCmd());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ON OFF", "1.2.3", "1.", "-", "-ON", "", "a:b" })
    public void commandRequiringQuotesRoundTrip(String cmd) {
        SitemapModel model = parse("sitemap test { Switch item=Light mappings=[\"" + cmd + "\"=\"Label\"] }");
        assertEquals(cmd, firstMapping(model).getCmd());

        String serialized = serialize(model);
        assertTrue(serialized.contains("\"" + cmd + "\""), "Command should be serialized quoted: " + serialized);

        assertEquals(cmd, firstMapping(parse(serialized)).getCmd());
    }

    @ParameterizedTest
    @ValueSource(strings = { "test", "my_sitemap", "Sitemap1", "_x", "1test" })
    public void validSitemapName(String name) {
        parse("sitemap " + name + " { Text label=\"Test\" }");
    }

    @ParameterizedTest
    @ValueSource(strings = { "my-sitemap", "1-2" })
    public void invalidSitemapName(String name) {
        List<String> errors = assertParseFails("sitemap " + name + " { Text label=\"Test\" }");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Sitemap name")), "Unexpected errors: " + errors);
    }

    @ParameterizedTest
    @ValueSource(strings = { "Temp", "Temp_1", "_Temp", "gTemp2" })
    public void validWidgetItemName(String item) {
        parse("sitemap test { Text item=" + item + " }");
    }

    @ParameterizedTest
    @ValueSource(strings = { "My-Item", "1Item", "1-2" })
    public void invalidWidgetItemName(String item) {
        List<String> errors = assertParseFails("sitemap test { Text item=" + item + " }");
        assertTrue(errors.stream().anyMatch(e -> e.contains("item name")), "Unexpected errors: " + errors);
    }

    @ParameterizedTest
    @ValueSource(strings = { "visibility=[Temp>10]", "visibility=[Temp>-10]", "visibility=[Temp==-10 AND Humidity<50]",
            "visibility=[Temp==ON]", "labelcolor=[Temp>=10=\"red\"]", "valuecolor=[Temp<-5=\"blue\", \"gray\"]",
            "icon=[Temp>20=sun, rain]" })
    public void validConditionItemName(String rule) {
        parse("sitemap test { Text label=\"Test\" " + rule + " }");
    }

    @ParameterizedTest
    @ValueSource(strings = { "visibility=[My-Item==ON]", "visibility=[1Item>10]", "labelcolor=[My-Item>=10=\"red\"]",
            "valuecolor=[My-Item<5=\"blue\"]", "iconcolor=[1-2==ON=\"blue\"]", "icon=[My-Item>20=sun]" })
    public void invalidConditionItemName(String rule) {
        List<String> errors = assertParseFails("sitemap test { Text label=\"Test\" " + rule + " }");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Item name")), "Unexpected errors: " + errors);
    }

    @ParameterizedTest
    @CsvSource({ "Temp+1,Temp,+,1", "Temp -1,Temp,-,1", "Temp -1.5,Temp,-,1.5" })
    public void conditionWithSignAndNoOperator(String rule, String item, String sign, String state) {
        SitemapModel model = parse("sitemap test { Text label=\"Test\" visibility=[" + rule + "] }");
        ModelCondition c = firstText(model).getVisibility().getElements().getFirst().getConditions().getFirst();
        assertEquals(item, c.getItem());
        assertNull(c.getCondition());
        assertEquals(sign, c.getSign());
        assertEquals(state, c.getState());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Temp-1", "Temp1-2" })
    public void ambiguousConditionIsRejected(String rule) {
        List<String> errors = assertParseFails("sitemap test { Text label=\"Test\" visibility=[" + rule + "] }");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Ambiguous")), "Unexpected errors: " + errors);
    }

    @Test
    public void quotedHyphenatedStateIsAccepted() {
        parse("sitemap test { Text label=\"Test\" visibility=[\"Mode-1\"] }");
    }

    private SitemapModel parse(String dsl) {
        List<String> errors = new ArrayList<>();
        String name = modelRepository.createIsolatedModel(SITEMAP_MODEL_TYPE, toStream(dsl), errors, new ArrayList<>());
        assertNotNull(name, () -> "Parsing failed for '" + dsl + "': " + errors);
        loadedModelNames.add(name);
        EObject model = modelRepository.getModel(name);
        assertInstanceOf(SitemapModel.class, model);
        assertNotNull(model);
        return (SitemapModel) model;
    }

    /**
     * Asserts the DSL is rejected, either because it cannot be parsed (syntax or value converter errors) or because
     * it fails validation.
     *
     * @return the reported errors
     */
    private List<String> assertParseFails(String dsl) {
        List<String> errors = new ArrayList<>();
        String name = modelRepository.createIsolatedModel(SITEMAP_MODEL_TYPE, toStream(dsl), errors, new ArrayList<>());
        if (name != null) {
            loadedModelNames.add(name);
        }
        assertNull(name, () -> "Parsing should have failed for '" + dsl + "'");
        assertFalse(errors.isEmpty(), () -> "No errors reported for '" + dsl + "'");
        return errors;
    }

    /**
     * Serializes the model back to DSL text. A copy is serialized, as serializing moves the model to another resource.
     */
    private String serialize(SitemapModel model) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        modelRepository.generateFileFormat(out, SITEMAP_MODEL_TYPE, EcoreUtil.copy(model));
        return out.toString(StandardCharsets.UTF_8);
    }

    private static ByteArrayInputStream toStream(String dsl) {
        return new ByteArrayInputStream(dsl.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Removes all whitespace, to make assertions independent of the formatter.
     */
    private static String compact(String dsl) {
        return dsl.replaceAll("\\s+", "");
    }

    private static ModelText firstText(SitemapModel model) {
        return assertInstanceOf(ModelText.class, model.getSitemaps().getFirst().getChildren().getFirst());
    }

    private static ModelChart firstChart(SitemapModel model) {
        return assertInstanceOf(ModelChart.class, model.getSitemaps().getFirst().getChildren().getFirst());
    }

    private static ModelMapping firstMapping(SitemapModel model) {
        ModelSwitch widget = assertInstanceOf(ModelSwitch.class,
                model.getSitemaps().getFirst().getChildren().getFirst());
        return widget.getMappings().getElements().getFirst();
    }
}
