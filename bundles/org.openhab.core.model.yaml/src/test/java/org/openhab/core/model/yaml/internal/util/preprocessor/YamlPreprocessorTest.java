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
package org.openhab.core.model.yaml.internal.util.preprocessor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
        Map<Object, Object> data = Map.of("top", Map.of("level1", Map.of("level2", "value")));
        assertThat(getNestedValue(data, "top", "level1", "level2"), equalTo("value"));
        assertNull(getNestedValue(data, "top", "nolevel1", "level2"));
        assertNull(getNestedValue(data, "top", "level1", "nolevel2"));
    }

    @Nested
    class YamlLoaderTests {
        static final String PATH = "loader/";

        @Test
        void doesNotReturnNull() throws IOException {
            Yaml yaml = createYamlParser(true);

            assertThat(yaml.load(""), equalTo(""));
            assertThat(yaml.load("# Comment"), equalTo(""));
            assertThat(yaml.load("null"), equalTo(""));
            assertThat(yaml.load("a: null"), equalTo(Map.of("a", "")));
            assertThat(yaml.load("null: null"), equalTo(Map.of("", "")));
            assertThat(yaml.load("- null"), equalTo(List.of("")));
        }

        @Test
        void booleanParser() throws IOException {
            Yaml yaml = createYamlParser(false);

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
            Map<Object, Object> data = loadFixture(PATH + "anchors.yaml");
            assertThat(data.get("baz"), equalTo("bar"));
            assertThat(data.get("bar"), equalTo("qux"));
        }

        // After the preprocessor, the variables and packages sections should be removed
        // from the resulting data structure
        @Test
        void extraElementsRemoved() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "extraElementsRemoved.yaml");

            assertThat(data, not(hasKey("variables")));
            assertThat(data, not(hasKey("packages")));
            assertThat(data, not(hasKey("preprocessors")));
        }

        @Test
        void removesKeysPrefixedWithDot() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "hiddenKeys.yaml");

            assertThat(getNestedValue(data, ".energy_type"), nullValue());
        }

        @Test
        void mergeWithInclude() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "mergeWithInclude.yaml");
            assertThat(getNestedValue(data, "simple", "foo"), equalTo("include1"));
            assertThat(getNestedValue(data, "duplicate", "foo"), equalTo("include1"));
            assertThat(getNestedValue(data, "override", "foo"), equalTo("direct"));
            assertThat(getNestedValue(data, "with_anchor_first", "foo"), equalTo("anchor"));
            assertThat(getNestedValue(data, "with_anchor_last", "foo"), equalTo("include2"));
            assertThat(getNestedValue(data, "list_merge", "foo"), equalTo("include1"));
        }
    }

    @Nested
    class VariableTests {
        static final String PATH = "variables/";

        // Ensure that our regex pattern captures these and send them to Jinjava
        // rather than being silently not recognized and returning the raw string
        @ParameterizedTest
        @ValueSource(strings = { "${{}", "${'}", "${\"}", "${'\"}", "${\"'}", "${${}}", "${${}" })
        void invalidExpressionThrows(String data) throws IOException {
            Yaml yaml = createYamlParser(true);
            assertThrows(YAMLException.class, () -> yaml.load("!sub " + data));
        }

        @Test
        void untaggedPatternsNotInterpolated() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "untaggedPatternsNotInterpolated.yaml");

            assertThat(data.get("plain"), not(equalTo("bar")));
            assertThat(data.get("double_quoted"), not(equalTo("bar")));
            assertThat(data.get("single_quoted"), not(equalTo("bar")));
            assertThat(data.get("spaced_pattern"), not(equalTo("bar")));
            assertThat(data.get("multiline_pattern"), not(equalTo("bar")));
            assertThat(data.get("folded"), not(equalTo("bar")));
        }

        @ParameterizedTest
        @ValueSource(strings = { "${undefined_variable}", "${2 + foo}", "moo ${cow}" })
        void undefinedVariables(String expression) throws IOException {
            // Create a Yaml parser with finalPass = true to trigger substitutions
            Yaml yaml = createYamlParser(true);

            YAMLException exception;

            exception = assertThrows(YAMLException.class, () -> yaml.load("test: !sub " + expression));
            assertThat(exception.getMessage(),
                    anyOf(containsString("Unknown token"), containsString("Undefined variable")));
        }

        @Test
        void customDelimiters() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "customDelimiters.yaml");

            assertThat(getNestedValue(data, "test", "data"), equalTo("barbar"));
            assertThat(getNestedValue(data, "test", "level1", "data"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "level2", "data"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "level2", "level3", "data"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "level2", "level3", "level4", "data"), equalTo("bar"));
        }

        @Test
        void variableSyntax() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "variableSyntax.yaml");

            assertThat(data.get("empty"), equalTo(""));
            assertThat(data.get("plain"), equalTo("value1"));
            assertThat(data.get("double_quoted"), equalTo("value1"));
            assertThat(data.get("single_quoted"), equalTo("value1"));

            assertThat(data.get("null_value"), equalTo(""));

            assertThat(data.get("spaces_inside_delimiters"), equalTo("value1"));
            assertThat(data.get("braces_in_double_quotes"), equalTo("${}"));
            assertThat(data.get("braces_in_single_quotes"), equalTo("${}"));

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

            assertThat(data.get("string_const"), equalTo("1"));

            Object stringConstLiteral = data.get("string_const_literal");
            if (stringConstLiteral instanceof String literal) {
                assertThat(literal.strip(), equalTo("1"));
            }

            Object stringConstFolded = data.get("string_const_folded");
            if (stringConstFolded instanceof String folded) {
                assertThat(folded.strip(), equalTo("1"));
            }

            assertThat(data.get("int_const"), equalTo(1));
            assertThat(data.get("int_const_quoted"), equalTo("1"));
            assertThat(data.get("int_var"), equalTo(1));
            assertThat(data.get("int_var_VARS"), equalTo(1));

            assertThat(data.get("map_var"), equalTo(Map.of("foo", "bar", "baz", "qux")));
            assertThat(data.get("map_lookup"), equalTo("bar"));
            assertThat(data.get("map_literal_key"), equalTo("qux"));
            assertThat(data.get("map_literal_key_dbl"), equalTo("bar"));
            assertThat(data.get("map_literal_chained"), equalTo("qux"));

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
        void predefinedVars() throws IOException {
            Path file = SOURCE_PATH.resolve(PATH + "predefinedVars.yaml").toAbsolutePath();
            Map<Object, Object> data = loadFixture(file.toString());

            assertThat(data.get("file"), equalTo(file.toString()));
            assertThat(data.get("filename"), equalTo("predefinedVars"));
            assertThat(data.get("ext"), equalTo("yaml"));
            assertThat(data.get("path"), equalTo(file.getParent().toString()));
            assertThat((String) data.get("openhab_conf"), is(not(emptyOrNullString())));
            assertThat((String) data.get("openhab_userdata"), is(not(emptyOrNullString())));
        }

        @Test
        void predefinedVarsNotOverridable() throws IOException {
            Path file = SOURCE_PATH.resolve(PATH + "predefinedVarsNotOverridable.yaml").toAbsolutePath();
            Map<Object, Object> data = loadFixture(file.toString());

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
            Map<Object, Object> data = loadFixture(PATH + "nosub.yaml");

            assertThat(getNestedValue(data, "top", "level1", "level2"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "top", "level1", "level2_sub", "level3"), equalTo("bar"));
            assertThat(getNestedValue(data, "top", "level1", "level2_sub", "level3_nosub", "level4"),
                    equalTo("${foo}"));
            assertThat(getNestedValue(data, "top", "level1", "level2_sub", "level3_nosub", "level4_sub", "level5"),
                    equalTo("bar"));
        }

        @Test
        void untaggedVariableReferencedBySub() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "untaggedVariableReferencedBySub.yaml");

            // When !sub references a variable that was defined without !sub,
            // it should get the literal value (pattern not interpolated)
            assertThat(getNestedValue(data, "data", "literal_pattern"), equalTo("${first} world"));
        }

        @Test
        void includeWithSubstitutionTags() throws IOException {
            // Tests how !include interacts with !sub and !nosub tags in both the main and included files.
            // Ensures that substitution behavior is isolated to the scope where the tag is applied:
            // - !include without !sub/!nosub leaves included content unchanged
            // - !sub/!nosub in the main file does NOT affect substitution inside the included file.
            // Specifically, plain patterns in the included file remain unaffected
            // - Verifies correct substitution for variables and patterns in various tag combinations
            Map<Object, Object> data = loadFixture(PATH + "includeWithSubstitutionTags.yaml");

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
        void anchorWithSubstitutionTags() throws IOException {
            // Tests how YAML anchors interact with !sub and !nosub tags.
            // - Ensures that substitution is performed only when !sub is present in the anchor itself,
            // even when the anchor is inserted within a !nosub context
            // - Literal patterns in the anchor are preserved even when the anchor is inserted within a !sub
            Map<Object, Object> data = loadFixture(PATH + "anchorWithSubstitutionTags.yaml");

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
        void replaceWithSubstitution() throws IOException {
            // Tests the !replace tag in combination with variable substitution.
            // Ensures that replacement operations correctly apply substitutions to the replaced content.
            // Verifies that nested substitutions within replaced values are resolved as expected.
            Map<Object, Object> data = loadFixture(PATH + "replaceWithSubstitution.yaml");

            assertThat(getNestedValue(data, "things", "MyThing", "foo"), equalTo(Map.of("qux", "cow")));
        }

        @Test
        void variablesWithSubTag() throws IOException {
            String yaml = """
                    variables: !sub
                      label: "test_${__FILE_NAME__}"
                      value: 123

                    test:
                      name: !sub ${label}
                      num: !sub ${value}
                    """;

            Path tempFile = Files.createTempFile("test_variables_sub", ".yaml");
            try {
                Files.writeString(tempFile, yaml);
                byte[] fileContent = Files.readAllBytes(tempFile);
                @SuppressWarnings("unchecked")
                Map<Object, Object> result = (Map<Object, Object>) YamlPreprocessor.process(tempFile, fileContent,
                        path -> {
                        });

                String expectedLabel = "test_" + tempFile.getFileName().toString().replace(".yaml", "");
                assertThat(getNestedValue(result, "test", "name"), is(expectedLabel));
                assertThat(getNestedValue(result, "test", "num"), is(123));
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }

        @Test
        void variableChaining() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "variableChaining.yaml");

            // Variables defined with !sub can reference other variables defined before them
            assertThat(getNestedValue(data, "data", "greeting"), equalTo("hello world"));
            assertThat(getNestedValue(data, "data", "exclamation"), equalTo("hello world!!!"));
            assertThat(getNestedValue(data, "data", "url"), equalTo("https://example.com/api/v1"));
            assertThat(getNestedValue(data, "data", "item"), equalTo("test_myitem"));
            assertThat(getNestedValue(data, "data", "plain_scalar"), equalTo(1));
            assertThat(getNestedValue(data, "data", "string_scalar"), equalTo("1"));

            assertThat(getNestedValue(data, "data", "hello_value"), equalTo("value_for_key_hello"));

            assertThat(getNestedValue(data, "data", "map_substitution"), equalTo(Map.of("first", "hello")));
            assertThat(getNestedValue(data, "data", "list_substitution"), equalTo(List.of("hello")));
        }

        @Test
        void variableChainingWithSub() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "variableChainingWithSub.yaml");

            // Variables block with !sub tag should still work with variable chaining
            assertThat(getNestedValue(data, "data", "greeting"), equalTo("hello world"));
            assertThat(getNestedValue(data, "data", "exclamation"), equalTo("hello world!!!"));
            assertThat(getNestedValue(data, "data", "nosub"), equalTo("${first}"));
        }
    }

    @Nested
    class InclusionTests {
        static final String PATH = "include/";

        @ParameterizedTest
        @ValueSource(strings = { "!include", "!include {}", "!include { file: null }" })
        void invalidIncludeThrows(String input) throws IOException {
            Yaml yaml = createYamlParser(true);
            assertThrows(YAMLException.class, () -> yaml.load(input), input);
        }

        @Test
        void include1Deep() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "include1Deep.yaml");

            assertThat(getNestedValue(data, "toplevel", "includedkey"), equalTo("value"));
        }

        @Test
        void include2Deep() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "include2Deep.yaml");

            assertThat(getNestedValue(data, "toplevel", "level1", "level2"), equalTo("foo"));
        }

        @Test
        void circularInclusion() {
            IOException exception = assertThrows(IOException.class, () -> loadFixture(PATH + "circularInclusion.yaml"));
            assertThat(exception.getMessage(), containsString("Circular inclusion detected"));
        }

        @Test
        void includedTopLevelVars() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "includedTopLevelVars.yaml");

            assertThat(getNestedValue(data, "toplevel", "level1"), equalTo("set_at_toplevel"));
        }

        @Test
        void includedTopLevelFileVars() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "includedTopLevelFileVars.yaml");

            assertThat(getNestedValue(data, "toplevel", "level1"), equalTo("set_at_include_level"));
        }

        @Test
        void includeVarsFromGlobal() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "includeVarsFromGlobal.yaml");

            // The included file resolves ${bar} where bar is set to ${foo} from globals
            assertThat(getNestedValue(data, "data", "includedkey"), equalTo("globalFoo"));
        }

        @Test
        void varsPropagate2Levels() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "varsPropagate2Levels.yaml");

            assertThat(getNestedValue(data, "toplevel", "data", "data"), equalTo("toplevel"));
        }
    }

    @Nested
    class PackagingTests {
        static final String PATH = "packages/";

        @Test
        void packageImport() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "packaging.yaml");

            assertThat(getNestedValue(data, "things", "thing1", "label"), equalTo("label1"));
            assertThat(getNestedValue(data, "things", "thing1", "scalar"), equalTo("package"));
            assertThat(getNestedValue(data, "things", "thing1", "map1", "scalar1"), equalTo("package"));
            assertThat(getNestedValue(data, "things", "thing1", "config", "map1", "scalar1"), equalTo("package"));
        }

        @Test
        void packageIdInsertion() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "packaging.yaml");

            assertThat(getNestedValue(data, "things", "pid_test", "packageid"), equalTo("package_id_test"));
            assertThat(getNestedValue(data, "things", "pid_override", "packageid"), equalTo("id_override"));
        }

        @Test
        void keepsNonPackageThings() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "packaging.yaml");

            assertThat(getNestedValue(data, "things", "thing_only_in_main", "label"), equalTo("label3"));
        }

        @Test
        @SuppressWarnings({ "null", "unchecked" })
        void packageOverwriteBehavior() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "packaging.yaml");

            Object thingOverwriteObj = getNestedValue(data, "things", "thing_overwrite");

            Map<Object, Object> thingOverwrite = (Map<Object, Object>) thingOverwriteObj;

            // Verify that scalar properties overwrite package values
            assertThat(getNestedValue(thingOverwrite, "label"), equalTo("main"));
            assertThat(getNestedValue(thingOverwrite, "scalar"), equalTo("main"));

            // Verify map overwrite - should only have main values, not package values
            assertThat(getNestedValue(thingOverwrite, "map1", "scalar1"), equalTo("main"));
            assertThat((Map<Object, Object>) getNestedValue(thingOverwrite, "map1"), not(hasKey("scalar2")));

            // Verify list overwrite - should only have main values, not package values
            assertThat(getNestedValue(thingOverwrite, "list1"), equalTo(List.of("main")));

            // Verify that top-level non-overridden properties from package are retained
            assertThat(getNestedValue(thingOverwrite, "map2", "scalar1"), equalTo("package"));
            assertThat(getNestedValue(thingOverwrite, "map2", "scalar2"), equalTo("package"));
            assertThat(getNestedValue(thingOverwrite, "list2"), equalTo(List.of("package")));

            // Verify nested config overwrite
            assertThat(getNestedValue(thingOverwrite, "config", "scalar1"), equalTo("main"));

            Map<Object, Object> configMap = (Map<Object, Object>) getNestedValue(thingOverwrite, "config");

            // Verify nested map overwrite - should only have main values, not package values
            assertThat(getNestedValue(configMap, "map1", "mainkey"), equalTo("main"));
            assertThat((Map<Object, Object>) getNestedValue(configMap, "map1"), not(hasKey("scalar1")));
            assertThat((Map<Object, Object>) getNestedValue(configMap, "map1"), not(hasKey("scalar2")));

            // Verify nested list overwrite - should only have main values, not package values
            assertThat(getNestedValue(configMap, "list1"), equalTo(List.of("main")));
        }

        @Test
        @SuppressWarnings({ "null", "unchecked" })
        void packageMergeBehavior() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "packaging.yaml");

            Map<Object, Object> thingMerge = (Map<Object, Object>) getNestedValue(data, "things", "thing_merge");

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
        @SuppressWarnings({ "null", "unchecked" })
        void packageRemoveBehavior() throws IOException {
            Map<Object, Object> data = loadFixture(PATH + "packaging.yaml");

            Map<Object, Object> thingRemove = (Map<Object, Object>) getNestedValue(data, "things", "thing_remove");

            // Verify that !remove directive removes top-level keys
            assertThat(thingRemove, not(hasKey("label")));
            assertThat(thingRemove, not(hasKey("list1")));

            Map<Object, Object> configMap = (Map<Object, Object>) getNestedValue(thingRemove, "config");

            // Verify that !remove directive removes nested keys
            assertThat(configMap, not(hasKey("scalar1")));
            assertThat(configMap, not(hasKey("map1")));
            assertThat(configMap, not(hasKey("list1")));
            assertThat((Map<Object, Object>) getNestedValue(configMap, "map2"), not(hasKey("scalar1")));

            // Verify that non-removed keys are retained
            assertThat(getNestedValue(thingRemove, "scalar"), equalTo("package"));
            assertThat(getNestedValue(thingRemove, "map1", "scalar1"), equalTo("package"));
            assertThat(getNestedValue(thingRemove, "map1", "scalar2"), equalTo("package"));
            assertThat(getNestedValue(thingRemove, "list2"), equalTo(List.of("package")));

            assertThat(getNestedValue(configMap, "scalar2"), equalTo("package"));
            assertThat(getNestedValue(configMap, "map2", "scalar2"), equalTo("package"));
            assertThat(getNestedValue(configMap, "list2"), equalTo(List.of("package")));

            // Verify that an entire thing can be removed
            assertThat((Map<Object, Object>) getNestedValue(data, "things"), not(hasKey("whole_thing_removed")));
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
    private Map<Object, Object> loadFixture(String filename) throws IOException {
        Path filePath = SOURCE_PATH.resolve(filename);
        byte[] fileContent = Files.readAllBytes(filePath);

        Object result = YamlPreprocessor.process(filePath, fileContent, path -> {
        });
        if (result instanceof Map<?, ?> dataMap) {
            return (Map<Object, Object>) dataMap;
        }
        fail("Fixture file did not produce a Map structure: " + filename);
        return Map.of(); // Unreachable
    }

    private Yaml createYamlParser(boolean finalPass) {
        YamlPreprocessor preprocessor = new YamlPreprocessor(Path.of("dummy.yaml"), Map.of(), Set.of(), (path) -> {
        });
        return YamlPreprocessor.newYaml(Map.of(), preprocessor, finalPass);
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
    private @Nullable Object getNestedValue(Map<Object, Object> data, String... key) {
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
