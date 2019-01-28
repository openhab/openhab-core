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
package org.eclipse.smarthome.io.rest.sse.beans;

/**
 * Event bean for broadcasted events.
 *
 * @author Ivan Iliev - Initial Contribution and API
 * @author Dennis Nobel - Added event type and renamed object to payload
 */
public class EventBean {

    public String topic;

    public String payload;
    
    public String type;

}
