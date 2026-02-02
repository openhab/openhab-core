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
package org.openhab.core.model.yaml.internal.rules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.util.TriggerBuilder;
import org.openhab.core.io.dto.SerializationException;

/**
 * The {@link AbstractYamlRuleProviderTest} contains tests for the {@link AbstractYamlRuleProvider} class.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
public class AbstractYamlRuleProviderTest {

    @Test
    public void testExtractModuleIds() {
        AbstractYamlRuleProvider<Rule> provider = new AbstractYamlRuleProvider<Rule>() {

            @Override
            public Collection<Rule> getAll() {
                return Set.of();
            }
        };

        Set<String> ids = provider.extractModuleIds();
        assertThat(ids, is(empty()));

        ids = provider.extractModuleIds(List.of(TriggerBuilder.create().withTypeUID("testUID").withId("test").build(),
                new Object(), TriggerBuilder.create().withTypeUID("testUID2").withId("   ").build()));
        assertThat(ids, is(hasSize(1)));
    }

    @SuppressWarnings("null")
    @Test
    public void testMapModules() throws SerializationException {
        AbstractYamlRuleProvider<Rule> provider = new AbstractYamlRuleProvider<Rule>() {

            @Override
            public Collection<Rule> getAll() {
                return Set.of();
            }
        };

        YamlConditionDTO cond1 = new YamlConditionDTO();
        cond1.id = "cond1";
        YamlConditionDTO cond2 = new YamlConditionDTO();
        cond2.id = "cond2";

        assertThrows(SerializationException.class,
                () -> provider.mapModules(List.of(cond1, cond2), Set.of("otherId1", "otherId2"), Condition.class));

        cond1.type = "type1";
        cond2.type = "type2";
        List<Condition> conditions = provider.mapModules(List.of(cond1, cond2), Set.of("otherId1", "otherId2"),
                Condition.class);
        assertThat(conditions, is(hasSize(2)));

        cond1.id = null;
        cond2.id = "   ";
        YamlConditionDTO cond3 = new YamlConditionDTO();
        cond3.id = "4";
        cond3.type = "type3";
        HashSet<@Nullable String> otherModuleIds = new HashSet<>(Set.of("1", "3", "otherId2"));
        otherModuleIds.add(null);
        conditions = provider.mapModules(List.of(cond1, cond2, cond3), otherModuleIds, Condition.class);
        assertThat(conditions, is(hasSize(3)));
        assertThat(conditions.get(0).getId(), is("2"));
        assertThat(conditions.get(1).getId(), is("5"));
        assertThat(conditions.get(2).getId(), is("4"));

        assertThrows(SerializationException.class,
                () -> provider.mapModules(List.of(cond1, cond2, cond3), otherModuleIds, Action.class));

        YamlModuleDTO trig1 = new YamlModuleDTO();
        YamlModuleDTO trig2 = new YamlModuleDTO();
        trig1.type = "sudden";
        trig2.type = "late";
        assertThrows(SerializationException.class,
                () -> provider.mapModules(List.of(trig1, trig2, cond3), otherModuleIds, Condition.class));
    }
}
