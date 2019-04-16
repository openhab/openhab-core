/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.io.rest.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.io.rest.LocaleService;
import org.eclipse.smarthome.io.rest.RESTService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsName;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers a Jax-RS application as service with the configured root path. Also provides {@link LocaleService} for REST
 * producers.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = { RESTService.class }, configurationPid = { "smarthome.rest.root",
        "org.eclipse.smarthome.io.rest.root" })
public class RESTServiceImpl implements LocaleService, RESTService {
    @Reference
    private @NonNullByDefault({}) LocaleProvider localeProvider;

    private @NonNullByDefault({}) Config config;

    private final Logger logger = LoggerFactory.getLogger(RESTapplication.class);

    private @NonNullByDefault({}) ServiceRegistration<RESTapplication> restService;

    @ObjectClassDefinition(description = "%cors_description", name = "%cors_name")
    @interface Config {
        String root() default "/rest";
    }

    /**
     * The Jax-RS application that starts up all openHAB REST activities.
     * It registers itself as a Jax-RS Whiteboard service and all Jax-RS resources that are targeting REST_APP_NAME will
     * start up.
     */
    @JaxrsName(REST_APP_NAME)
    private static class RESTapplication extends Application {
        private String root;

        RESTapplication(String root) {
            this.root = root;
        }

        @NonNullByDefault({})
        @Override
        public Map<String, Object> getProperties() {
            Map<String, Object> p = new TreeMap<>();
            p.put("jersey.config.server.disableMetainfServicesLookup", true);
            p.put("jersey.config.server.disableAutoDiscovery", true);
            return p;
        }

        Dictionary<String, String> serviceProperties() {
            Dictionary<String, String> dict = new Hashtable<>();
            dict.put(JaxrsWhiteboardConstants.JAX_RS_APPLICATION_BASE, root);
            return dict;
        }
    }

    @Activate
    protected void activate(BundleContext context, Config config) {
        this.config = config;
        RESTapplication app = new RESTapplication(config.root());
        restService = context.registerService(RESTapplication.class, app, app.serviceProperties());
        logger.info("openHAB REST started on {}", config.root());
    }

    @Deactivate
    protected void deactivate() {
        if (restService != null) {
            restService.unregister();
            restService = null;
        }
    }

    @Override
    public String getRESTroot() {
        return config.root();
    }

    @Override
    public Locale getLocale(@Nullable String acceptLanguageHttpHeader) {
        Locale locale = localeProvider.getLocale();
        if (acceptLanguageHttpHeader != null) {
            int pos = acceptLanguageHttpHeader.indexOf(',');
            String[] split;
            if (pos > -1) {
                split = acceptLanguageHttpHeader.substring(0, pos).split("-");
            } else {
                split = acceptLanguageHttpHeader.split("-");
            }
            if (split.length == 2) {
                locale = new Locale(split[0], split[1]);
            } else {
                locale = new Locale(split[0]);
            }
        }
        return locale;
    }

}
