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
package org.openhab.core.automation.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.openhab.core.automation.RulePredicates.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleRegistry;
import org.openhab.core.test.java.JavaOSGiTest;

/**
 * Testing the {@link Predicate}s for {@link RuleImpl}s.
 *
 * @author Victor Toni - Initial contribution
 */
@NonNullByDefault
public class RuleRegistryTest extends JavaOSGiTest {

    private @NonNullByDefault({}) RuleRegistry ruleRegistry;

    @BeforeEach
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
        assertEquals(0, ruleRegistry.getAll().size(), "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking rule without tag
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        name = "rule_without_tag";
        final RuleImpl ruleWithoutTag = new RuleImpl(name);

        addedRule = ruleRegistry.add(ruleWithoutTag);
        assertNotNull(addedRule, "RuleImpl for:" + name);

        getRule = ruleRegistry.get(name);
        assertNotNull(getRule, "RuleImpl for:" + name);

        assertEquals(1, ruleRegistry.getAll().size(), "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking that results from stream() have the same size as getAll() above
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        assertEquals(1, ruleRegistry.stream().collect(Collectors.toList()).size(), "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking predicates
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        assertEquals(1, ruleRegistry.stream().filter(hasNoTags()).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking rule with 1 tag
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        name = "rule_with_tag1";

        tags = new HashSet<>();
        tags.add(tag1);

        final RuleImpl ruleWithTag1 = new RuleImpl(name);
        ruleWithTag1.setTags(tags);

        addedRule = ruleRegistry.add(ruleWithTag1);
        assertNotNull(addedRule, "RuleImpl for:" + name);

        getRule = ruleRegistry.get(name);
        assertNotNull(getRule, "RuleImpl for:" + name);

        assertEquals(2, ruleRegistry.getAll().size(), "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking that results from stream() have the same size as getAll() above
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        assertEquals(2, ruleRegistry.stream().collect(Collectors.toList()).size(), "RuleImpl list size");

        Collection<Rule> rulesWithTag1 = ruleRegistry.getByTags(tag1);
        assertEquals(1, rulesWithTag1.size(), "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking predicates
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        assertEquals(1, ruleRegistry.stream().filter(hasNoTags()).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(1, ruleRegistry.stream().filter(hasAnyOfTags(tags)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(1, ruleRegistry.stream().filter(hasAnyOfTags(tag1)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(1, ruleRegistry.stream().filter(hasAllTags(tags)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(1, ruleRegistry.stream().filter(hasAllTags(tag1)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking rule with 2 tags
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        name = "rule_with_tag1_tag2";

        tags = new HashSet<>();
        tags.add(tag1);
        tags.add(tag2);

        final RuleImpl ruleWithTag1Tag2 = new RuleImpl(name);
        ruleWithTag1Tag2.setTags(tags);

        addedRule = ruleRegistry.add(ruleWithTag1Tag2);
        assertNotNull(addedRule, "RuleImpl for:" + name);

        getRule = ruleRegistry.get(name);
        assertNotNull(getRule, "RuleImpl for:" + name);

        assertEquals(3, ruleRegistry.getAll().size(), "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking that results from stream() have the same size as getAll() above
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        assertEquals(3, ruleRegistry.stream().collect(Collectors.toList()).size(), "RuleImpl list size");

        rulesWithTag1 = ruleRegistry.getByTags(tag1);
        assertEquals(2, rulesWithTag1.size(), "RuleImpl list size");

        Collection<Rule> rulesWithTag2 = ruleRegistry.getByTags(tag2);
        assertEquals(1, rulesWithTag2.size(), "RuleImpl list size");

        Collection<Rule> rulesWithAllOfTag1Tag2 = ruleRegistry.getByTags(tag1, tag2);
        assertEquals(1, rulesWithAllOfTag1Tag2.size(), "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking predicates
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        assertEquals(1, ruleRegistry.stream().filter(hasNoTags()).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(2, ruleRegistry.stream().filter(hasAnyOfTags(tags)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(2, ruleRegistry.stream().filter(hasAnyOfTags(tag1)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(2, ruleRegistry.stream().filter(hasAnyOfTags(tag1, tag2)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(1, ruleRegistry.stream().filter(hasAnyOfTags(tag2)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(1, ruleRegistry.stream().filter(hasAnyOfTags(tag1).and(hasAnyOfTags(tag2)))
                .collect(Collectors.toList()).size(), "RuleImpl list size");

        assertEquals(2, ruleRegistry.stream().filter(hasAllTags(tag1)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(1, ruleRegistry.stream().filter(hasAllTags(tags)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(1, ruleRegistry.stream().filter(hasAllTags(tag1, tag2)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(1, ruleRegistry.stream().filter(hasAllTags(tag2)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(1, ruleRegistry.stream().filter(hasAllTags(tag1).and(hasAllTags(tag2)))
                .collect(Collectors.toList()).size(), "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking rule with 3 tags
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        name = "rule_with_tag1_tag2_tag3";

        tags = new HashSet<>();
        tags.add(tag1);
        tags.add(tag2);
        tags.add(tag3);

        final RuleImpl ruleWithTag1Tag2Tag3 = new RuleImpl("rule_with_tag1_tag2_tag3");
        ruleWithTag1Tag2Tag3.setTags(tags);

        addedRule = ruleRegistry.add(ruleWithTag1Tag2Tag3);
        assertNotNull(addedRule, "RuleImpl for:" + name);

        getRule = ruleRegistry.get(name);
        assertNotNull(getRule, "RuleImpl for:" + name);

        assertEquals(4, ruleRegistry.getAll().size(), "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking that results from stream() have the same size as getAll() above
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        assertEquals(4, ruleRegistry.stream().collect(Collectors.toList()).size(), "RuleImpl list size");

        rulesWithTag1 = ruleRegistry.getByTags(tag1);
        assertEquals(3, rulesWithTag1.size(), "RuleImpl list size");

        rulesWithTag2 = ruleRegistry.getByTags(tag2);
        assertEquals(2, rulesWithTag2.size(), "RuleImpl list size");

        Collection<Rule> rulesWithTag3 = ruleRegistry.getByTags(tag3);
        assertEquals(1, rulesWithTag3.size(), "RuleImpl list size");

        rulesWithAllOfTag1Tag2 = ruleRegistry.getByTags(tag1, tag2);
        assertEquals(2, rulesWithAllOfTag1Tag2.size(), "RuleImpl list size");

        Collection<Rule> rulesWithAllTag1Tag3 = ruleRegistry.getByTags(tag1, tag3);
        assertEquals(1, rulesWithAllTag1Tag3.size(), "RuleImpl list size");

        Collection<Rule> rulesWithAllOfTag1Tag2Tag3 = ruleRegistry.getByTags(tag1, tag2, tag3);
        assertEquals(1, rulesWithAllOfTag1Tag2Tag3.size(), "RuleImpl list size");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // checking predicates
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        assertEquals(1, ruleRegistry.stream().filter(hasNoTags()).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(3, ruleRegistry.stream().filter(hasAnyOfTags(tags)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(3, ruleRegistry.stream().filter(hasAnyOfTags(tag1)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(3, ruleRegistry.stream().filter(hasAnyOfTags(tag1, tag2)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(3,
                ruleRegistry.stream().filter(hasAnyOfTags(tag1, tag2, tag3)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(2, ruleRegistry.stream().filter(hasAnyOfTags(tag2)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(2, ruleRegistry.stream().filter(hasAnyOfTags(tag2, tag3)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(2, ruleRegistry.stream().filter(hasAnyOfTags(tag1).and(hasAnyOfTags(tag2)))
                .collect(Collectors.toList()).size(), "RuleImpl list size");

        assertEquals(1, ruleRegistry.stream().filter(hasAnyOfTags(tag1).and(hasAnyOfTags(tag3)))
                .collect(Collectors.toList()).size(), "RuleImpl list size");
        assertEquals(1, ruleRegistry.stream().filter(hasAnyOfTags(tag2).and(hasAnyOfTags(tag3)))
                .collect(Collectors.toList()).size(), "RuleImpl list size");

        assertEquals(1, ruleRegistry.stream().filter(hasAnyOfTags(tag3)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(3, ruleRegistry.stream().filter(hasAllTags(tag1)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(2, ruleRegistry.stream().filter(hasAllTags(tag2)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(2, ruleRegistry.stream().filter(hasAllTags(tag1, tag2)).collect(Collectors.toList()).size(),
                "RuleImpl list size");

        assertEquals(1, ruleRegistry.stream().filter(hasAllTags(tags)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
        assertEquals(1, ruleRegistry.stream().filter(hasAllTags(tag1, tag2, tag3)).collect(Collectors.toList()).size(),
                "RuleImpl list size");
    }
}
