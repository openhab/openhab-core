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
package org.openhab.core.auth.oauth2client.test.internal.console;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.openhab.core.auth.client.oauth2.AccessTokenResponse;
import org.openhab.core.auth.client.oauth2.OAuthClientService;
import org.openhab.core.auth.client.oauth2.OAuthException;
import org.openhab.core.auth.client.oauth2.OAuthResponseException;
import org.openhab.core.auth.oauth2client.test.internal.AbstractTestAgent;
import org.openhab.core.auth.oauth2client.test.internal.AuthorizationCodeTestAgent;
import org.openhab.core.auth.oauth2client.test.internal.ResourceOwnerTestAgent;
import org.openhab.core.auth.oauth2client.test.internal.TestAgent;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Command for testing in the OSGI console
 *
 * @author Gary Tse - Initial contribution
 */
@Component(immediate = true, name = "ConsoleOAuthCommandExtension")
public class ConsoleOAuthCommandExtension extends AbstractConsoleCommandExtension implements ConsoleCommandExtension {

    public ConsoleOAuthCommandExtension() {
        super("oauth", "Test of oauth client with console. Available test agent: {Code, ResourceOwner}.\n"
                + "The commands in oauth requires oauth provider data to be inputted in configuration admin.");
    }

    private AuthorizationCodeTestAgent authorizationCodeTestAgent;
    private ResourceOwnerTestAgent resourceOwnerTestAgent;
    private Console console;

    @Override
    public void execute(String[] args, Console console) {
        this.console = console;

        if (args.length < 2) {
            console.println("Argument expected.  Please check usage.");
            return;
        }

        AbstractTestAgent agent = getTestAgent(args[0]);
        if (agent == null) {
            console.println("Unexpected test agent:" + args[0]);
            return;
        }

        AccessTokenResponse response;
        try {
            switch (args[1]) {
                case "create":
                    OAuthClientService newService = agent.testCreateClient();
                    console.println("handle: " + agent.handle + ", service: " + newService);
                    break;
                case "getAccessTokenByResourceOwnerPassword":
                    response = agent.testGetAccessTokenByResourceOwnerPasswordCredentials();
                    consolePrintAccessToken(response);
                    break;
                case "getClient":
                    OAuthClientService service = agent.testGetClient(args[2]);
                    console.println("OAuthClientService: " + service);
                    break;
                case "refresh":
                    response = agent.testRefreshToken();
                    consolePrintAccessToken(response);
                    break;
                case "getAccessTokenByCode":
                    console.println("using authorization code: " + args[2]);
                    response = agent.testGetAccessTokenByAuthorizationCode(args[2]);
                    consolePrintAccessToken(response);
                    break;
                case "getAuthorizationUrl":
                    String authURL;
                    if (args.length >= 3) {
                        authURL = agent.testGetAuthorizationUrl(args[2]);
                        console.println("Authorization URL: " + authURL + " state: " + args[2]);
                    } else {
                        authURL = agent.testGetAuthorizationUrl(null);
                        console.println("Authorization URL: " + authURL + " state: null");
                    }
                    break;
                case "getCachedAccessToken":
                    response = agent.testGetCachedAccessToken();
                    consolePrintAccessToken(response);
                    break;
                case "close":
                    console.println("Closing test agent client service...");
                    agent.close();
                    break;
                case "delete":
                    console.println("Delete by handle: " + args[2]);
                    agent.delete(args[2]);
                    break;
                default:
                    console.println("Commands are case-sensitive.  Unknown command: " + args[1]);
                    break;
            }
        } catch (OAuthException | IOException | OAuthResponseException e) {
            console.print(String.format("%s %s, cause %s", e.getClass(), e.getMessage(), e.getCause()));
        }
    }

    private AbstractTestAgent getTestAgent(String name) {
        if ("Code".equals(name)) {
            return authorizationCodeTestAgent;
        } else if ("ResourceOwner".equals(name)) {
            return resourceOwnerTestAgent;
        } else {
            return null;
        }
    }

    private void consolePrintAccessToken(AccessTokenResponse response) {
        console.println("AccessTokenResponse: " + (response == null ? "null" : response.toString()));
    }

    /**
     * @formatter:off
     */
    @Override
    public List<String> getUsages() {
        return Arrays.asList(
                buildCommandUsage("<agent> create","create an oauth client. Agent:={Code | ResourceOwner}"),
                buildCommandUsage("<agent> getClient <handle>", "get the client by handle"),
                buildCommandUsage("<agent> getAccessTokenByResourceOwnerPassword","get access token by resource owner username and password"),
                buildCommandUsage("<agent> getAuthorizationUrl", "get authorization url"),
                buildCommandUsage("<agent> getAccessTokenByCode <code>","get the access token by authorization code"),
                buildCommandUsage("<agent> getCachedAccessToken", "get the access token from cache"),
                buildCommandUsage("<agent> refresh", "refresh access token"),
                buildCommandUsage("<agent> delete <handle>", "delete service, access token, configurations by the handle"),
                buildCommandUsage("<agent> close", "close the test agent and release resources, tokens are persisted"));
    }

    @Reference(target = "(service.pid=ResourceOwnerTestAgent)")
    protected void setResourceOwnerTestAgent(TestAgent resourceOwnerTestAgent) {
        this.resourceOwnerTestAgent = (ResourceOwnerTestAgent)resourceOwnerTestAgent;
    }

    protected void unsetResourceOwnerTestAgent(TestAgent resourceOwnerTestAgent) {
        this.resourceOwnerTestAgent = null;
    }

    @Reference(target = "(service.pid=AuthorizationCodeTestAgent)")
    protected void setAuthorizationCodeTestAgent(TestAgent authorizationCodeTestAgent) {
        this.authorizationCodeTestAgent = (AuthorizationCodeTestAgent)authorizationCodeTestAgent;
    }

    protected void unsetAuthorizationCodeTestAgent(TestAgent authorizationCodeTestAgent) {
        this.authorizationCodeTestAgent = null;
    }

}
