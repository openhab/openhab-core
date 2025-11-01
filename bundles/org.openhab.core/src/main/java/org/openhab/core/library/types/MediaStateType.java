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
package org.openhab.core.library.types;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.AbstractEventFactory.ZonedDateTimeAdapter;
import org.openhab.core.types.Command;
import org.openhab.core.types.ComplexType;
import org.openhab.core.types.PrimitiveType;
import org.openhab.core.types.State;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This type is used by the {@link org.openhab.core.library.items.PlayerItem}.
 *
 * @author Laurent Arnal - Initial contribution
 */
@NonNullByDefault
public class MediaStateType implements ComplexType, State, Command {

    public static final String KEY_STATE = "state";
    public static final String KEY_DEVICE = "device";
    public static final String KEY_BINDING = "binding";

    private final PlayPauseType state;
    private final StringType device;
    private final StringType binding;
    private StringType currentPlayingArtistName;
    private StringType currentPlayingTrackName;
    private StringType currentPlayingArtUri;
    private DecimalType currentPlayingTrackPosition;
    private DecimalType currentPlayingTrackDuration;
    private DecimalType currentPlayingVolume;

    private static final Gson JSONCONVERTER = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter()).create();

    public MediaStateType() {
        this(PlayPauseType.PLAY, new StringType(""), new StringType(""));
    }

    public MediaStateType(PlayPauseType state, @Nullable StringType device, @Nullable StringType binding) {
        this.state = state;
        this.device = device != null ? device : new StringType("");
        this.binding = binding != null ? binding : new StringType("");
        this.currentPlayingArtistName = new StringType("");
        this.currentPlayingTrackName = new StringType("");
        this.currentPlayingArtUri = new StringType("");
        this.currentPlayingTrackPosition = new DecimalType("0.0");
        this.currentPlayingTrackDuration = new DecimalType("0.0");
        this.currentPlayingVolume = new DecimalType("0.0");
    }

    @Override
    public String toString() {
        return toFullString();
    }

    @Override
    public String toFullString() {
        return JSONCONVERTER.toJson(this);
        // return this.state.toFullString() + "," + this.command.toFullString() + "," + param + "," + device + ","
        // + binding;
    }

    public void setCurrentPlayingPosition(double value) {
        this.currentPlayingTrackPosition = new DecimalType(value);
    }

    public void setCurrentPlayingTrackDuration(double value) {
        this.currentPlayingTrackDuration = new DecimalType(value);
    }

    public void setCurrentPlayingVolume(double value) {
        this.currentPlayingVolume = new DecimalType(value);
    }

    public void setCurrentPlayingArtistName(String artistName) {
        this.currentPlayingArtistName = new StringType(artistName);
    }

    public void setCurrentPlayingTrackName(String trackName) {
        this.currentPlayingTrackName = new StringType(trackName);
    }

    public void setCurrentPlayingArtUri(String artUri) {
        this.currentPlayingArtUri = new StringType(artUri);
    }

    public static MediaStateType valueOf(String value) {
        try {
            MediaStateType res = JSONCONVERTER.fromJson(value, MediaStateType.class);
            if (res == null) {
                return new MediaStateType();
            }
            return res;
        } catch (Exception ex) {
            throw ex;
        }
    }

    @Override
    public String format(String pattern) {
        return String.format(pattern, state, device, binding);
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, binding);
    }

    @Override
    public SortedMap<String, PrimitiveType> getConstituents() {
        TreeMap<String, PrimitiveType> map = new TreeMap<>();
        map.put(KEY_STATE, getState());
        map.put(KEY_DEVICE, getDevice());
        map.put(KEY_BINDING, getBinding());
        return map;
    }

    public PlayPauseType getState() {
        return state;
    }

    public StringType getDevice() {
        return device;
    }

    public StringType getBinding() {
        return binding;
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

        MediaStateType other = (MediaStateType) obj;
        return Objects.equals(this.state, other.state) && Objects.equals(this.device, other.device)
                && Objects.equals(this.binding, other.binding)
                && Objects.equals(this.currentPlayingArtistName, other.currentPlayingArtistName)
                && Objects.equals(this.currentPlayingTrackName, other.currentPlayingTrackName)
                && Objects.equals(this.currentPlayingArtUri, other.currentPlayingArtUri)
                && Objects.equals(this.currentPlayingTrackPosition, other.currentPlayingTrackPosition)
                && Objects.equals(this.currentPlayingTrackDuration, other.currentPlayingTrackDuration)
                && Objects.equals(this.currentPlayingVolume, other.currentPlayingVolume);
    }
}
