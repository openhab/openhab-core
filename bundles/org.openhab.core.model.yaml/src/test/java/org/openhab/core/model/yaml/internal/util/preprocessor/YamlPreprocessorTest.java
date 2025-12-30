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
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.yaml.snakeyaml.Yaml;

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
        Yaml yaml = YamlPreprocessor.newYaml(new HashMap<>(), SOURCE_PATH.resolve("dummy.yaml"), false);

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
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor.load(SOURCE_PATH.resolve("anchors.yaml"),
                path -> {
                });
        assertThat(data.get("baz"), equalTo("bar"));
        assertThat(data.get("bar"), equalTo("qux"));
    }

    @Test
    void getNestedValueTest() {
        Map<String, Object> data = Map.of("top", Map.of("level1", Map.of("level2", "value")));
        assertThat(getNestedValue(data, "top", "level1", "level2"), equalTo("value"));
        assertNull(getNestedValue(data, "top", "nolevel1", "level2"));
        assertNull(getNestedValue(data, "top", "level1", "nolevel2"));
    }

    @Test
    // After the preprocessor, the variables and packages sections should be removed
    // from the resulting data structure
    void extraElementsRemovedTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("extraElementsRemoved.yaml"), path -> {
                });

        assertThat(data, not(hasKey("variables")));
        assertThat(data, not(hasKey("packages")));
    }

    @Test
    void variableSyntaxTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("variableSyntax.yaml"), path -> {
                });

        assertThat(data.get("simple"), equalTo("value1"));
        assertThat(data.get("double_quoted"), equalTo("value1"));
        assertThat(data.get("single_quoted"), equalTo("${exist}"));

        assertThat(data.get("reserved_word"), equalTo("reserved"));
        assertThat(data.get("simple_spaced_interpolation"), equalTo("reserved"));

        assertThat(data.get("varname_with_dash"), equalTo("dashvalue"));
        assertThat(data.get("varname_with_space"), equalTo("spacevalue"));

        assertThat(data.get("empty_no_default"), equalTo(""));
        assertThat(data.get("absent_no_default"), equalTo(""));

        assertThat(data.get("exists"), equalTo("value1"));
        assertThat(data.get("empty_value_test"), equalTo(""));
        assertThat(data.get("empty_value_test_compound"), equalTo("prefix-"));
        assertThat(data.get("absent"), equalTo("default"));

        assertThat(data.get("exists_with_true"), equalTo("value1"));
        assertThat(data.get("empty_value_with_true"), equalTo("default"));
        assertThat(data.get("absent_with_true"), equalTo("default"));

        assertThat(data.get("absent_default_multiple"), equalTo("foobar"));
        assertThat(data.get("absent_default_multiple_braces"), equalTo("{foo} {bar}"));
        assertThat(data.get("absent_default_braces"), equalTo("{foo}"));
        assertThat(data.get("absent_default_single_quoted"), equalTo("{foo}"));
        assertThat(data.get("absent_default_double_quoted"), equalTo("{foo}"));
        assertThat(data.get("absent_with_empty_default"), equalTo(""));
        assertThat(data.get("absent_with_nested_default"), equalTo("value1"));
        assertThat(data.get("absent_with_nested_default_quoted"), equalTo("value1"));

        assertThat(data.get("chained"), equalTo("VALUE1"));

        assertThat(data.get("compound_value"), equalTo("Value is value1"));
        assertThat(data.get("compound_quoted"), equalTo("Value is value1"));

        assertThat(data.get("multiple_patterns"), equalTo("value1-1"));

        assertThat(data.get("string_var"), instanceOf(String.class));
        assertThat(data.get("string_var"), equalTo("1"));

        assertThat(data.get("string_var_literal"), instanceOf(String.class));
        assertThat(((String) data.get("string_var_literal")).strip(), equalTo("1"));

        assertThat(data.get("string_var_folded"), instanceOf(String.class));
        assertThat(((String) data.get("string_var_folded")).strip(), equalTo("1"));

        assertThat(data.get("int_var"), instanceOf(Integer.class));
        assertThat(data.get("int_var"), equalTo(1));

        assertThat(data.get("map_var"), instanceOf(Map.class));
        assertThat(data.get("map_var"), equalTo(Map.of("foo", "bar", "baz", "qux")));

        assertThat(data.get("map_lookup"), equalTo("bar"));
        assertThat(data.get("map_literal_key"), equalTo("qux"));
        assertThat(data.get("map_literal_key_dbl"), equalTo("bar"));
        assertThat(data.get("map_literal_chained"), equalTo("qux"));

        assertThat(data.get("list_var"), instanceOf(List.class));
        assertThat(data.get("list_var"), equalTo(List.of("item1", "item2")));
        assertThat(data.get("list_index_0"), equalTo("item1"));
        assertThat(data.get("list_index_1"), equalTo("item2"));
        assertThat(data.get("list_index_lookup"), equalTo("item2"));

        assertThat(data.get("foo"), equalTo("dynamickey"));

        assertThat(getNestedValue(data, "level1", "level2a"), equalTo("value1"));
        assertThat(getNestedValue(data, "level1", "level2b", "level3"), equalTo("foo"));

        assertThat(data.get("jinja_expression"), equalTo("barbarian"));
    }

    @Test
    void include1DeepTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor.load(SOURCE_PATH.resolve("include1Deep.yaml"),
                path -> {
                });

        assertThat(getNestedValue(data, "toplevel", "includedkey"), equalTo("value"));
    }

    @Test
    void include2DeepTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor.load(SOURCE_PATH.resolve("include2Deep.yaml"),
                path -> {
                });

        assertThat(getNestedValue(data, "toplevel", "level1", "level2"), equalTo("foo"));
    }

    @Test
    void predefinedVarsTest() throws IOException {
        Path sourcePath = SOURCE_PATH.resolve("predefinedVars.yaml");
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor.load(sourcePath, path -> {
        });

        assertThat(data.get("file"), equalTo(sourcePath.toAbsolutePath().toString()));
        assertThat(data.get("filename"), equalTo("predefinedVars"));
        assertThat(data.get("ext"), equalTo("yaml"));
        assertThat(data.get("path"), equalTo(sourcePath.toAbsolutePath().getParent().toString()));
    }

    @Test
    void predefinedVarsNotOverridableTest() throws IOException {
        Path sourcePath = SOURCE_PATH.resolve("predefinedVarsNotOverridable.yaml");
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor.load(sourcePath, path -> {
        });

        Path file = sourcePath;
        assertThat(data.get("file"), equalTo(file.toAbsolutePath().toString()));
        assertThat(data.get("filename"), equalTo("predefinedVarsNotOverridable"));
        assertThat(data.get("ext"), equalTo("yaml"));
        assertThat(data.get("path"), equalTo(file.toAbsolutePath().getParent().toString()));

        file = sourcePath.resolveSibling("predefinedVarsNotOverridable.inc.yaml");
        assertThat(getNestedValue(data, "include", "file"), equalTo(file.toAbsolutePath().toString()));
        assertThat(getNestedValue(data, "include", "filename"), equalTo("predefinedVarsNotOverridable.inc"));
        assertThat(getNestedValue(data, "include", "ext"), equalTo("yaml"));
        assertThat(getNestedValue(data, "include", "path"), equalTo(file.toAbsolutePath().getParent().toString()));
    }

    @Test
    void circularInclusionTest() {
        IOException exception = assertThrows(IOException.class,
                () -> YamlPreprocessor.load(SOURCE_PATH.resolve("circularInclusion.yaml"), path -> {
                }));
        assertThat(exception.getMessage(), containsString("Circular inclusion detected"));
    }

    @Test
    void includedTopLevelVarsTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("includedTopLevelVars.yaml"), path -> {
                });

        assertThat(getNestedValue(data, "toplevel", "level1"), equalTo("set_at_toplevel"));
    }

    @Test
    void includedTopLevelFileVarsTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("includedTopLevelFileVars.yaml"), path -> {
                });

        assertThat(getNestedValue(data, "toplevel", "level1"), equalTo("set_at_include_level"));
    }

    @Test
    void includeVarsFromGlobalTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("includeVarsFromGlobal.yaml"), path -> {
                });

        // The included file resolves ${bar} where bar is set to ${foo} from globals
        assertThat(getNestedValue(data, "data", "includedkey"), equalTo("globalFoo"));
    }

    @Test
    void varsPropagate2LevelsTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor
                .load(SOURCE_PATH.resolve("varsPropagate2Levels.yaml"), path -> {
                });

        assertThat(getNestedValue(data, "toplevel", "data", "data"), equalTo("toplevel"));
    }

    @Test
    void packagesLoadFromPackagesTest() throws IOException {
        Map<String, Object> data = loadPackages();

        assertThat(getNestedValue(data, "things", "thing1", "label"), equalTo("label1"));
        assertThat(getNestedValue(data, "things", "thing1", "scalar"), equalTo("package"));
        assertThat(getNestedValue(data, "things", "thing1", "map1", "scalar1"), equalTo("package"));
        assertThat(getNestedValue(data, "things", "thing1", "config", "map1", "scalar1"), equalTo("package"));
    }

    @Test
    void packagesInjectPackageIdTest() throws IOException {
        Map<String, Object> data = loadPackages();

        assertThat(getNestedValue(data, "things", "pid_test", "packageid"), equalTo("package_id_test"));
        assertThat(getNestedValue(data, "things", "pid_override", "packageid"), equalTo("id_override"));
    }

    @Test
    void packagesDoNotRemoveNonPackageThingsTest() throws IOException {
        Map<String, Object> data = loadPackages();

        assertThat(getNestedValue(data, "things", "thing_only_in_main", "label"), equalTo("label3"));
    }

    @Test
    void packagesOverwriteBehaviorTest() throws IOException {
        Map<String, Object> data = loadPackages();

        Map<String, Object> thingOverwrite = (Map<String, Object>) getNestedValue(data, "things", "thing_overwrite");

        // Verify that scalar properties overwrite package values
        assertThat(getNestedValue(thingOverwrite, "label"), equalTo("main"));
        assertThat(getNestedValue(thingOverwrite, "scalar"), equalTo("main"));

        // Verify map overwrite - should only have main values, not package values
        assertThat(getNestedValue(thingOverwrite, "map1", "scalar1"), equalTo("main"));
        assertThat((Map<String, Object>) getNestedValue(thingOverwrite, "map1"), not(hasKey("scalar2")));

        // Verify list overwrite - should only have main values, not package values
        assertThat(getNestedValue(thingOverwrite, "list1"), equalTo(List.of("main")));

        // Verify that top-level non-overridden properties from package are retained
        assertThat(getNestedValue(thingOverwrite, "map2", "scalar1"), equalTo("package"));
        assertThat(getNestedValue(thingOverwrite, "map2", "scalar2"), equalTo("package"));
        assertThat(getNestedValue(thingOverwrite, "list2"), equalTo(List.of("package")));

        // Verify nested config overwrite
        assertThat(getNestedValue(thingOverwrite, "config", "scalar1"), equalTo("main"));

        Map<String, Object> configMap = (Map<String, Object>) getNestedValue(thingOverwrite, "config");

        // Verify nested map overwrite - should only have main values, not package values
        assertThat(getNestedValue(configMap, "map1", "mainkey"), equalTo("main"));
        assertThat((Map<String, Object>) getNestedValue(configMap, "map1"), not(hasKey("scalar1")));
        assertThat((Map<String, Object>) getNestedValue(configMap, "map1"), not(hasKey("scalar2")));

        // Verify nested list overwrite - should only have main values, not package values
        assertThat(getNestedValue(configMap, "list1"), equalTo(List.of("main")));
    }

    @Test
    void packagesMergeBehaviorTest() throws IOException {
        Map<String, Object> data = loadPackages();

        Map<String, Object> thingMerge = (Map<String, Object>) getNestedValue(data, "things", "thing_merge");

        // Verify scalar merging
        assertThat(getNestedValue(thingMerge, "config", "scalar1"), equalTo("package"));
        assertThat(getNestedValue(thingMerge, "config", "scalar2"), equalTo("main"));
        assertThat(getNestedValue(thingMerge, "config", "scalar3"), equalTo("new"));

        // Verify map merging (merges by default)
        // map1:scalar1 is not defined in main
        assertThat(getNestedValue(thingMerge, "config", "map1", "scalar1"), equalTo("package"));
        // map1:scalar2 is defined in main -> overwrites package
        assertThat(getNestedValue(thingMerge, "config", "map1", "scalar2"), equalTo("main"));
        // map1:scalar3 is only defined in main
        assertThat(getNestedValue(thingMerge, "config", "map1", "scalar3"), equalTo("main"));

        // Verify list merging (merges by default)
        assertThat(getNestedValue(thingMerge, "config", "list1"), equalTo(List.of("package", "main")));

        // Verify map2 now also merges (recursive merge behavior)
        // map2:mainkey is only defined in main
        assertThat(getNestedValue(thingMerge, "config", "map2", "mainkey"), equalTo("main"));
        // map2:scalar1 and scalar2 are only defined in package
        assertThat(getNestedValue(thingMerge, "config", "map2", "scalar1"), equalTo("package"));
        assertThat(getNestedValue(thingMerge, "config", "map2", "scalar2"), equalTo("package"));

        // Verify list2 is also merged from package, even though not in main
        assertThat(getNestedValue(thingMerge, "config", "list2"), equalTo(List.of("package")));

        // Verify top-level properties
        assertThat(getNestedValue(thingMerge, "scalar"), equalTo("package"));
        assertThat(getNestedValue(thingMerge, "mainscalar"), equalTo("main"));
        assertThat(getNestedValue(thingMerge, "mainmap", "mainkey"), equalTo("main"));
        assertThat(getNestedValue(thingMerge, "mainlist1"), equalTo(List.of("main")));
        assertThat(getNestedValue(thingMerge, "mainlist2"), equalTo(List.of("main")));
    }

    @Test
    void packagesRemoveBehaviorTest() throws IOException {
        Map<String, Object> data = loadPackages();

        Map<String, Object> thingRemove = (Map<String, Object>) getNestedValue(data, "things", "thing_remove");

        // Verify that !remove directive removes top-level keys
        assertThat(thingRemove, not(hasKey("label")));
        assertThat(thingRemove, not(hasKey("list1")));

        Map<String, Object> configMap = (Map<String, Object>) getNestedValue(thingRemove, "config");

        // Verify that !remove directive removes nested keys
        assertThat(configMap, not(hasKey("scalar1")));
        assertThat(configMap, not(hasKey("map1")));
        assertThat(configMap, not(hasKey("list1")));
        assertThat((Map<String, Object>) getNestedValue(configMap, "map2"), not(hasKey("scalar1")));

        // Verify that non-removed keys are retained
        assertThat(getNestedValue(thingRemove, "scalar"), equalTo("package"));
        assertThat(getNestedValue(thingRemove, "map1", "scalar1"), equalTo("package"));
        assertThat(getNestedValue(thingRemove, "map1", "scalar2"), equalTo("package"));
        assertThat(getNestedValue(thingRemove, "list2"), equalTo(List.of("package")));

        assertThat(getNestedValue(configMap, "scalar2"), equalTo("package"));
        assertThat(getNestedValue(configMap, "map2", "scalar2"), equalTo("package"));
        assertThat(getNestedValue(configMap, "list2"), equalTo(List.of("package")));

        // Verify that an entire thing can be removed
        assertThat((Map<String, Object>) getNestedValue(data, "things"), not(hasKey("whole_thing_removed")));
    }

    @Test
    void hiddenKeysTest() throws IOException {
        Map<String, Object> data = (Map<String, Object>) YamlPreprocessor.load(SOURCE_PATH.resolve("hiddenKeys.yaml"),
                path -> {
                });

        assertThat(getNestedValue(data, ".energy_type"), nullValue());
        assertThat(getNestedValue(data, "items", "energy_1", "type"), equalTo("number"));
        assertThat(getNestedValue(data, "items", "energy_1", "dimension"), equalTo("Energy"));
        assertThat(getNestedValue(data, "items", "energy_1", "unit"), equalTo("kWh"));
        assertThat(getNestedValue(data, "items", "energy_1", "label"), equalTo("Energy_1"));
        assertThat(getNestedValue(data, "items", "energy_2", "type"), equalTo("number"));
        assertThat(getNestedValue(data, "items", "energy_2", "dimension"), equalTo("Energy"));
        assertThat(getNestedValue(data, "items", "energy_2", "unit"), equalTo("kWh"));
        assertThat(getNestedValue(data, "items", "energy_2", "label"), equalTo("Energy_2"));
    }

    private Map<String, Object> loadPackages() throws IOException {
        return (Map<String, Object>) YamlPreprocessor.load(SOURCE_PATH.resolve("packaging.yaml"), path -> {
        });
    }

    /**
     * Retrieve a nested value from a map using the provided keys.
     * <p>
     * This method navigates through a map structure using the given keys in order.
     * If a key is not found or the current value is not a map, the method returns {@code null}.
     *
     * @param data the map to retrieve the value from; must not be {@code null}.
     * @param key the sequence of keys to navigate the map; must not be {@code null}.
     * @return the nested value if found, or {@code null} if a key is missing or the value is not a map.
     */
    private @Nullable Object getNestedValue(Map<String, Object> data, String... key) {
        Object value = data;
        for (String k : key) {
            if (value instanceof Map<?, ?> map) {
                value = map.get(k);
            } else {
                return null;
            }
        }
        return value;
    }
}
