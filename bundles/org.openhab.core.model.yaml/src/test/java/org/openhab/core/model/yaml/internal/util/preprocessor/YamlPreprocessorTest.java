/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.util.preprocessor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * The {@link YamlPreprocessorTest} contains tests for the {@link YamlPreprocessor} class.
 *
 * @author Jimmy Tanagra - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@NonNullByDefault
public class YamlPreprocessorTest {
    private static final Path SOURCE_PATH = Path.of("src/test/resources/model/preprocessor");

    @Test
    public void booleanParserTest() throws IOException {
        Yaml yaml = YamlPreprocessor.newYaml(new HashMap<>());

        assertThat(yaml.load("true"), equalTo(true));
        assertThat(yaml.load("TRUE"), equalTo(true));
        assertThat(yaml.load("True"), equalTo(true));
        assertThat(yaml.load("TrUe"), equalTo(true));

        assertThat(yaml.load("false"), equalTo(false));
        assertThat(yaml.load("False"), equalTo(false));
        assertThat(yaml.load("FALSE"), equalTo(false));
        assertThat(yaml.load("fALsE"), equalTo(false));

        List.of("on", "On", "ON", "oN").forEach(value -> {
            assertThat(yaml.load(value), equalTo(value));
        });

        List.of("off", "Off", "OFF", "oFf").forEach(value -> {
            assertThat(yaml.load(value), equalTo(value));
        });

        List.of("yes", "Yes", "YES", "yEs").forEach(value -> {
            assertThat(yaml.load(value), equalTo(value));
        });

        List.of("no", "No", "NO", "nO").forEach(value -> {
            assertThat(yaml.load(value), equalTo(value));
        });
    }

    @Test
    void anchorsTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor.load(SOURCE_PATH.resolve("anchors.yaml"));
        assertThat(data.get("baz"), equalTo("bar"));
        assertThat(data.get("bar"), equalTo("qux"));
    }

    @Test
    void getNestedValueTest() {
        Map<String, Object> data = Map.of("top", Map.of("level1", Map.of("level2", "value")));
        assertThat(YamlPreprocessor.getNestedValue(data, "top", "level1", "level2"), equalTo("value"));
        assertNull(YamlPreprocessor.getNestedValue(data, "top", "nolevel1", "level2"));
        assertNull(YamlPreprocessor.getNestedValue(data, "top", "level1", "nolevel2"));
    }

    @Test
    // After the preprocessor, the variables and packages sections should be removed
    // from the resulting data structure
    void extraElementsRemovedTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("extraElementsRemoved.yaml"));
        assertNull(data.get("variables"));
        assertNull(data.get("packages"));
    }

    @Test
    public void simpleVariableSubstitutionsTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("simpleVariableSubstitutions.yaml"));
        assertThat(data.get("plainkey"), equalTo("value1"));
        assertThat(data.get("dynamickey"), equalTo("dynamicvalue"));

        Map<String, Object> level1 = (Map<String, Object>) data.get("level1");
        assertThat(level1.get("level2a"), equalTo("value2"));

        Map<String, Object> level2b = (Map<String, Object>) level1.get("level2b");
        assertThat(level2b.get("level3"), equalTo("value3"));

        assertThat(data.get("list"), equalTo(List.of("value4")));
    }

    @Test
    public void nestedVariablesTest() throws IOException {
        Map<String, String> data = (Map<String, String>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("nestedVariables.yaml"));
        assertThat(data.get("key"), equalTo("value"));
    }

    @Test
    void variableSyntaxTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("variableSyntax.yaml"));

        assertThat(data.get("empty_no_default"), equalTo(""));
        assertThat(data.get("absent_no_default"), equalTo(""));

        assertThat(data.get("exists"), equalTo("value1"));
        assertThat(data.get("empty"), equalTo(""));
        assertThat(data.get("absent"), equalTo("default"));

        assertThat(data.get("exists_with_colon"), equalTo("value1"));
        assertThat(data.get("empty_with_colon"), equalTo("default"));
        assertThat(data.get("absent_with_colon"), equalTo("default"));

        assertThat(data.get("absent_default_multiple"), equalTo("foobar"));
        assertThat(data.get("absent_default_multiple_braces"), equalTo("{foo} {bar}"));
        assertThat(data.get("absent_default_braces"), equalTo("{foo}"));
        assertThat(data.get("absent_default_single_quoted"), equalTo("{foo}"));
        assertThat(data.get("absent_default_double_quoted"), equalTo("{foo}"));
        assertThat(data.get("absent_with_empty_default"), equalTo(""));
        assertThat(data.get("absent_with_nested_default"), equalTo("value1"));
        assertThat(data.get("absent_with_nested_default_quoted"), equalTo("value1"));
    }

    @Test
    void include1DeepTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("include1Deep.yaml"));

        assertThat(YamlPreprocessor.getNestedValue(data, "toplevel", "includedkey"), equalTo("value"));
    }

    @Test
    void include2DeepTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("include2Deep.yaml"));

        assertThat(YamlPreprocessor.getNestedValue(data, "toplevel", "level1", "level2"), equalTo("foo"));
    }

    @Test
    void predefinedVarsTest() throws IOException {
        Path sourcePath = SOURCE_PATH.resolve("predefinedVars.yaml");
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor.load(sourcePath);

        assertThat(data.get("file"), equalTo(sourcePath.toAbsolutePath().toString()));
        assertThat(data.get("filename"), equalTo("predefinedVars"));
        assertThat(data.get("ext"), equalTo("yaml"));
        assertThat(data.get("path"), equalTo(sourcePath.toAbsolutePath().getParent().toString()));
    }

    @Test
    void predefinedVarsNotOverridableTest() throws IOException {
        Path sourcePath = SOURCE_PATH.resolve("predefinedVarsNotOverridable.yaml");
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor.load(sourcePath);

        Path file = sourcePath;
        assertThat(data.get("file"), equalTo(file.toAbsolutePath().toString()));
        assertThat(data.get("filename"), equalTo("predefinedVarsNotOverridable"));
        assertThat(data.get("ext"), equalTo("yaml"));
        assertThat(data.get("path"), equalTo(file.toAbsolutePath().getParent().toString()));

        file = sourcePath.resolveSibling("predefinedVarsNotOverridable.inc.yaml");
        assertThat(YamlPreprocessor.getNestedValue(data, "include", "file"), equalTo(file.toAbsolutePath().toString()));
        assertThat(YamlPreprocessor.getNestedValue(data, "include", "filename"),
                equalTo("predefinedVarsNotOverridable.inc"));
        assertThat(YamlPreprocessor.getNestedValue(data, "include", "ext"), equalTo("yaml"));
        assertThat(YamlPreprocessor.getNestedValue(data, "include", "path"),
                equalTo(file.toAbsolutePath().getParent().toString()));
    }

    @Test
    void circularInclusionTest() throws IOException {
        try {
            YamlPreprocessor.load(SOURCE_PATH.resolve("circularInclusion.yaml"));
            fail("Expected an exception to be thrown");
        } catch (YAMLException e) {
            assertThat(e.getMessage(), containsString("Circular inclusion detected"));
        }
    }

    @Test
    void includedTopLevelVarsTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("includedTopLevelVars.yaml"));

        assertThat(YamlPreprocessor.getNestedValue(data, "toplevel", "level1"), equalTo("set_at_toplevel"));
    }

    @Test
    void includedTopLevelFileVarsTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("includedTopLevelFileVars.yaml"));

        assertThat(YamlPreprocessor.getNestedValue(data, "toplevel", "level1"), equalTo("set_at_include_level"));
    }

    @Test
    void varsPropagate2LevelsTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("varsPropagate2Levels.yaml"));

        assertThat(YamlPreprocessor.getNestedValue(data, "toplevel", "data", "data"), equalTo("toplevel"));
    }

    @Test
    void packagesTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor.load(SOURCE_PATH.resolve("packages.yaml"));

        // defined in the package
        assertThat(YamlPreprocessor.getNestedValue(data, "things", "thing1", "label"), equalTo("label1"));
        assertThat(YamlPreprocessor.getNestedValue(data, "things", "thing2", "label"), equalTo("label2"));

        // defined in the main level
        assertThat(YamlPreprocessor.getNestedValue(data, "things", "thing3", "label"), equalTo("label3"));

        // defined in both main and package, they should be merged with values from the main overriding
        // the package
        assertThat(YamlPreprocessor.getNestedValue(data, "things", "thing4", "label"), equalTo("main"));
        assertThat(YamlPreprocessor.getNestedValue(data, "things", "thing4", "config", "mainprop"), equalTo("main"));
        assertThat(YamlPreprocessor.getNestedValue(data, "things", "thing4", "config", "pkgprop"), equalTo("package"));
        assertThat(YamlPreprocessor.getNestedValue(data, "things", "thing4", "config", "commonprop"),
                equalTo("overridden"));

        assertThat(YamlPreprocessor.getNestedValue(data, "list", "test1"), equalTo(List.of("main1", "package1")));
    }
}
