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
import org.junit.jupiter.api.Nested;
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
    void nestedValueLookup() {
        Map<String, Object> data = Map.of("top", Map.of("level1", Map.of("level2", "value")));
        assertThat(getNestedValue(data, "top", "level1", "level2"), equalTo("value"));
        assertNull(getNestedValue(data, "top", "nolevel1", "level2"));
        assertNull(getNestedValue(data, "top", "level1", "nolevel2"));
    }

    @Nested
    class YamlLoaderTests {
        static final String PATH = "loader/";

        @Test
        public void booleanParser() throws IOException {
            Yaml yaml = YamlPreprocessor.newYaml(new HashMap<>(), Path.of("dummy.yaml"), false);

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
        void anchors() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "anchors.yaml");
            assertThat(data.get("baz"), equalTo("bar"));
            assertThat(data.get("bar"), equalTo("qux"));
        }

        // After the preprocessor, the variables and packages sections should be removed
        // from the resulting data structure
        @Test
        void extraElementsRemoved() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "extraElementsRemoved.yaml");

            assertThat(data, not(hasKey("variables")));
            assertThat(data, not(hasKey("packages")));
        }

        @Test
        void hidesKeysPrefixedWithDot() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "hiddenKeys.yaml");

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
    }

    @Nested
    class VariableTests {
        static final String PATH = "variables/";

        @Test
        void untaggedPatternsNotInterpolated() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "untaggedPatternsNotInterpolated.yaml");

            assertThat(data.get("plain"), not(equalTo("bar")));
            assertThat(data.get("double_quoted"), not(equalTo("bar")));
            assertThat(data.get("single_quoted"), not(equalTo("bar")));
            assertThat(data.get("spaced_pattern"), not(equalTo("bar")));
            assertThat(data.get("multiline_pattern"), not(equalTo("bar")));
            assertThat(data.get("folded"), not(equalTo("bar")));
        }

        @Test
        void errorHandling() {
            // Create a Yaml parser with finalPass = true to trigger substitutions
            Yaml yaml = YamlPreprocessor.newYaml(new HashMap<>(), Path.of("dummy.yaml"), true);

            YAMLException exception;

            exception = assertThrows(YAMLException.class, () -> yaml.load("test: !sub ${undefined_variable}"));
            assertThat(exception.getMessage(), containsString("undefined_variable"));

            // Jinjava will silently pass this expression as "2" without error, but
            // we specifically want to error on undefined variables
            exception = assertThrows(YAMLException.class, () -> yaml.load("test: !sub ${2 + foo}"));
            assertThat(exception.getMessage(), containsString("foo"));
        }

        @Test
        void customPatterns() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "customPatterns.yaml");

            assertThat(getNestedValue(data, "test", "data"), equalTo("barbar"));
            assertThat(getNestedValue(data, "test", "level1", "data"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "level2", "data"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "level2", "level3", "data"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "level2", "level3", "level4", "data"), equalTo("bar"));
        }

        @Test
        void variableSyntax() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "variableSyntax.yaml");

            assertThat(data.get("plain"), equalTo("value1"));
            assertThat(data.get("double_quoted"), equalTo("value1"));
            assertThat(data.get("single_quoted"), equalTo("value1"));

            assertThat(data.get("spaces_inside_delimiters"), equalTo("value1"));

            assertThat(data.get("vars_bracket_double"), equalTo("reserved"));
            assertThat(data.get("vars_bracket_single"), equalTo("reserved"));
            assertThat(data.get("vars_with_dash"), equalTo("dashvalue"));
            assertThat(data.get("vars_with_space"), equalTo("spacevalue"));

            assertThat(data.get("empty_no_default"), equalTo(""));

            assertThat(data.get("exists"), equalTo("value1"));
            assertThat(data.get("empty_value_test"), equalTo(""));
            assertThat(data.get("empty_value_test_compound"), equalTo("prefix-"));

            assertThat(data.get("exists_with_true"), equalTo("value1"));
            assertThat(data.get("empty_value_with_true"), equalTo("default"));

            assertThat(data.get("chained"), equalTo("VALUE1"));

            assertThat(data.get("compound_value"), equalTo("Value is value1"));
            assertThat(data.get("compound_quoted"), equalTo("Value is value1"));

            assertThat(data.get("multiple_patterns"), equalTo("value1-1"));

            assertThat(data.get("string_const"), instanceOf(String.class));
            assertThat(data.get("string_const"), equalTo("1"));

            Object stringConstLiteral = data.get("string_const_literal");
            assertThat(stringConstLiteral, instanceOf(String.class));
            // although this seems redundant, it avoids explicit cast warning
            if (stringConstLiteral instanceof String literal) {
                assertThat(literal.strip(), equalTo("1"));
            }

            Object stringConstFolded = data.get("string_const_folded");
            assertThat(stringConstFolded, instanceOf(String.class));
            if (stringConstFolded instanceof String folded) {
                assertThat(folded.strip(), equalTo("1"));
            }

            assertThat(data.get("int_const"), equalTo(1));
            assertThat(data.get("int_const_quoted"), equalTo("1"));
            assertThat(data.get("int_var"), equalTo(1));
            assertThat(data.get("int_var_VARS"), equalTo(1));

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
        void withinMergedAliases() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "aliasSubstitutions.yaml");

            assertThat(getNestedValue(data, "final_plain_plain", "foo"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "final_plain_sub", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "final_plain_nosub", "foo"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "final_sub_plain", "foo"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "final_sub_sub", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "final_sub_nosub", "foo"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "final_nosub_plain", "foo"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "final_nosub_sub", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "final_nosub_nosub", "foo"), equalTo("${foo}"));
        }

        @Test
        void predefinedVars() throws IOException {
            Path file = SOURCE_PATH.resolve(PATH + "predefinedVars.yaml").toAbsolutePath();
            Map<String, Object> data = loadFixture(file.toString());

            assertThat(data.get("file"), equalTo(file.toString()));
            assertThat(data.get("filename"), equalTo("predefinedVars"));
            assertThat(data.get("ext"), equalTo("yaml"));
            assertThat(data.get("path"), equalTo(file.getParent().toString()));
        }

        @Test
        void predefinedVarsNotOverridable() throws IOException {
            Path file = SOURCE_PATH.resolve(PATH + "predefinedVarsNotOverridable.yaml").toAbsolutePath();
            Map<String, Object> data = loadFixture(file.toString());

            // Verify that predefined vars cannot be overridden by the `variables` section
            assertThat(data.get("file"), equalTo(file.toString()));
            assertThat(data.get("filename"), equalTo("predefinedVarsNotOverridable"));
            assertThat(data.get("ext"), equalTo("yaml"));
            assertThat(data.get("path"), equalTo(file.getParent().toString()));

            // Verify that predefined vars cannot be overridden by !include vars
            file = file.resolveSibling("predefinedVarsNotOverridable.inc.yaml");
            assertThat(getNestedValue(data, "include", "file"), equalTo(file.toString()));
            assertThat(getNestedValue(data, "include", "filename"), equalTo("predefinedVarsNotOverridable.inc"));
            assertThat(getNestedValue(data, "include", "ext"), equalTo("yaml"));
            assertThat(getNestedValue(data, "include", "path"), equalTo(file.getParent().toString()));
        }

        @Test
        void nosub() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "nosub.yaml");

            assertThat(getNestedValue(data, "top", "level1", "level2"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "top", "level1", "level2_sub", "level3"), equalTo("bar"));
            assertThat(getNestedValue(data, "top", "level1", "level2_sub", "level3_nosub", "level4"),
                    equalTo("${foo}"));
            assertThat(getNestedValue(data, "top", "level1", "level2_sub", "level3_nosub", "level4_sub", "level5"),
                    equalTo("bar"));
        }

        @Test
        void subOnInclude() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "subOnInclude.yaml");

            // !include without !sub / !nosub should behave as plain inclusion
            // leaving the behavior inside the included file unaffected
            assertThat(getNestedValue(data, "plain", "data", "plain"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "plain", "data", "sub"), equalTo("bar"));
            assertThat(getNestedValue(data, "plain", "data", "nosub"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "plain", "data", "moo_plain"), equalTo("${moo}"));
            assertThat(getNestedValue(data, "plain", "data", "moo_sub"), equalTo("${mainmoo}"));

            // !sub in the main file should NOT alter the behavior inside the included file
            assertThat(getNestedValue(data, "sub", "data", "plain"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "sub", "data", "sub"), equalTo("bar"));
            assertThat(getNestedValue(data, "sub", "data", "nosub"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "sub", "data", "moo_plain"), equalTo("${moo}"));
            assertThat(getNestedValue(data, "sub", "data", "moo_sub"), equalTo("cow"));

            // !nosub in the main file should NOT alter the behavior inside the included file
            assertThat(getNestedValue(data, "nosub", "data", "plain"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "nosub", "data", "sub"), equalTo("bar"));
            assertThat(getNestedValue(data, "nosub", "data", "nosub"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "nosub", "data", "moo_plain"), equalTo("${moo}"));
            assertThat(getNestedValue(data, "nosub", "data", "moo_sub"), equalTo("${mainmoo}"));
        }

        @Test
        void subOnAnchor() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "subOnAnchor.yaml");

            assertThat(getNestedValue(data, "sub_anchor", "obj", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "nosub_anchor", "obj", "foo"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "plain_plain", "foo"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "plain_sub", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "plain_nosub", "foo"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "sub_plain", "foo"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "sub_sub", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "sub_nosub", "foo"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "nosub_plain", "foo"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "nosub_sub", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "nosub_nosub", "foo"), equalTo("${foo}"));
        }

        @Test
        void subOnReplace() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "subOnReplace.yaml");

            assertThat(getNestedValue(data, "things", "MyThing", "foo"), equalTo(Map.of("qux", "cow")));
        }
    }

    @Nested
    class InclusionTests {
        static final String PATH = "include/";

        @Test
        void include1Deep() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "include1Deep.yaml");

            assertThat(getNestedValue(data, "toplevel", "includedkey"), equalTo("value"));
        }

        @Test
        void include2Deep() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "include2Deep.yaml");

            assertThat(getNestedValue(data, "toplevel", "level1", "level2"), equalTo("foo"));
        }

        @Test
        void circularInclusion() {
            IOException exception = assertThrows(IOException.class, () -> loadFixture(PATH + "circularInclusion.yaml"));
            assertThat(exception.getMessage(), containsString("Circular inclusion detected"));
        }

        @Test
        void includedTopLevelVars() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "includedTopLevelVars.yaml");

            assertThat(getNestedValue(data, "toplevel", "level1"), equalTo("set_at_toplevel"));
        }

        @Test
        void includedTopLevelFileVars() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "includedTopLevelFileVars.yaml");

            assertThat(getNestedValue(data, "toplevel", "level1"), equalTo("set_at_include_level"));
        }

        @Test
        void includeVarsFromGlobal() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "includeVarsFromGlobal.yaml");

            // The included file resolves ${bar} where bar is set to ${foo} from globals
            assertThat(getNestedValue(data, "data", "includedkey"), equalTo("globalFoo"));
        }

        @Test
        void varsPropagate2Levels() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "varsPropagate2Levels.yaml");

            assertThat(getNestedValue(data, "toplevel", "data", "data"), equalTo("toplevel"));
        }
    }

    @Nested
    class PackagingTests {
        static final String PATH = "packages/";

        @Test
        void packageImport() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "packaging.yaml");

            assertThat(getNestedValue(data, "things", "thing1", "label"), equalTo("label1"));
            assertThat(getNestedValue(data, "things", "thing1", "scalar"), equalTo("package"));
            assertThat(getNestedValue(data, "things", "thing1", "map1", "scalar1"), equalTo("package"));
            assertThat(getNestedValue(data, "things", "thing1", "config", "map1", "scalar1"), equalTo("package"));
        }

        @Test
        void packageIdInsertion() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "packaging.yaml");

            assertThat(getNestedValue(data, "things", "pid_test", "packageid"), equalTo("package_id_test"));
            assertThat(getNestedValue(data, "things", "pid_override", "packageid"), equalTo("id_override"));
        }

        @Test
        void keepsNonPackageThings() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "packaging.yaml");

            assertThat(getNestedValue(data, "things", "thing_only_in_main", "label"), equalTo("label3"));
        }

        @Test
        @SuppressWarnings("null")
        void packageOverwriteBehavior() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "packaging.yaml");

            Object thingOverwriteObj = getNestedValue(data, "things", "thing_overwrite");

            Map<String, Object> thingOverwrite = (Map<String, Object>) thingOverwriteObj;

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
        @SuppressWarnings("null")
        void packageMergeBehavior() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "packaging.yaml");

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
        @SuppressWarnings("null")
        void packageRemoveBehavior() throws IOException {
            Map<String, Object> data = loadFixture(PATH + "packaging.yaml");

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
    }

    /**
     * Load a YAML fixture file from the test resources.
     * <p>
     * This helper method simplifies loading fixture files by automatically resolving the path
     * relative to the standard test resources directory and parsing the YAML content.
     *
     * @param filename the name of the YAML file to load (relative to the fixture directory)
     * @return the parsed YAML content as a Map
     * @throws IOException if an error occurs reading the file
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadFixture(String filename) throws IOException {
        return (Map<String, Object>) YamlPreprocessor.load(SOURCE_PATH.resolve(filename), path -> {
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
