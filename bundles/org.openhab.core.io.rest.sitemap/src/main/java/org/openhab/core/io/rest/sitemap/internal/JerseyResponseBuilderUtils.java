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
package org.openhab.core.io.rest.sitemap.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.core.io.rest.sitemap.internal.JerseyResponseBuilderUtils.JerseyResponseBuilderDTO.ContextDTO;
import org.openhab.core.io.rest.sitemap.internal.JerseyResponseBuilderUtils.JerseyResponseBuilderDTO.ContextDTO.StreamInfoDTO;
import org.osgi.dto.DTO;

/**
 * Data transfer object until UIs are fixed.
 *
 * <p>
 * {@link https://github.com/openhab/openhab-core/issues/1216}
 *
 * @author Markus Rathgeb - Initial contribution
 */
public class JerseyResponseBuilderUtils extends DTO {

    public static class JerseyResponseBuilderDTO extends DTO {

        public static class ContextDTO extends DTO {

            public static class StreamInfoDTO extends DTO {
                int bufferSize;
                boolean directWrite;
                boolean isCommitted;
                boolean isClosed;
            }

            public Map<String, List<Object>> headers;
            public StreamInfoDTO committingOutputStream;
            public List<String> entityAnnotations;
            public StreamInfoDTO entityStream;
        }

        public String status;
        public ContextDTO context;
    }

    public static DTO created(final String location) {
        JerseyResponseBuilderDTO jrbDTO = new JerseyResponseBuilderDTO();
        jrbDTO.status = "CREATED";
        jrbDTO.context = new ContextDTO();
        jrbDTO.context.headers = new HashMap<>();
        jrbDTO.context.headers.put("Location", Arrays.asList(location));
        jrbDTO.context.committingOutputStream = new StreamInfoDTO();
        jrbDTO.context.committingOutputStream.bufferSize = 0;
        jrbDTO.context.committingOutputStream.directWrite = true;
        jrbDTO.context.committingOutputStream.isCommitted = false;
        jrbDTO.context.committingOutputStream.isClosed = false;
        jrbDTO.context.entityAnnotations = new ArrayList<>(0);
        jrbDTO.context.entityStream = new StreamInfoDTO();
        jrbDTO.context.entityStream.bufferSize = 0;
        jrbDTO.context.entityStream.directWrite = true;
        jrbDTO.context.entityStream.isCommitted = false;
        jrbDTO.context.entityStream.isClosed = false;
        return jrbDTO;
    }
}
