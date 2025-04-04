package org.openhab.core.media.model;

@FunctionalInterface
public interface MediaAllocator<T extends MediaEntry> {
    T alloc();

}
