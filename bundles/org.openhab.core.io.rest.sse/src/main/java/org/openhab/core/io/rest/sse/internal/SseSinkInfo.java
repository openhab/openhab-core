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
package org.openhab.core.io.rest.sse.internal;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.io.rest.sse.internal.util.SseUtil;

/**
 * The specific information we need to hold for a SSE sink.
 *
 * @author Markus Rathgeb - Initial contribution
 */
@NonNullByDefault
public class SseSinkInfo {

    private final List<String> regexFilters;

    public SseSinkInfo(String topicFilter) {
        this.regexFilters = SseUtil.convertToRegex(topicFilter);
    }

    public boolean matchesTopic(final String topic) {
        return regexFilters.stream().filter(topic::matches).findAny().isPresent();
    }
}
