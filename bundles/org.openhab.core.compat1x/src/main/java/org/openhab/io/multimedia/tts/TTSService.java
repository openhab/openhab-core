package org.openhab.io.multimedia.tts;

// NOTE: This interface is only kept in order to allow openHAB 1.x TTS services to correctly compile.
// openHAB 2 is NOT compatible with these services, not even through the compatibility layer.
// Instead, ESH/openHAB2 compatible TTS services should be used.

public interface TTSService {

    void say(String text, String voiceName, String outputDevice);
}
