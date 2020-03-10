package org.openhab.core.io.http.auth.internal;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.auth.Authentication;
import org.openhab.core.auth.AuthenticationException;
import org.openhab.core.auth.AuthenticationProvider;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.PendingToken;
import org.openhab.core.auth.Role;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.auth.UsernamePasswordCredentials;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jdk.internal.joptsimple.internal.Strings;

@NonNullByDefault
@Component(immediate = true)
public class AuthorizePageServlet extends HttpServlet {

    private static final long serialVersionUID = 5340598701104679843L;

    private final Logger logger = LoggerFactory.getLogger(AuthorizePageServlet.class);

    private HashSet<String> csrfTokens = new HashSet<>();

    private HttpService httpService;
    private UserRegistry userRegistry;
    private AuthenticationProvider authProvider;

    private String pageTemplate;

    @Activate
    public AuthorizePageServlet(BundleContext bundleContext, @Reference HttpService httpService,
            @Reference UserRegistry userRegistry, @Reference AuthenticationProvider authProvider) {
        this.httpService = httpService;
        this.userRegistry = userRegistry;
        this.authProvider = authProvider;

        pageTemplate = "";
        try {
            URL resource = bundleContext.getBundle().getResource("pages/authorize.html");
            if (resource != null) {
                try {
                    pageTemplate = IOUtils.toString(resource.openStream());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                httpService.registerServlet("/auth", this, null, null);
            }
        } catch (NamespaceException | ServletException e) {
            logger.error("Error during authorization page registration: {}", e.getMessage());
        }
    }

    @Override
    protected void doGet(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp)
            throws ServletException, IOException {
        if (req != null && resp != null) {
            Map<String, String[]> params = req.getParameterMap();

            try {
                String message = Strings.EMPTY;
                if (userRegistry.getAll().size() == 0) {
                    message = "Create a first administrator account to continue.";
                }
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().append(getPageBody(params, message));
                resp.getWriter().close();
            } catch (Exception e) {
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().append(e.getMessage());
                resp.getWriter().close();
            }
        }
    }

    @Override
    protected void doPost(@Nullable HttpServletRequest req, @Nullable HttpServletResponse resp)
            throws ServletException, IOException {
        if (req != null && resp != null) {
            Map<String, String[]> params = req.getParameterMap();
            try {
                if (!params.containsKey(("username"))) {
                    throw new AuthenticationException("no username");
                }
                if (!params.containsKey(("username"))) {
                    throw new AuthenticationException("no password");
                }
                if (!params.containsKey("csrf_token") || !csrfTokens.contains(params.get("csrf_token")[0])) {
                    throw new AuthenticationException("CSRF check failed");
                }
                if (!params.containsKey(("redirect_uri"))) {
                    throw new IllegalArgumentException("invalid_request");
                }
                if (!params.containsKey(("response_type"))) {
                    throw new IllegalArgumentException("unsupported_response_type");
                }
                if (!params.containsKey(("client_id"))) {
                    throw new IllegalArgumentException("unauthorized_client");
                }
                if (!params.containsKey(("scope"))) {
                    throw new IllegalArgumentException("invalid_scope");
                }
                String baseRedirectUri = params.get("redirect_uri")[0];
                String responseType = params.get("response_type")[0];
                String clientId = params.get("redirect_uri")[0];
                String scope = params.get("scope")[0];

                if (!("code".equals(responseType))) {
                    throw new AuthenticationException("unsupported_response_type");
                }

                if (!clientId.equals(baseRedirectUri)) {
                    throw new IllegalArgumentException("unauthorized_client");
                }

                String username = params.get("username")[0];
                String password = params.get("password")[0];

                User user;
                if (userRegistry.getAll().size() == 0) {
                    // Create a first administrator account with the supplied credentials
                    user = userRegistry.register(username, password, Set.of(Role.ADMIN));
                    logger.info("First user account created: {}", username);
                } else {
                    // Authenticate the user with the supplied credentials
                    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
                    Authentication auth = authProvider.authenticate(credentials);
                    logger.debug("Login successful: {}", auth.getUsername());
                    user = userRegistry.get(auth.getUsername());
                }

                String authorizationCode = UUID.randomUUID().toString().replace("-", "");

                if (user instanceof ManagedUser) {
                    ManagedUser managedUser = (ManagedUser) user;
                    PendingToken pendingToken = new PendingToken(authorizationCode, clientId, scope);
                    managedUser.setPendingToken(pendingToken);
                    userRegistry.update(managedUser);
                }

                String state = params.containsKey("state") ? params.get("state")[0] : null;
                resp.addHeader(HttpHeaders.LOCATION, getRedirectUri(baseRedirectUri, authorizationCode, null, state));
                resp.setStatus(HttpStatus.SC_MOVED_TEMPORARILY);

            } catch (AuthenticationException e) {
                resp.setContentType("text/html;charset=UTF-8");
                logger.warn("Authentication failed: ", e.getMessage());
                resp.getWriter().append(getPageBody(params, "Please try again")); // TODO: i18n
                resp.getWriter().close();
            } catch (IllegalArgumentException e) {
                @Nullable
                String baseRedirectUri = params.containsKey("redirect_uri") ? params.get("redirect_uri")[0] : null;
                @Nullable
                String state = params.containsKey("state") ? params.get("state")[0] : null;
                if (baseRedirectUri != null) {
                    resp.addHeader(HttpHeaders.LOCATION, getRedirectUri(baseRedirectUri, null, e.getMessage(), state));
                    resp.setStatus(HttpStatus.SC_MOVED_TEMPORARILY);
                } else {
                    resp.setContentType("text/plain;charset=UTF-8");
                    resp.getWriter().append(e.getMessage());
                    resp.getWriter().close();
                }
            }
        }
    }

    private String getPageBody(Map<String, String[]> params, String message) {
        String responseBody = pageTemplate.replace("{form_fields}", getFormFields(params));
        responseBody = responseBody.replace("{message}", message);
        return responseBody;
    }

    private String getFormFields(Map<String, String[]> params) {
        String hiddenFormFields = "";

        if (!params.containsKey(("redirect_uri"))) {
            throw new IllegalArgumentException("invalid_request");
        }
        if (!params.containsKey(("response_type"))) {
            throw new IllegalArgumentException("unsupported_response_type");
        }
        if (!params.containsKey(("client_id"))) {
            throw new IllegalArgumentException("unauthorized_client");
        }
        if (!params.containsKey(("scope"))) {
            throw new IllegalArgumentException("invalid_scope");
        }
        String csrfToken = addCsrfToken();
        String redirectUri = params.get("redirect_uri")[0];
        String responseType = params.get("response_type")[0];
        String clientId = params.get("client_id")[0];
        String scope = params.get("scope")[0];
        @Nullable
        String state = (params.containsKey("state")) ? params.get("state")[0] : null;
        hiddenFormFields += "<input type=\"hidden\" name=\"csrf_token\" value=\"" + csrfToken + "\">";
        hiddenFormFields += "<input type=\"hidden\" name=\"redirect_uri\" value=\"" + redirectUri + "\">";
        hiddenFormFields += "<input type=\"hidden\" name=\"response_type\" value=\"" + responseType + "\">";
        hiddenFormFields += "<input type=\"hidden\" name=\"client_id\" value=\"" + clientId + "\">";
        hiddenFormFields += "<input type=\"hidden\" name=\"scope\" value=\"" + scope + "\">";
        if (state != null) {
            hiddenFormFields += "<input type=\"hidden\" name=\"state\" value=\"" + state + "\">";
        }

        return hiddenFormFields;
    }

    private String getRedirectUri(String baseRedirectUri, @Nullable String authorizationCode, @Nullable String error,
            @Nullable String state) {
        String redirectUri = baseRedirectUri;

        if (authorizationCode != null) {
            redirectUri += "?code=" + authorizationCode;
        } else if (error != null) {
            redirectUri += "?error=" + error;
        }

        if (state != null) {
            redirectUri += "&state=" + state;
        }

        return redirectUri;
    }

    private String addCsrfToken() {
        String csrfToken = UUID.randomUUID().toString().replace("-", "");
        csrfTokens.add(csrfToken);
        return csrfToken;
    }

    @Deactivate
    public void deactivate() {
        httpService.unregister("/auth");
    }
}
