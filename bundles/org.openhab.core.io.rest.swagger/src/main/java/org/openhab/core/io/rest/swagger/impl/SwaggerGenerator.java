package org.openhab.core.io.rest.swagger.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openhab.core.io.rest.internal.resources.beans.RootBean;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.maggu2810.jaxrswb.gen.JaxRsWhiteboardGeneratorConfig;
import de.maggu2810.jaxrswb.swagger1.gen.JaxRsWhiteboardSwaggerSpecialGenerator;
import de.maggu2810.jaxrswb.utils.JaxRsHelper;
import io.swagger.jaxrs.Reader;
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.Parameter;
import io.swagger.util.Json;

/**
 * An JAX-RS Whiteboard Swagger 1 generator implementation.
 *
 *
 * @author Markus Rathgeb - Initial contribution
 * @author Yannick Schaus - adapt to provide OAuth2 security definitions
 */
@Component(service = SwaggerGenerator.class)
public class SwaggerGenerator implements JaxRsWhiteboardSwaggerSpecialGenerator {

    public static final String API_TITLE = "openHAB REST API";
    public static final String API_VERSION = new RootBean().version;
    public static final String OAUTH_AUTHORIZE_ENDPOINT = "/auth/authorize";
    public static final String OAUTH_TOKEN_ENDPOINT = "/rest/auth/token";

    @SuppressWarnings("all")
    private static final TypeReference<Map<String, Object>> TYPEREF_MAP_STR_OBJ = new TypeReference<Map<String, Object>>() {
    };

    private static class SwaggerReader extends Reader {
        public SwaggerReader(final Swagger swagger) {
            super(swagger);
        }

        @Override
        public Swagger read(final Class<?> cls, final String parentPath, final String parentMethod,
                final boolean isSubresource, final String[] parentConsumes, final String[] parentProduces,
                final Map<String, Tag> parentTags, final List<Parameter> parentParameters) {
            return super.read(cls, parentPath, parentMethod, isSubresource, parentConsumes, parentProduces, parentTags,
                    parentParameters);
        }
    }

    private final BundleContext bc;
    private final JaxRsWhiteboardGeneratorConfig config;

    /**
     * Creates a new instance.
     *
     * @param bc the bundle context
     * @param config the configuration
     */
    @Activate
    public SwaggerGenerator(final BundleContext bc) {
        this.bc = bc;
        this.config = null;
    }

    @Override
    public Swagger generate() {
        final Swagger swagger = new Swagger();

        Info info = new Info();
        info.setTitle(API_TITLE);
        info.setVersion(API_VERSION);
        swagger.setInfo(info);

        // Add OAuth2 security definition
        SecuritySchemeDefinition oauth2Definition = new OAuth2Definition()
                .accessCode(OAUTH_AUTHORIZE_ENDPOINT, OAUTH_TOKEN_ENDPOINT).scope("admin", "Administration operations");
        Map<String, SecuritySchemeDefinition> securityDefinitions = new HashMap<>();
        securityDefinitions.put("oauth2", oauth2Definition);
        swagger.setSecurityDefinitions(securityDefinitions);

        final SwaggerReader reader = new SwaggerReader(swagger);

        final JaxRsHelper jaxRsHelper = new JaxRsHelper(bc);
        final Map<String, Set<Class<?>>> basePathAndClasses = jaxRsHelper.getBasePathAndClasses();
        basePathAndClasses.forEach((basePath, classes) -> {
            classes.forEach(clazz -> {
                final String parentPath = basePath.startsWith("/") ? basePath : "/" + basePath;
                reader.read(clazz, parentPath, null, false, new String[0], new String[0],
                        new LinkedHashMap<String, Tag>(), new ArrayList<Parameter>());
            });
        });

        return reader.getSwagger();
    }

    @Override
    public String toJSON(final Swagger info) throws IOException {
        final ObjectMapper mapper = Json.mapper();
        return mapper.writeValueAsString(info);
    }

    @Override
    public Map<String, Object> toMap(final Swagger info) throws IOException {
        final ObjectMapper mapper = Json.mapper();
        final String jsonString = mapper.writeValueAsString(info);
        return mapper.readValue(jsonString, TYPEREF_MAP_STR_OBJ);
    }
}
