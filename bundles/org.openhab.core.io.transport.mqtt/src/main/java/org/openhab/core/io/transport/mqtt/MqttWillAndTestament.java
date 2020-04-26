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
package org.openhab.core.io.transport.mqtt;

import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Class encapsulating the last will and testament that is published after the client has gone offline.
 *
 * @author Markus Mann - Initial contribution
 */
@NonNullByDefault
public class MqttWillAndTestament {
    private static final int DFL_QOS = 0;
    private static final boolean DFL_RETAIN = false;

    private final String topic;
    private final byte @Nullable [] payload;
    private final int qos;
    private final boolean retain;

    /**
     * Create an instance of the last will using a string with the following format:<br/>
     * topic:message:qos:retained <br/>
     * Where
     * <ul>
     * <li>topic is a normal topic string (no placeholders are allowed)</li>
     * <li>message the message to send</li>
     * <li>qos Valid values are 0 (Deliver at most once),1 (Deliver at least once) or 2</li>
     * <li>retain true if messages shall be retained</li>
     * </ul>
     *
     * @param string the string to parse. If null, null is returned
     * @return the will instance, will be null only if parameter is null
     */
    public static @Nullable MqttWillAndTestament fromString(@Nullable String string) {
        return fromString(string, null, null, null, null);
    }

    public static @Nullable MqttWillAndTestament fromString(@Nullable String string, @Nullable String topic,
            byte @Nullable [] payload, @Nullable Integer qos, @Nullable Boolean retain) {
        String tmpTopic = null;
        byte[] tmpPayload = null;
        int tmpQos = DFL_QOS;
        boolean tmpRetain = DFL_RETAIN;

        // Parse string if given.
        if (string != null) {
            String[] components = string.split(":");
            for (int i = 0; i < Math.min(components.length, 4); i++) {
                String component = components[i];
                String value = component == null ? "" : component.trim();
                switch (i) {
                    case 0:
                        tmpTopic = value;
                        break;
                    case 1:
                        tmpPayload = value.getBytes(StandardCharsets.UTF_8);
                        break;
                    case 2:
                        if (!"".equals(value)) {
                            int tmp = Integer.valueOf(value);
                            if (tmp >= 0 && tmp <= 2) {
                                tmpQos = tmp;
                            }
                        }
                        break;
                    case 3:
                        tmpRetain = Boolean.valueOf(value);
                        break;
                }
            }
        }

        // Use explicit given values.
        if (topic != null) {
            tmpTopic = topic;
        }
        if (payload != null) {
            tmpPayload = payload;
        }
        if (qos != null) {
            tmpQos = qos;
        }
        if (retain != null) {
            tmpRetain = retain;
        }

        // Check if valid
        if (tmpTopic == null || tmpTopic.isEmpty()) {
            return null;
        }

        // Create MQTT Last Will and Testament object
        return new MqttWillAndTestament(tmpTopic, tmpPayload, tmpQos, tmpRetain);
    }

    /**
     * Create a new {@link} MqttWillAndTestament with at least a topic name.
     *
     * @param topic topic is a normal topic string (no placeholders are allowed)
     * @param payload The optional payload. Can be null.
     * @param qos Valid values are 0 (Deliver at most once),1 (Deliver at least once) or 2</li>
     * @param retain true if messages shall be retained
     */
    public MqttWillAndTestament(String topic, byte @Nullable [] payload, int qos, boolean retain) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Topic must be set");
        }
        this.topic = topic;
        this.payload = payload;
        this.qos = qos;
        this.retain = retain;
    }

    /**
     * @return the topic for the last will.
     */
    public String getTopic() {
        return topic;
    }

    /**
     * @return the payload of the last will.
     */
    public byte @Nullable [] getPayload() {
        return payload;
    }

    /**
     * @return quality of service level.
     */
    public int getQos() {
        return qos;
    }

    /**
     * @return true if the last will should be retained by the broker.
     */
    public boolean isRetain() {
        return retain;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(getClass());
        sb.append("] Send '");
        if (payload != null) {
            sb.append(new String(payload));
        } else {
            sb.append(payload);
        }
        sb.append("' to topic '");
        sb.append(topic);
        sb.append("'");
        if (retain) {
            sb.append(" retained");
        }
        sb.append(" using qos mode ").append(qos);
        return sb.toString();
    }
}
