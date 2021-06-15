/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.addon.marketplace.internal.community.model;

import java.util.Date;

/**
 * A DTO class mapped to the Discourse tagged topic list API.
 *
 * @author Yannick Schaus - Initial contribution
 *
 */
public class DiscourseCategoryResponse {
    public DiscourseUser[] users;
    public DiscourseTopicList topic_list;

    public class DiscourseUser {
        public Integer id;
        public String username;
        public String name;
        public String avatar_template;
    }

    public class DiscourseTopicList {
        public String more_topics_url;
        public Integer per_page;
        public DiscourseTopicItem[] topics;
    }

    public class DiscoursePosterInfo {
        public String extras;
        public String description;
        public Integer user_id;
    }

    public class DiscourseTopicItem {
        public Integer id;
        public String title;
        public String slug;
        public String[] tags;
        public Integer posts_count;
        public String image_url;
        public Date created_at;
        public Integer like_count;
        public Integer views;
        public Integer category_id;
        public DiscoursePosterInfo[] posters;
    }
}
