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
package org.openhab.core.automation.internal.parser.jackson;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.template.RuleTemplate;
import org.openhab.core.automation.template.Template;

/**
 * Tests the YAML export of {@link TemplateYAMLParser}.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public class TemplateYAMLParserTest {

    @Test
    public void serializeWritesUidFirstAndLowerCase() throws Exception {
        Template template = new RuleTemplate("basic:yaml-rule-template", "Label", "Description", Set.of("tag"),
                List.of(), List.of(), List.of(), List.of(), Visibility.VISIBLE);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            new TemplateYAMLParser().serialize(Set.of(template), writer);
        }
        String yaml = out.toString(StandardCharsets.UTF_8);

        assertThat(yaml, startsWith("---\nuid: \"basic:yaml-rule-template\"\n"));
        assertThat(yaml, not(containsString("UID")));
        assertThat(yaml, containsString("label: \"Label\"\n"));
    }
}
