package org.openhab.core.io.dto;

import org.eclipse.jdt.annotation.NonNull;

public interface ModularDTO<D, M, N> { // TODO: (Nad) Header + JavaDocs

    @NonNull D toDto(@NonNull N node, @NonNull M mapper) throws SerializationException;
}
