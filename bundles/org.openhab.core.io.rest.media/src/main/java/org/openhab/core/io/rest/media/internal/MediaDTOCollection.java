package org.openhab.core.io.rest.media.internal;

import java.util.ArrayList;
import java.util.List;

public class MediaDTOCollection extends MediaDTO {
    private final List<MediaDTO> childs;

    public MediaDTOCollection(String id, String path, String type, String label) {
        super(id, path, type, label);
        childs = new ArrayList<>();
    }

    public void addMediaDTO(MediaDTO mediaDTO) {
        childs.add(mediaDTO);
    }

    public List<MediaDTO> getChilds() {
        return childs;
    }
}
