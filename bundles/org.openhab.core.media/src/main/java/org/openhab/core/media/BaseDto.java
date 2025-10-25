package org.openhab.core.media;

import java.util.List;

import org.openhab.core.media.model.MediaEntry;

/**
 * A base class for all DTO use in Media Service
 *
 * @author Laurent Arnal - Initial contribution
 */
public class BaseDto {
    private String id;
    private String type;
    private String uri;
    private String name;
    private List<Image> images;

    public String getKey() {
        return id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getUri() {
        return uri;
    }

    public List<Image> getImages() {
        return images;
    }

    public String getArtwork() {
        try {
            List<Image> imagesList = getImages();
            if (imagesList != null && imagesList.getFirst() != null) {
                return imagesList.getFirst().getUrl();
            }
            return "";
        } catch (Exception ex) {
            return "";
        }
    }

    public void initFields(MediaEntry entry) {
    }
}
