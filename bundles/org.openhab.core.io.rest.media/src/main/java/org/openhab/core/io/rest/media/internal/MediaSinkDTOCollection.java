package org.openhab.core.io.rest.media.internal;

import java.util.ArrayList;
import java.util.List;

public class MediaSinkDTOCollection extends MediaSinkDTO {
    private final List<MediaSinkDTO> childs;

    public MediaSinkDTOCollection() {
        super("id", "name", "type", "binding");
        childs = new ArrayList<>();
    }

    public void addMediaSinkDTO(MediaSinkDTO mediaSinkDTO) {
        childs.add(mediaSinkDTO);
    }

    public List<MediaSinkDTO> getChilds() {
        return childs;
    }
}
