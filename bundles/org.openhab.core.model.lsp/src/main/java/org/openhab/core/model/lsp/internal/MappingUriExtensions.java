/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.model.lsp.internal;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.emf.common.util.URI;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.ide.server.UriExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link UriExtensions} implementation.
 *
 * It takes into account the fact that although language server and client both operate on the same set of files, their
 * file system location might be different due to remote access via SMB, SSH and the like.
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class MappingUriExtensions extends UriExtensions {

    private final Logger logger = LoggerFactory.getLogger(MappingUriExtensions.class);

    private final String rawConfigFolder;
    private final String serverLocation;

    private @Nullable String clientLocation;

    public MappingUriExtensions(String configFolder) {
        this.rawConfigFolder = configFolder;
        this.serverLocation = calcServerLocation(configFolder);
        logger.debug("The language server is using '{}' as its workspace", serverLocation);
    }

    protected String calcServerLocation(String configFolder) {
        Path configPath = Paths.get(configFolder);
        Path absoluteConfigPath = configPath.toAbsolutePath();
        java.net.URI configPathURI = absoluteConfigPath.toUri();
        return removeTrailingSlash(configPathURI.toString());
    }

    @Override
    public String toPath(URI uri) {
        return toPath(java.net.URI.create(uri.toString()));
    }

    @Override
    public String toPath(java.net.URI uri) {
        java.net.URI ret = uri;
        try {
            ret = Paths.get(uri).toUri();
        } catch (FileSystemNotFoundException e) {
            // fall-back to the argument
        }
        String clientPath = removeTrailingSlash(ret.toASCIIString());
        if (clientLocation != null) {
            clientPath = clientPath.replace(serverLocation, clientLocation);
        }
        return clientPath;
    }

    @Override
    public URI toUri(String pathWithScheme) {
        String decodedPathWithScheme = URLDecoder.decode(pathWithScheme, StandardCharsets.UTF_8);

        if (clientLocation != null && decodedPathWithScheme.startsWith(clientLocation)) {
            return map(decodedPathWithScheme);
        }

        clientLocation = guessClientPath(decodedPathWithScheme);
        if (clientLocation != null) {
            logger.debug("Identified client workspace as '{}'", clientLocation);
            return map(decodedPathWithScheme);
        }

        clientLocation = pathWithScheme;

        logger.debug("Path mapping could not be done for '{}', leaving it untouched", pathWithScheme);
        java.net.URI javaNetUri = java.net.URI.create(pathWithScheme);
        return URI.createURI(toPathAsInXtext212(javaNetUri));
    }

    @Override
    public String toUriString(URI uri) {
        if (clientLocation == null) {
            return uri.toString();
        }
        return mapToClientPath(uri.toString());
    }

    @Override
    public String toUriString(java.net.URI uri) {
        return toUriString(URI.createURI(uri.toString()));
    }

    private String mapToClientPath(String pathWithScheme) {
        String clientPath = toPathAsInXtext212(
                java.net.URI.create(pathWithScheme.replace(serverLocation, clientLocation)));
        logger.trace("Mapping server path {} to client path {}", pathWithScheme, clientPath);
        return clientPath;
    }

    protected final String removeTrailingSlash(String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        } else {
            return path;
        }
    }

    /**
     * Guess the client path.
     *
     * It works as follows: It starts with replacing the full clients path with the path of the config folder.
     * In the next iteration it shortens the path to be replaced by one subfolder.
     * It repeats that until the resulting filename exists.
     *
     * @param pathWithScheme the filename as coming from the client
     * @return the substring which needs to be replaced with the runtime's config folder path
     */
    protected @Nullable String guessClientPath(String pathWithScheme) {
        if (isPointingToConfigFolder(pathWithScheme)) {
            return removeTrailingSlash(pathWithScheme);
        } else if (isFolder(pathWithScheme)) {
            return removeTrailingSlash(pathWithScheme);
        }

        String currentPath = pathWithScheme;
        int nextIndex = getLastPathSegmentIndex(currentPath);
        while (nextIndex > -1) {
            currentPath = currentPath.substring(0, nextIndex);
            java.net.URI uri = toURI(pathWithScheme, currentPath);
            File realFile = new File(uri);
            if (realFile.exists()) {
                return currentPath;
            }

            nextIndex = getLastPathSegmentIndex(currentPath);
        }

        return null;
    }

    private boolean isFolder(String currentPath) {
        return !currentPath.substring(getLastPathSegmentIndex(currentPath)).contains(".");
    }

    private boolean isPointingToConfigFolder(String currentPath) {
        return currentPath.endsWith("/" + rawConfigFolder);
    }

    private int getLastPathSegmentIndex(String currentPath) {
        return removeTrailingSlash(currentPath).lastIndexOf("/");
    }

    private URI map(String pathWithScheme) {
        java.net.URI javaNetUri = toURI(pathWithScheme, clientLocation);
        logger.trace("Going to map path {}", javaNetUri);
        URI ret = URI.createURI(toPathAsInXtext212(javaNetUri));
        logger.trace("Mapped path {} to {}", pathWithScheme, ret);
        return ret;
    }

    private java.net.URI toURI(String pathWithScheme, String currentPath) {
        return java.net.URI.create(pathWithScheme.replace(currentPath, serverLocation));
    }

    private String toPathAsInXtext212(java.net.URI uri) {
        // org.eclipse.xtext.ide.server.UriExtensions:
        // In Xtext 2.14 the method "String toPath(java.netURI)" has been deprecated but still exist.
        // It delegate the logic internally to the new method "String toUriString(java.net.URI uri)".
        // That new method seems to return a different result for folder / directories with respect to
        // the present / absent of a trailing slash.

        // The old logic removes trailing slashes if it has been present in the input.
        // The new logic keeps trailing slashes if it has been present in the input.

        // input: file:///d/
        // output old: file:///d
        // output new: file:///d

        // input: file:///d/
        // output old: file:///d
        // output new: file:///d/

        // We use this method now to keep the old behavior.
        return Paths.get(uri).toUri().toString();
    }
}
