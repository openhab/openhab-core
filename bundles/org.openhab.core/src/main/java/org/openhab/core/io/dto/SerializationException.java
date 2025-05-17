package org.openhab.core.io.dto;

import java.io.IOException;

public class SerializationException extends IOException { // TODO: (Nad) Header + JavaDocs

    private static final long serialVersionUID = 1L;

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
