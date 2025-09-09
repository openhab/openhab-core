/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.hli;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.registry.Identifiable;

/**
 * A card is a special type of {@link Component} at the root of the hierarchy.
 * It has a number of specific parameters, a set of tags, a timestamp, and is
 * identifiable by its UID (generally a GUID).
 *
 * @author Yannick Schaus - Initial contribution
 */
@NonNullByDefault
public class Card extends Component implements Identifiable<String> {
    private final String uid;
    private @Nullable String title;
    private @Nullable String subtitle;
    private Set<String> tags = new HashSet<String>();
    private Set<String> objects = new HashSet<String>();
    private Set<String> locations = new HashSet<String>();
    private boolean ephemeral;
    private @Nullable Date timestamp;
    private boolean bookmarked;
    private boolean notReuseableInChat;
    private boolean addToDeckDenied;

    /**
     * Constructs a card.
     *
     * @param name the name of the UI component to render the card on client frontends, ie. "HbCard"
     */
    public Card(String name) {
        super(name);
        this.uid = UUID.randomUUID().toString();
    }

    /**
     * Constructs a Card with a specific UID.
     *
     * @param uid the UID of the new card
     * @param name the name of the UI component to render the card on client frontends, ie. "HbCard"
     */
    public Card(String uid, String name) {
        super(name);
        this.uid = uid;
    }

    @Override
    public String getUID() {
        return uid;
    }

    /**
     * Gets the card's title
     *
     * @return the card title
     */
    public @Nullable String getTitle() {
        return title;
    }

    /**
     * Sets the card's title
     *
     * @param title the card title
     */
    public void setTitle(@Nullable String title) {
        this.title = title;
    }

    /**
     * Gets the card's subtitle
     *
     * @return the card subtitle
     */
    public @Nullable String getSubtitle() {
        return subtitle;
    }

    /**
     * Sets the card's subtitle
     *
     * @param subtitle the card subtitle
     */
    public void setSubtitle(@Nullable String subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * Gets the set of tags attached to the card
     *
     * @return the card tags
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Gets the set of object attributes attached to the card
     *
     * @return the card object attributes
     */
    public Set<String> getObjectAttributes() {
        return objects;
    }

    /**
     * Gets the set of location attributes attached to the card
     *
     * @return the card location attributes
     */
    public Set<String> getLocationAttributes() {
        return tags;
    }

    /**
     * Returns whether the card is bookmarked (appears on a dedicated "bookmarks" page)
     *
     * @return the card bookmark status
     */
    public boolean isBookmarked() {
        return bookmarked;
    }

    /**
     * Specifies whether the card is bookmarked or not (appears on a dedicated "bookmarks" page)
     *
     * @param bookmarked the card bookmark status
     */
    public void setBookmark(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    /**
     * Returns whether the card should be ignored during the chat sessions, even if its attributes match an @link
     * {@link Intent}'s extracted entities.
     *
     * @return true if the card is not be considered during chat sessions, false (default) otherwise
     */
    public boolean isNotReuseableInChat() {
        return notReuseableInChat;
    }

    /**
     * Specifies whether the card should be ignored during the chat sessions, even if its attributes match an @link
     * {@link Intent}'s extracted entities.
     *
     * @param notReuseableInChat true if the card is not be considered during chat sessions, false (default) otherwise
     */
    public void setNotReuseableInChat(boolean notReuseableInChat) {
        this.notReuseableInChat = notReuseableInChat;
    }

    /**
     * Returns whether the card can be added to the "card deck" permanently
     *
     * @return true if the card cannot be saved permanently, false (default) if it can
     */
    public boolean isAddToDeckDenied() {
        return addToDeckDenied;
    }

    /**
     * Returns whether the card can be added to the "card deck" permanently
     *
     * @param addToDeckDenied true if the card cannot be saved permanently, false (default) if it can
     */
    public void setAddToDeckDenied(boolean addToDeckDenied) {
        this.addToDeckDenied = addToDeckDenied;
    }

    /**
     * Returns whether the card is ephemeral, meaning it is not saved permanently to the @link {@link CardRegistry} and
     * will be purged after a number of newer ephemeral cards are added
     *
     * @return true if the card is ephemeral, false (default) otherwise
     */
    public boolean isEphemeral() {
        return ephemeral;
    }

    /**
     * Specifies whether the card is ephemeral, meaning it is not saved permanently to the @link {@link CardRegistry}
     * and will be purged once a number of newer ephemeral cards are added
     *
     * @param ephemeral true if the card is ephemeral, false (default) otherwise
     */
    public void setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    /**
     * Gets the timestamp of the card
     *
     * @return the timestamp
     */
    public @Nullable Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the specified timestamp of the card
     *
     * @param date the timestamp
     */
    public void setTimestamp(Date date) {
        this.timestamp = date;
    }

    /**
     * Updates the timestamp of the card to the current date & time.
     */
    public void updateTimestamp() {
        this.timestamp = new Date();
    }

    /**
     * Returns whether the card has a certain tag
     *
     * @param tag the tag to check
     * @return true if the card is tagged with the specified tag
     */
    public boolean hasTag(String tag) {
        return (tags != null && tags.contains(tag));
    }

    /**
     * Adds a tag to the card
     *
     * @param tag the tag to add
     */
    public void addTag(String tag) {
        this.tags.add(tag);
    }

    /**
     * Adds several tags to the card
     *
     * @param tags the tags to add
     */
    public void addTags(Collection<String> tags) {
        this.tags.addAll(tags);
    }

    /**
     * Adds several tags to the card
     *
     * @param tags the tags to add
     */
    public void addTags(String... tags) {
        this.tags.addAll(Arrays.asList(tags));
    }

    /**
     * Removes a tag on a card
     *
     * @param tag the tag to remove
     */
    public void removeTag(String tag) {
        this.tags.remove(tag);
    }

    /**
     * Removes all tags on the card
     */
    public void removeAllTags() {
        this.tags.clear();
    }

    /**
     * Adds an object attribute to the card
     *
     * @param object the object to add
     */
    public void addObjectAttribute(String object) {
        this.objects.add(object);
    }

    /**
     * Adds several object attributes to the card
     *
     * @param objects the objects to add
     */
    public void addObjectAttributes(Collection<String> objects) {
        this.objects.addAll(objects);
    }

    /**
     * Adds several object attributes to the card
     *
     * @param objects the objects to add
     */
    public void addObjectAttributes(String... objects) {
        this.objects.addAll(Arrays.asList(objects));
    }

    /**
     * Removes an object attribute on a card
     *
     * @param object the object to remove
     */
    public void removeObjectAttribute(String object) {
        this.objects.remove(object);
    }

    /**
     * Returns whether the card has the specified object attribute (case insensitive)
     *
     * @param object
     */
    public boolean hasObjectAttribute(@Nullable String object) {
        if (this.objects == null || object == null || object.isEmpty()) {
            return false;
        }
        return this.objects.stream().anyMatch(o -> o.equalsIgnoreCase(object));
    }

    /**
     * Adds an location attribute to the card
     *
     * @param location the location to add
     */
    public void addLocationAttribute(String location) {
        this.locations.add(location);
    }

    /**
     * Adds several object attributes to the card
     *
     * @param locations the locations to add
     */
    public void addLocationAttributes(Collection<String> locations) {
        this.locations.addAll(locations);
    }

    /**
     * Adds several object attributes to the card
     *
     * @param locations the locations to add
     */
    public void addLocationAttributes(String... locations) {
        this.locations.addAll(Arrays.asList(locations));
    }

    /**
     * Removes an object attribute on a card
     *
     * @param location the location to remove
     */
    public void removeLocationAttribute(String location) {
        this.locations.remove(location);
    }

    /**
     * Returns whether the card has the specified location attribute
     *
     * @param location
     */
    public boolean hasLocationAttribute(@Nullable String location) {
        if (this.locations == null || location == null || location.isEmpty()) {
            return false;
        }
        return this.locations.stream().anyMatch(o -> o.equalsIgnoreCase(location));
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Card other = (Card) obj;
        if (!this.getUID().equals(other.getUID())) {
            return false;
        }
        return true;
    }
}
