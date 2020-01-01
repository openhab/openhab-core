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
package org.openhab.core.io.rest.optimize.internal;

import static com.eclipsesource.jaxrs.publisher.ServiceProperties.PUBLISH;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.jaxrs.publisher.ResourceFilter;

/**
 * Provides a filter for all classes/interfaces which are relevant in the context of JAX-RS.
 *
 * By default, this filter will allow every service outside of the org.openhab.core.**-Namespace to be parsed by
 * the JAXR-RS implementation. To further optimize this, install a fragment which adds a "/res/whitelist.txt" file,
 * containing one service interface or class per line like in the following example:
 *
 * <pre>
 * {@code
 * # My Custom Services
 * org.example.foo
 * org.example.bar
 * # Another one
 * org.example.test
 * }
 * </pre>
 *
 * If this file is present, no other services will be scanned and hence won't be available.
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class ResourceFilterImpl implements ResourceFilter {

    private final Logger logger = LoggerFactory.getLogger(ResourceFilterImpl.class);

    /**
     * All classes and interfaces which are considered to be relevant for JAX-RS.
     */
    private static final String[] WHITELIST = new String[] {
            // JAX-RS
            "javax.ws.rs.ext.MessageBodyReader", "javax.ws.rs.ext.MessageBodyWriter",
            // openHAB
            "org.openhab.core.io.rest.internal.filter.ProxyFilter",
            "org.openhab.core.io.rest.internal.resources.RootResource",
            "org.openhab.core.io.rest.JSONResponse$ExceptionMapper", "org.openhab.core.io.rest.RESTResource",
            "org.openhab.core.io.rest.sse.internal.async.BlockingAsyncFeature",
            "org.openhab.core.io.rest.sse.SseResource",
            // SSE
            "org.glassfish.jersey.media.sse.SseFeature",
            "org.glassfish.jersey.server.monitoring.ApplicationEventListener" };

    @Override
    public Filter getFilter() {
        String filterString = createFilter(WHITELIST);
        try {
            return FrameworkUtil.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            logger.error("Error creating RESTResource filter", e);
        }
        return null;
    }

    /**
     * @param interfaces interface or class names
     * @return filter string which matches if the class implements one of the interfaces or the name of the class is
     *         contained in interfaces
     */
    private String createFilter(String[] interfaces) {
        StringBuilder builder = new StringBuilder();
        builder.append("(&");
        builder.append("(|");
        List<String> whitelist = loadWhitelistExtension();
        if (whitelist == null) {
            logger.debug("No /res/whitelist.txt file found - scanning all unknown services");
            builder.append("(!(" + Constants.OBJECTCLASS + "=org.openhab.core.*))");
        } else {
            logger.debug("Whitelist /res/whitelist.txt file found - restricting scanning of services");
            whitelist.forEach(entry -> {
                builder.append("(" + Constants.OBJECTCLASS + "=" + entry + ")");
            });
        }
        for (String clazz : interfaces) {
            builder.append("(" + Constants.OBJECTCLASS + "=" + clazz + ")");
        }
        builder.append(")");
        builder.append("(!(" + PUBLISH + "=false)))");
        return builder.toString();
    }

    private List<String> loadWhitelistExtension() {
        Enumeration<URL> entries = FrameworkUtil.getBundle(this.getClass()).findEntries("res", "whitelist.txt", false);
        if (entries != null && entries.hasMoreElements()) {
            URL url = entries.nextElement();
            try (InputStream is = url.openStream()) {
                return readWhitelistEntries(is);
            } catch (IOException e) {
                logger.warn("Error reading REST extension whitelist from {}", url, e);
                return null;
            }
        } else {
            return null;
        }
    }

    private List<String> readWhitelistEntries(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        List<String> ret = new LinkedList<>();
        String line = reader.readLine();
        while (line != null) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                ret.add(trimmed);
            }
            line = reader.readLine();
        }
        return ret;
    }

}
