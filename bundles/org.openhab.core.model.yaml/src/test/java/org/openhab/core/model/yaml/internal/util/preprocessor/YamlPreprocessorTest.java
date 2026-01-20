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
import java.nio.file.Path;
import java.util.HashMap;
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

    // getNestedValue is a helper method used throughout the tests
    @Test
    void getNestedValue_extractsValueFromDeeplyNestedMap() {
        Map<Object, Object> data = Map.of("top", Map.of("level1", Map.of("level2", "value")));
        assertThat(getNestedValue(data, "top", "level1", "level2"), equalTo("value"));
        assertNull(getNestedValue(data, "top", "nolevel1", "level2"));
        assertNull(getNestedValue(data, "top", "level1", "nolevel2"));
    }

    @Nested
    class YamlLoaderTests {
        static final Path PATH = Path.of("loader");

        @Test
        void nullResolvesToEmptyString() throws IOException {
            Yaml yaml = createYamlParser(true);

            assertThat(yaml.load(""), equalTo(""));
            assertThat(yaml.load("# Comment"), equalTo(""));
            assertThat(yaml.load("null"), equalTo(""));
            assertThat(yaml.load("a: null"), equalTo(Map.of("a", "")));
            assertThat(yaml.load("null: null"), equalTo(Map.of("", "")));
            assertThat(yaml.load("- null"), equalTo(List.of("")));
        }

        @Test
        void onlyTrueFalseParsedAsBoolean() throws IOException {
            Yaml yaml = createYamlParser(false);

            assertThat(yaml.load("true"), equalTo(true));
            assertThat(yaml.load("True"), equalTo(true));
            assertThat(yaml.load("TRUE"), equalTo(true));
            assertThat(yaml.load("tRuE"), equalTo(true));

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
        void anchorsSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("anchors.yaml"));
            assertThat(data.get("baz"), equalTo("bar"));
            assertThat(data.get("bar"), equalTo("qux"));
        }

        // After the preprocessor, the variables and packages sections should be removed
        // from the resulting data structure
        @Test
        void extraElementsRemoved() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("extraElements.yaml"));
            assertThat(data, not(hasKey("variables")));
            assertThat(data, not(hasKey("templates")));
            assertThat(data, not(hasKey("packages")));
            assertThat(data, not(hasKey("preprocessors")));
        }

        @Test
        void hiddenKeysRemoved() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("hiddenKeys.yaml"));
            assertThat(getNestedValue(data, ".energy_type"), is(nullValue()));
        }
    }

    @Nested
    class VariableTests {
        static final Path PATH = Path.of("variables");

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
            Map<Object, Object> data = loadFixture(PATH.resolve("untaggedPatterns.yaml"));

            assertThat(data.get("plain"), not(equalTo("bar")));
            assertThat(data.get("double_quoted"), not(equalTo("bar")));
            assertThat(data.get("single_quoted"), not(equalTo("bar")));
            assertThat(data.get("spaced_pattern"), not(equalTo("bar")));
            assertThat(data.get("multiline_pattern"), not(equalTo("bar")));
            assertThat(data.get("folded"), not(equalTo("bar")));
        }

        @ParameterizedTest
        @ValueSource(strings = { "${undefined_variable}", "${2 + foo}", "moo ${cow}" })
        void undefinedVariableThrows(String expression) throws IOException {
            // Create a Yaml parser with finalPass = true to trigger substitutions
            Yaml yaml = createYamlParser(true);

            YAMLException exception;

            exception = assertThrows(YAMLException.class, () -> yaml.load("test: !sub " + expression));
            assertThat(exception.getMessage(),
                    anyOf(containsString("Unknown token"), containsString("Undefined variable")));
        }

        @Test
        void customVariableDelimitersSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("customVariableDelimiters.yaml"));

            assertThat(getNestedValue(data, "test", "data"), equalTo("barbar"));
            assertThat(getNestedValue(data, "test", "level1", "data"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "data2"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "level2", "data"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "level2", "data2"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "level2", "level3", "data"), equalTo("bar"));
            assertThat(getNestedValue(data, "test", "level1", "level2", "level3", "level4", "data"), equalTo("bar"));
        }

        @Test
        void variableGrammarValidations() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("variableSyntaxSpec.yaml"));

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
        void labelFilterSupported() throws IOException {
            Yaml yaml = createYamlParser(true);

            Map.of( //
                    "foo bar", "Foo Bar", //
                    "one two Three", "One Two Three", //
                    "multi   space", "Multi Space", //
                    "fooBar", "Foo Bar", //
                    "FooBar", "Foo Bar", //
                    "FOOBAR", "FOOBAR", //
                    "openHAB", "OpenHAB", //
                    "foo-bar_baz", "Foo Bar Baz", //
                    "multiple---separators___here", "Multiple Separators Here" //
            ).forEach((input, expected) -> {
                try {
                    assertThat(yaml.load("!sub ${'" + input + "' | label}"), equalTo(expected));
                } catch (YAMLException e) {
                    fail("YAMLException thrown during test execution: " + e.getMessage());
                }
            });
        }

        @Test
        void predefinedVarsResolved() throws IOException {
            Path file = SOURCE_PATH.resolve(PATH).resolve("predefinedVars.yaml").toAbsolutePath();
            Map<Object, Object> data = loadFixture(file);

            assertThat(data.get("file"), equalTo(file.toString()));
            assertThat(data.get("filename"), equalTo("predefinedVars"));
            assertThat(data.get("ext"), equalTo("yaml"));
            assertThat(data.get("path"), equalTo(file.getParent().toString()));
            assertThat((String) data.get("openhab_conf"), is(not(emptyOrNullString())));
            assertThat((String) data.get("openhab_userdata"), is(not(emptyOrNullString())));
        }

        @Test
        void predefinedVarsNotOverridable() throws IOException {
            Path file = SOURCE_PATH.resolve(PATH).resolve("predefinedVarsNotOverridable.yaml").toAbsolutePath();
            Map<Object, Object> data = loadFixture(file);

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
        void nosubDisablesSubstitutions() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("nosub.yaml"));

            assertThat(getNestedValue(data, "top", "level1_a"), equalTo("bar"));
            assertThat(getNestedValue(data, "top", "level1", "level2"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "top", "level1", "level2_a", "level3"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "top", "level1", "level2_sub", "level3"), equalTo("bar"));
            assertThat(getNestedValue(data, "top", "level1", "level2_sub", "level3_nosub", "level4"),
                    equalTo("${foo}"));
            assertThat(getNestedValue(data, "top", "level1", "level2_sub", "level3_nosub", "level4_sub", "level5"),
                    equalTo("bar"));
        }

        @Test
        void variableLoadsFromInclude() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("variableFromInclude.yaml"));

            assertThat(getNestedValue(data, "static_value"), equalTo("qux"));
            assertThat(getNestedValue(data, "scalar_subst_include"), equalTo("qux"));
            assertThat(getNestedValue(data, "map_subst_include"), equalTo("qux"));
        }

        @Test
        void literalPatternsInVariableValueNotInterpolated() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("literalPatternsInVariableValue.yaml"));
            // When !sub references a variable that was defined without !sub,
            // it should get the literal value (pattern not interpolated)
            assertThat(getNestedValue(data, "data", "literal_pattern"), equalTo("${first} world"));
        }

        /**
         * Verify that included content remains literal
         * when the !include tag is nested inside a !sub block.
         */
        @Test
        void subDoesNotApplyToIncludedLiteralValues() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("subOverIncludeLiterals.yaml"));

            // !include without !sub / !nosub should behave as plain inclusion
            // leaving the behavior inside the included file unaffected
            assertThat(getNestedValue(data, "plain", "data", "plain"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "plain", "data", "sub"), equalTo("bar"));
            assertThat(getNestedValue(data, "plain", "data", "nosub"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "plain", "data", "moo_plain"), equalTo("${moo}"));
            assertThat(getNestedValue(data, "plain", "data", "moo_sub"), equalTo("${mainmoo}"));

            // !sub in the main file should NOT interpolate literal patterns inside the included file
            assertThat(getNestedValue(data, "sub", "data", "plain"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "sub", "data", "sub"), equalTo("bar"));
            assertThat(getNestedValue(data, "sub", "data", "nosub"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "sub", "data", "moo_plain"), equalTo("${moo}"));
            assertThat(getNestedValue(data, "sub", "data", "moo_sub"), equalTo("cow"));

            // !sub in the main file should NOT interpolate literal patterns inside the included file when using merge
            assertThat(getNestedValue(data, "submerge", "data", "plain"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "submerge", "data", "sub"), equalTo("bar"));
            assertThat(getNestedValue(data, "submerge", "data", "nosub"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "submerge", "data", "moo_plain"), equalTo("${moo}"));
            assertThat(getNestedValue(data, "submerge", "data", "moo_sub"), equalTo("cow"));

            // !nosub in the main file should NOT affect interpolation inside the included file
            assertThat(getNestedValue(data, "nosub", "data", "plain"), equalTo("${foo}"));
            assertThat(getNestedValue(data, "nosub", "data", "sub"), equalTo("bar"));
            assertThat(getNestedValue(data, "nosub", "data", "nosub"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "nosub", "data", "moo_plain"), equalTo("${moo}"));
            assertThat(getNestedValue(data, "nosub", "data", "moo_sub"), equalTo("${mainmoo}"));
        }

        @Test
        void subDoesNotApplyToAnchorLiteralValues() throws IOException {
            // Tests how YAML anchors interact with !sub and !nosub tags.
            // - Ensures that substitution is performed only when !sub is present in the anchor itself,
            // even when the anchor is inserted within a !nosub context
            // - Literal patterns in the anchor are preserved even when the anchor is inserted within a !sub
            Map<Object, Object> data = loadFixture(PATH.resolve("subOverAnchorLiterals.yaml"));

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
        void subDoesNotApplyToTemplateLiteralValues() throws IOException {
            // Tests how YAML templates interact with !sub and !nosub tags.
            // - Ensures that substitution is performed only when !sub is present in the template itself,
            // even when the template is inserted within a !nosub context
            // - Literal patterns in the template are preserved even when the template is inserted within a !sub
            Map<Object, Object> data = loadFixture(PATH.resolve("subOverTemplateLiterals.yaml"));

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
        void substitutionWorksInsideReplace() throws IOException {
            // Tests the !replace tag in combination with variable substitution.
            // Ensures that replacement operations correctly apply substitutions to the replaced content.
            // Verifies that nested substitutions within replaced values are resolved as expected.
            Map<Object, Object> data = loadFixture(PATH.resolve("substitutionInsideReplace.yaml"));

            assertThat(getNestedValue(data, "things", "MyThing", "foo"), equalTo(Map.of("qux", "cow")));
        }

        @Test
        void subOnVariablesNodeSupported() throws IOException {
            Map<Object, Object> result = loadFixture(PATH.resolve("subOnVariablesNode.yaml"));

            assertThat(getNestedValue(result, "test", "name"), is("the label"));
            assertThat(getNestedValue(result, "test", "num"), is(123));
        }

        @Test
        void variableChainingSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("variableChaining.yaml"));

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
        void variableChainingWithSubSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("variableChainingWithSub.yaml"));

            // Variables block with !sub tag should still work with variable chaining
            assertThat(getNestedValue(data, "data", "greeting"), equalTo("hello world"));
            assertThat(getNestedValue(data, "data", "exclamation"), equalTo("hello world!!!"));
            assertThat(getNestedValue(data, "data", "nosub"), equalTo("${first}"));
        }

        @Test
        void subInScalarFormIncludeArgSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("subInScalarFormIncludeArg.yaml"));

            assertThat(getNestedValue(data, "foo", "bar"), equalTo("bar"));
        }
    }

    @Nested
    class InclusionTests {
        static final Path PATH = Path.of("include");

        @ParameterizedTest
        @ValueSource(strings = { "!include", "!include ''", "!include {}", "!include { file: null }" })
        void invalidIncludeThrows(String input) throws IOException {
            Yaml yaml = createYamlParser(true);
            assertThrows(YAMLException.class, () -> yaml.load(input), input);
        }

        @Test
        void includeWithScalarArgumentSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("includeWithScalarArgument.yaml"));

            assertThat(getNestedValue(data, "toplevel", "includedkey"), equalTo("value"));
        }

        @Test
        void includeWithMapArgumentSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("includeWithMapArgument.yaml"));

            assertThat(getNestedValue(data, "toplevel", "includedkey"), equalTo("value"));
        }

        @Test
        void includeNonexistentFileThrows() {
            assertThrows(IOException.class, () -> loadFixture(PATH.resolve("includeNonexistentFile.yaml")));
        }

        @Test
        void nestedIncludeSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("nestedInclude.yaml"));

            assertThat(getNestedValue(data, "toplevel", "level1", "level2"), equalTo("foo"));
        }

        @Test
        void circularInclusionThrows() {
            IOException exception = assertThrows(IOException.class,
                    () -> loadFixture(PATH.resolve("circularInclusion.yaml")));
            assertThat(exception.getMessage(), containsString("Circular inclusion detected"));
        }

        @Test
        void parentVariablesAccessibleInInclude() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("parentVariable.yaml"));

            assertThat(getNestedValue(data, "toplevel", "level1"), equalTo("set_at_toplevel"));
        }

        @Test
        void includeVarsOverrideMainVars() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("includeWithVarsArgument.yaml"));

            assertThat(getNestedValue(data, "toplevel", "level1"), equalTo("set_at_include_level"));
        }

        @Test
        void includeArgumentsResolveFromGlobalVars() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("includeArgumentsResolveFromGlobalVars.yaml"));

            // The included file resolves ${bar} where bar is set to ${foo} from globals
            assertThat(getNestedValue(data, "data", "includedkey"), equalTo("globalFoo"));
        }

        @Test
        void globalVarsVisibleInDeeplyNestedIncludes() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("deepIncludeVar.yaml"));

            assertThat(getNestedValue(data, "toplevel", "data", "data"), equalTo("toplevel"));
        }
    }

    @Nested
    class TemplateTests {
        static final Path PATH = Path.of("templates");

        @Test
        void insertWithScalarArg() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("simpleTemplate.yaml"));

            assertThat(getNestedValue(data, "plain_scalar"), equalTo("bar"));
            assertThat(getNestedValue(data, "plain_map", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "plain_list"), equalTo(List.of("foo", "baz")));
        }

        @Test
        void insertVarsOverrideMainVars() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("insertWithVarsArgument.yaml"));

            assertThat(getNestedValue(data, "foo"), equalTo("overridden"));
        }

        @Test
        void insertNonexistentTemplateThrows() {
            assertThrows(IOException.class, () -> loadFixture(PATH.resolve("insertNonexistentTemplate.yaml")));
        }

        @Test
        void subOnTemplatesNodeSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("subOnTemplatesValueNode.yaml"));

            assertThat(getNestedValue(data, "data"), is("foo"));
        }
    }

    @Nested
    class PackagingTests {
        static final Path PATH = Path.of("packages");

        @Test
        void packageWithIncludeSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("basicPackageWithInclude.yaml"));

            assertThat(getNestedValue(data, "things", "basic1", "label"), equalTo("Basic1 Thing"));
            assertThat(getNestedValue(data, "items", "basic1", "type"), equalTo("Switch"));
            assertThat(getNestedValue(data, "items", "basic1", "label"), equalTo("Basic1 Item"));

            assertThat(getNestedValue(data, "things", "basic2", "label"), equalTo("Basic2 Thing"));
            assertThat(getNestedValue(data, "items", "basic2", "type"), equalTo("Switch"));
            assertThat(getNestedValue(data, "items", "basic2", "label"), equalTo("Basic2 Item"));
        }

        @Test
        void packageWithTemplateSupported() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("basicPackageWithTemplate.yaml"));

            assertThat(getNestedValue(data, "things", "basic1", "label"), equalTo("Basic1 Thing"));
            assertThat(getNestedValue(data, "items", "basic1", "type"), equalTo("Switch"));
            assertThat(getNestedValue(data, "items", "basic1", "label"), equalTo("Basic1 Item"));

            assertThat(getNestedValue(data, "things", "basic2", "label"), equalTo("Basic2 Thing"));
            assertThat(getNestedValue(data, "items", "basic2", "type"), equalTo("Switch"));
            assertThat(getNestedValue(data, "items", "basic2", "label"), equalTo("Basic2 Item"));
        }

        @Test
        void nonPackageContentRemainsAfterMerge() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("nonPackageContent.yaml"));

            assertThat(getNestedValue(data, "things", "thing_only_in_main", "label"), equalTo("label3"));
        }

        @Test
        void packageIdInjectedIntoIncludeContext() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("packageIdInjectionIntoInclude.yaml"));

            assertThat(getNestedValue(data, "things", "id_test"), equalTo("id_test Value"));
        }

        @Test
        void packageIdInjectedIntoTemplateContext() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("packageIdInjectionIntoTemplate.yaml"));

            assertThat(getNestedValue(data, "things", "id_test"), equalTo("id_test Value"));
        }

        @Test
        void packageIdIsOverridable() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("packageIdOverride.yaml"));

            assertThat(getNestedValue(data, "things", "custom_id"), equalTo("custom_id Value"));
        }

        @Test
        @SuppressWarnings({ "null", "unchecked" })
        void packageMergeDefaultBehavior() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("packageMergeDefault.yaml"));

            Map<Object, Object> thingMerge = (Map<Object, Object>) getNestedValue(data, "things", "thing");

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
        }

        @Test
        @SuppressWarnings({ "null", "unchecked" })
        void packageMergeRemoveBehavior() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("packageMergeRemove.yaml"));

            Map<Object, Object> thing = (Map<Object, Object>) getNestedValue(data, "things", "thing");

            // Verify that !remove directive removes top-level keys
            assertThat(thing, not(hasKey("label")));
            assertThat(thing, not(hasKey("list1")));

            Map<Object, Object> config = (Map<Object, Object>) getNestedValue(thing, "config");

            // Verify that !remove directive removes nested keys
            assertThat(config, not(hasKey("scalar1")));
            assertThat(config, not(hasKey("map1")));
            assertThat(config, not(hasKey("list1")));

            assertThat((Map<Object, Object>) getNestedValue(config, "map2"), not(hasKey("scalar1")));

            // Verify that non-removed keys are retained
            assertThat(getNestedValue(thing, "scalar"), equalTo("package"));
            assertThat(getNestedValue(thing, "map1", "scalar1"), equalTo("package"));
            assertThat(getNestedValue(thing, "map1", "scalar2"), equalTo("package"));
            assertThat(getNestedValue(thing, "list2"), equalTo(List.of("package")));

            assertThat(getNestedValue(config, "scalar2"), equalTo("package"));
            assertThat(getNestedValue(config, "map2", "scalar2"), equalTo("package"));
            assertThat(getNestedValue(config, "list2"), equalTo(List.of("package")));

            // Verify that an entire thing can be removed
            assertThat((Map<Object, Object>) getNestedValue(data, "things"), hasKey("thing_retained"));
            assertThat((Map<Object, Object>) getNestedValue(data, "things"), not(hasKey("whole_thing_removed")));
        }

        @Test
        @SuppressWarnings({ "null", "unchecked" })
        void packageMergeReplaceBehavior() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("packageMergeReplace.yaml"));

            Object thingObj = getNestedValue(data, "things", "thing");

            Map<Object, Object> thing = (Map<Object, Object>) thingObj;

            // Verify !replace should only have main map/list, not merged with package values
            assertThat(getNestedValue(thing, "map1"), equalTo(Map.of("scalar1", "main")));
            assertThat(getNestedValue(thing, "list1"), equalTo(List.of("main")));

            // Verify that other properties from package are retained
            assertThat(getNestedValue(thing, "map2"), equalTo(Map.of("scalar1", "package", "scalar2", "package")));
            assertThat(getNestedValue(thing, "list2"), equalTo(List.of("package")));

            Map<Object, Object> config = (Map<Object, Object>) getNestedValue(thing, "config");

            // Verify !replace should only have main map/list, not merged with package values
            assertThat(getNestedValue(config, "map1"), equalTo(Map.of("mainkey", "main")));
            assertThat(getNestedValue(config, "list1"), equalTo(List.of("main")));

            // Verify that other properties from package are retained
            assertThat(getNestedValue(config, "scalar2"), equalTo("package"));
            assertThat(getNestedValue(config, "map2"), equalTo(Map.of("scalar1", "package", "scalar2", "package")));
            assertThat(getNestedValue(config, "list2"), equalTo(List.of("package")));
        }
    }

    @Nested
    class MergeKeyTests {
        static final Path PATH = Path.of("mergekeys");

        @Test
        void mergeKeysWorkWithIncludes() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("mergeWithInclude.yaml"));
            assertThat(getNestedValue(data, "simple", "foo"), equalTo("include1"));
            assertThat(getNestedValue(data, "duplicate", "foo"), equalTo("include1"));
            assertThat(getNestedValue(data, "override", "foo"), equalTo("direct"));
            assertThat(getNestedValue(data, "with_anchor_first", "foo"), equalTo("anchor"));
            assertThat(getNestedValue(data, "with_anchor_last", "foo"), equalTo("include2"));
            assertThat(getNestedValue(data, "list_merge", "foo"), equalTo("include1"));
            assertThat(getNestedValue(data, "merge_with_scalar", "foo"), equalTo("include1"));
            assertThat(getNestedValue(data, "merge_with_variable", "foo"), equalTo("include1"));
        }

        @Test
        void mergeKeysWorkWithSubstitutions() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("mergeWithSubstitution.yaml"));
            assertThat(getNestedValue(data, "simple", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "simple", "baz"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "parent_sub", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "parent_sub", "baz"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "parent_nosub", "foo"), equalTo("bar"));
            assertThat(getNestedValue(data, "parent_nosub", "baz"), equalTo("${foo}"));

            assertThat(getNestedValue(data, "conditionally_empty"), equalTo(Map.of()));

            assertThat(getNestedValue(data, "array_merge"),
                    equalTo(Map.of("foo", "bar", "baz", "${foo}", "qux", "quux")));
        }

        @Test
        void mergeWithSubstitutionMustResolveToMapping() throws IOException {
            Yaml yaml = createYamlParser(true);

            YAMLException exception = assertThrows(YAMLException.class, () -> yaml.load("simple:\n  <<: !sub scalar"));
            assertThat(exception.getMessage(), containsString("Substituted content must be a mapping for merge key"));
        }

        @Test
        void mergeKeysWorkWithTemplates() throws IOException {
            Map<Object, Object> data = loadFixture(PATH.resolve("mergeWithTemplates.yaml"));

            Map<Object, Object> map1 = Map.of("foo", "bar", "baz", "${foo}", "qux", "bar");
            Map<Object, Object> map2 = Map.of("corge", "grault");

            Map<Object, Object> combined = new HashMap<>(map1);
            combined.putAll(map2);

            assertThat(getNestedValue(data, "simple"), equalTo(map1));
            assertThat(getNestedValue(data, "parent_sub"), equalTo(map1));
            assertThat(getNestedValue(data, "parent_nosub"), equalTo(map1));

            assertThat(getNestedValue(data, "array_merge"), equalTo(combined));
        }
    }

    /**
     * Load a YAML fixture file from the test resources.
     * <p>
     * This helper method simplifies loading fixture files by automatically resolving the path
     * relative to the standard test resources directory and parsing the YAML content.
     *
     * @param source the name of the YAML file to load (relative to the fixture directory)
     * @return the parsed YAML content as a Map
     * @throws IOException if an error occurs reading the file
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Object> loadFixture(Path source) throws IOException {
        Path filePath = SOURCE_PATH.resolve(source);
        Object result = YamlPreprocessor.load(filePath, path -> {
        });
        if (result instanceof Map<?, ?> dataMap) {
            return (Map<Object, Object>) dataMap;
        }
        fail("Fixture file did not produce a Map structure: " + source);
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
