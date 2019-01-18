/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.transport.mqtt.internal;

import java.util.ArrayList;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.io.transport.mqtt.MqttMessageSubscriber;

/**
 * A list of all subscribers for a given topic. This object also stores a regex version of the topic, where
 * the MQTT wildcards got replaced.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class TopicSubscribers extends ArrayList<MqttMessageSubscriber> {
    private static final long serialVersionUID = -2969599983479371961L;
    final String regexMatchTopic;

    public TopicSubscribers(String topic) {
        this.regexMatchTopic = StringUtils.replace(StringUtils.replace(Matcher.quoteReplacement(topic), "+", "[^/]*"),
                "#", ".*");
    }
}
