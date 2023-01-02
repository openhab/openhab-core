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
package org.openhab.core.addon.marketplace.internal.community.model;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

/**
 * A DTO class mapped to the Discourse topic API.
 *
 * @author Yannick Schaus - Initial contribution
 */
public class DiscourseTopicResponseDTO {
    public Integer id;

    @SerializedName("post_stream")
    public DiscoursePostStream postStream;

    public String title;
    @SerializedName("posts_count")
    public Integer postsCount;
    @SerializedName("image_url")
    public String imageUrl;

    @SerializedName("created_at")
    public Date createdAt;
    @SerializedName("updated_at")
    public Date updatedAt;
    @SerializedName("last_posted")
    public Date lastPosted;

    @SerializedName("like_count")
    public Integer likeCount;
    public Integer views;

    public String[] tags;
    @SerializedName("category_id")
    public Integer categoryId;

    public DiscourseTopicDetails details;

    public static class DiscoursePostAuthor {
        public Integer id;
        public String username;
        @SerializedName("avatar_template")
        public String avatarTemplate;
    }

    public static class DiscoursePostLink {
        public String url;
        public Boolean internal;
        public Integer clicks;
    }

    public static class DiscoursePostStream {
        public DiscoursePost[] posts;
    }

    public static class DiscoursePost {
        public Integer id;

        public String username;
        @SerializedName("display_username")
        public String displayUsername;

        @SerializedName("created_at")
        public Date createdAt;
        @SerializedName("updated_at")
        public Date updatedAt;

        public String cooked;

        @SerializedName("link_counts")
        public DiscoursePostLink[] linkCounts;
    }

    public static class DiscourseTopicDetails {
        @SerializedName("created_by")
        public DiscoursePostAuthor createdBy;
        @SerializedName("last_poster")
        public DiscoursePostAuthor lastPoster;

        public DiscoursePostLink[] links;
    }
}
