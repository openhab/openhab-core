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
 * A DTO class mapped to the Discourse topic API.
 *
 * @author Yannick Schaus - Initial contribution
 *
 */
public class DiscourseTopicResponse {
    public Integer id;

    public DiscoursePostStream post_stream;

    public String title;
    public Integer posts_count;

    public Date created_at;
    public Date updated_at;
    public Date last_posted;

    public Integer like_count;
    public Integer views;

    public String[] tags;
    public Integer category_id;

    public DiscourseTopicDetails details;

    public class DiscoursePostAuthor {
        public Integer id;
        public String username;
        public String avatar_template;
    }

    public class DiscoursePostLink {
        public String url;
        public Boolean internal;
        public Integer clicks;
    }

    public class DiscoursePostStream {
        public DiscoursePost[] posts;
    }

    public class DiscoursePost {
        public Integer id;

        public String username;
        public String display_username;

        public Date created_at;
        public Date updated_at;

        public String cooked;

        public DiscoursePostLink[] link_counts;
    }

    public class DiscourseTopicDetails {
        public DiscoursePostAuthor created_by;
        public DiscoursePostAuthor last_poster;

        public DiscoursePostLink[] links;
    }
}
