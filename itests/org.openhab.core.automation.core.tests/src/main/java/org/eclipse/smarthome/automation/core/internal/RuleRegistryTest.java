/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.automation.core.internal;

import static org.eclipse.smarthome.automation.RulePredicates.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing the {@link Predicate}s for {@link RuleImpl}s.
 *
 * @author Victor Toni - Initial contribution
 */
public class RuleRegistryTest extends JavaOSGiTest {

    RuleRegistry ruleRegistry;

    @Before
    public void setup() {
        registerVolatileStorageService();
        ruleRegistry = getService(RuleRegistry.class);
    }

    /**
     * test adding and retrieving rules
     *
     */
    @Test
    public void testAddRetrieveRules() {
        final String tag1 = "tag1";
        final String tag2 = "tag2";
        final String tag3 = "tag3";

        Set<String> tags;
        String name;
        Rule addedRule;
        Rule getRule;

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking RuleRegistry
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertEquals("RuleImpl list size", 0, ruleRegistry.getAll().size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking rule without tag
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        name = "rule_without_tag";
        final RuleImpl rule_without_tag = new RuleImpl(name);

        addedRule = ruleRegistry.add(rule_without_tag);
        Assert.assertNotNull("RuleImpl for:" + name, addedRule);

        getRule = ruleRegistry.get(name);
        Assert.assertNotNull("RuleImpl for:" + name, getRule);

        Assert.assertEquals("RuleImpl list size", 1, ruleRegistry.getAll().size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking that results from stream() have the same size as getAll() above
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertEquals("RuleImpl list size", 1, ruleRegistry.stream().collect(Collectors.toList()).size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking predicates
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasNoTags()).collect(Collectors.toList()).size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking rule with 1 tag
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        name = "rule_with_tag1";

        tags = new HashSet<>();
        tags.add(tag1);

        final RuleImpl rule_with_tag1 = new RuleImpl(name);
        rule_with_tag1.setTags(tags);

        addedRule = ruleRegistry.add(rule_with_tag1);
        Assert.assertNotNull("RuleImpl for:" + name, addedRule);

        getRule = ruleRegistry.get(name);
        Assert.assertNotNull("RuleImpl for:" + name, getRule);

        Assert.assertEquals("RuleImpl list size", 2, ruleRegistry.getAll().size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking that results from stream() have the same size as getAll() above
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertEquals("RuleImpl list size", 2, ruleRegistry.stream().collect(Collectors.toList()).size());

        Collection<Rule> rules_with_tag1 = ruleRegistry.getByTags(tag1);
        Assert.assertEquals("RuleImpl list size", 1, rules_with_tag1.size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking predicates
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasNoTags()).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAnyOfTags(tags)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAnyOfTags(tag1)).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAllTags(tags)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAllTags(tag1)).collect(Collectors.toList()).size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking rule with 2 tags
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        name = "rule_with_tag1_tag2";

        tags = new HashSet<>();
        tags.add(tag1);
        tags.add(tag2);

        final RuleImpl rule_with_tag1_tag2 = new RuleImpl(name);
        rule_with_tag1_tag2.setTags(tags);

        addedRule = ruleRegistry.add(rule_with_tag1_tag2);
        Assert.assertNotNull("RuleImpl for:" + name, addedRule);

        getRule = ruleRegistry.get(name);
        Assert.assertNotNull("RuleImpl for:" + name, getRule);

        Assert.assertEquals("RuleImpl list size", 3, ruleRegistry.getAll().size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking that results from stream() have the same size as getAll() above
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertEquals("RuleImpl list size", 3, ruleRegistry.stream().collect(Collectors.toList()).size());

        rules_with_tag1 = ruleRegistry.getByTags(tag1);
        Assert.assertEquals("RuleImpl list size", 2, rules_with_tag1.size());

        Collection<Rule> rules_with_tag2 = ruleRegistry.getByTags(tag2);
        Assert.assertEquals("RuleImpl list size", 1, rules_with_tag2.size());

        Collection<Rule> rules_with_all_of_tag1_tag2 = ruleRegistry.getByTags(tag1, tag2);
        Assert.assertEquals("RuleImpl list size", 1, rules_with_all_of_tag1_tag2.size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking predicates
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasNoTags()).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 2,
                ruleRegistry.stream().filter(hasAnyOfTags(tags)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 2,
                ruleRegistry.stream().filter(hasAnyOfTags(tag1)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 2,
                ruleRegistry.stream().filter(hasAnyOfTags(tag1, tag2)).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAnyOfTags(tag2)).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 1, ruleRegistry.stream()
                .filter(hasAnyOfTags(tag1).and(hasAnyOfTags(tag2))).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 2,
                ruleRegistry.stream().filter(hasAllTags(tag1)).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAllTags(tags)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAllTags(tag1, tag2)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAllTags(tag2)).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 1, ruleRegistry.stream()
                .filter(hasAllTags(tag1).and(hasAllTags(tag2))).collect(Collectors.toList()).size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking rule with 3 tags
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        name = "rule_with_tag1_tag2_tag3";

        tags = new HashSet<>();
        tags.add(tag1);
        tags.add(tag2);
        tags.add(tag3);

        final RuleImpl rule_with_tag1_tag2_tag3 = new RuleImpl("rule_with_tag1_tag2_tag3");
        rule_with_tag1_tag2_tag3.setTags(tags);

        addedRule = ruleRegistry.add(rule_with_tag1_tag2_tag3);
        Assert.assertNotNull("RuleImpl for:" + name, addedRule);

        getRule = ruleRegistry.get(name);
        Assert.assertNotNull("RuleImpl for:" + name, getRule);

        Assert.assertEquals("RuleImpl list size", 4, ruleRegistry.getAll().size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking that results from stream() have the same size as getAll() above
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertEquals("RuleImpl list size", 4, ruleRegistry.stream().collect(Collectors.toList()).size());

        rules_with_tag1 = ruleRegistry.getByTags(tag1);
        Assert.assertEquals("RuleImpl list size", 3, rules_with_tag1.size());

        rules_with_tag2 = ruleRegistry.getByTags(tag2);
        Assert.assertEquals("RuleImpl list size", 2, rules_with_tag2.size());

        Collection<Rule> rules_with_tag3 = ruleRegistry.getByTags(tag3);
        Assert.assertEquals("RuleImpl list size", 1, rules_with_tag3.size());

        rules_with_all_of_tag1_tag2 = ruleRegistry.getByTags(tag1, tag2);
        Assert.assertEquals("RuleImpl list size", 2, rules_with_all_of_tag1_tag2.size());

        Collection<Rule> rules_with_all_tag1_tag3 = ruleRegistry.getByTags(tag1, tag3);
        Assert.assertEquals("RuleImpl list size", 1, rules_with_all_tag1_tag3.size());

        Collection<Rule> rules_with_all_of_tag1_tag2_tag3 = ruleRegistry.getByTags(tag1, tag2, tag3);
        Assert.assertEquals("RuleImpl list size", 1, rules_with_all_of_tag1_tag2_tag3.size());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking predicates
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasNoTags()).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 3,
                ruleRegistry.stream().filter(hasAnyOfTags(tags)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 3,
                ruleRegistry.stream().filter(hasAnyOfTags(tag1)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 3,
                ruleRegistry.stream().filter(hasAnyOfTags(tag1, tag2)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 3,
                ruleRegistry.stream().filter(hasAnyOfTags(tag1, tag2, tag3)).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 2,
                ruleRegistry.stream().filter(hasAnyOfTags(tag2)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 2,
                ruleRegistry.stream().filter(hasAnyOfTags(tag2, tag3)).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 2, ruleRegistry.stream()
                .filter(hasAnyOfTags(tag1).and(hasAnyOfTags(tag2))).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 1, ruleRegistry.stream()
                .filter(hasAnyOfTags(tag1).and(hasAnyOfTags(tag3))).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 1, ruleRegistry.stream()
                .filter(hasAnyOfTags(tag2).and(hasAnyOfTags(tag3))).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAnyOfTags(tag3)).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 3,
                ruleRegistry.stream().filter(hasAllTags(tag1)).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 2,
                ruleRegistry.stream().filter(hasAllTags(tag2)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 2,
                ruleRegistry.stream().filter(hasAllTags(tag1, tag2)).collect(Collectors.toList()).size());

        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAllTags(tags)).collect(Collectors.toList()).size());
        Assert.assertEquals("RuleImpl list size", 1,
                ruleRegistry.stream().filter(hasAllTags(tag1, tag2, tag3)).collect(Collectors.toList()).size());

    }

}
