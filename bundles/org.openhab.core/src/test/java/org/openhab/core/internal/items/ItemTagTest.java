/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.internal.items;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Test;
import org.openhab.core.library.items.SwitchItem;

/**
 * Test Item Tag handling.
 *
 * @author Andre Fuechsel - Initial contribution
 */
public class ItemTagTest {

    private static final String TAG1 = "tag1";
    private static final String TAG2 = "täg2";
    private static final String TAG3 = "tag3";
    private static final String TAG1_UPPERCASE = "TAG1";
    private static final String TAG2_MIXED_CASE = "tÄg2";
    private static final String OTHER_TAG = "other tag";

    private static final String ITEM1 = "item1";

    @Test
    public void assertTagsAreAddedAndRemovedCorrectly() {
        SwitchItem item = new SwitchItem(ITEM1);
        assertThat(item.getTags().size(), is(0));

        item.addTag(TAG1);
        item.addTag(TAG2);

        assertThat(item.getTags().size(), is(2));
        assertThat(item.hasTag(TAG1), is(true));
        assertThat(item.hasTag(TAG2), is(true));
        assertThat(item.hasTag(TAG3), is(false));

        item.removeTag(TAG2);
        assertThat(item.getTags().size(), is(1));
        assertThat(item.hasTag(TAG1), is(true));
        assertThat(item.hasTag(TAG2), is(false));
        assertThat(item.hasTag(TAG3), is(false));

        item.removeAllTags();
        assertThat(item.getTags().size(), is(0));
        assertThat(item.hasTag(TAG1), is(false));
        assertThat(item.hasTag(TAG2), is(false));
        assertThat(item.hasTag(TAG3), is(false));
    }

    @Test
    public void testThatRemoveNonExistingTagDoesNotThrowNPE() {
        SwitchItem item = new SwitchItem(ITEM1);
        assertThat(item.getTags().size(), is(0));

        item.addTag(TAG1);
        item.addTag(TAG2);

        assertThat(item.getTags().size(), is(2));
        assertThat(item.hasTag(TAG1), is(true));
        assertThat(item.hasTag(TAG2), is(true));

        item.removeTag(OTHER_TAG);
        assertThat(item.getTags().size(), is(2));
        assertThat(item.hasTag(TAG1), is(true));
        assertThat(item.hasTag(TAG2), is(true));
    }

    @Test
    public void testThatTagsAreHandledCaseInsensitive() {
        SwitchItem item = new SwitchItem(ITEM1);
        assertThat(item.getTags().size(), is(0));

        item.addTag(TAG1);
        item.addTag(TAG2_MIXED_CASE);

        assertThat(item.getTags().size(), is(2));
        assertThat(item.hasTag(TAG1), is(true));
        assertThat(item.hasTag(TAG1_UPPERCASE), is(true));
        assertThat(item.hasTag(TAG2), is(true));
        assertThat(item.hasTag(TAG2_MIXED_CASE), is(true));
    }

    @Test
    public void testThatItemsToBeRemovedAreFoundCaseInsensitive() {
        SwitchItem item = new SwitchItem(ITEM1);
        assertThat(item.getTags().size(), is(0));

        item.addTag(TAG1);
        assertThat(item.getTags().size(), is(1));
        assertThat(item.hasTag(TAG1), is(true));

        item.removeTag(TAG1_UPPERCASE);
        assertThat(item.getTags().size(), is(0));
    }

    @Test
    public void testThatAddTagsIsWorkingCaseInsensitive() {
        SwitchItem item = new SwitchItem(ITEM1);
        assertThat(item.getTags().size(), is(0));

        item.addTags(TAG1_UPPERCASE, TAG2_MIXED_CASE, TAG3);
        assertThat(item.getTags().size(), is(3));
        assertThat(item.hasTag(TAG1), is(true));
        assertThat(item.hasTag(TAG2), is(true));
        assertThat(item.hasTag(TAG3), is(true));
    }

    @Test
    public void testThatAddTagsFromListIsWorkingCaseInsensitive() {
        SwitchItem item = new SwitchItem(ITEM1);
        assertThat(item.getTags().size(), is(0));

        item.addTags(Arrays.asList(TAG1_UPPERCASE, TAG2_MIXED_CASE, TAG3));
        assertThat(item.getTags().size(), is(3));
        assertThat(item.hasTag(TAG1), is(true));
        assertThat(item.hasTag(TAG2), is(true));
        assertThat(item.hasTag(TAG3), is(true));
    }

    @Test
    public void testThatGetTagsReturnsTagsAsTheyHaveBeenStored() {
        SwitchItem item = new SwitchItem(ITEM1);
        assertThat(item.getTags().size(), is(0));

        item.addTags(TAG1_UPPERCASE, TAG2_MIXED_CASE, TAG3);
        assertThat(item.getTags().size(), is(3));
        assertThat(item.hasTag(TAG1), is(true));
        assertThat(item.hasTag(TAG2), is(true));
        assertThat(item.hasTag(TAG3), is(true));

        assertThat(item.getTags().containsAll(Arrays.asList(TAG1_UPPERCASE, TAG2_MIXED_CASE, TAG3)), is(true));
    }
}
