/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.core.io.console.internal.extension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.ManagedUser;
import org.openhab.core.auth.User;
import org.openhab.core.auth.UserApiToken;
import org.openhab.core.auth.UserRegistry;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension to manage users, sessions and API tokens
 *
 * @author Yannick Schaus - Initial contribution
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class UserConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_ADD = "add";
    private static final String SUBCMD_REMOVE = "remove";
    private static final String SUBCMD_CHANGEROLE = "changeRole";
    private static final String SUBCMD_LISTROLES = "listRoles";
    private static final String SUBCMD_ADDROLE = "addRole";
    private static final String SUBCMD_REMOVEROLE = "removeRole";
    private static final String SUBCMD_CHANGEPASSWORD = "changePassword";
    private static final String SUBCMD_LISTAPITOKENS = "listApiTokens";
    private static final String SUBCMD_ADDAPITOKEN = "addApiToken";
    private static final String SUBCMD_RMAPITOKEN = "rmApiToken";
    private static final String SUBCMD_CLEARSESSIONS = "clearSessions";

    private final UserRegistry userRegistry;

    @Activate
    public UserConsoleCommandExtension(final @Reference UserRegistry userRegistry) {
        super("users", "Access the user registry.");
        this.userRegistry = userRegistry;
    }

    @Override
    public List<String> getUsages() {
        return List.of(buildCommandUsage(SUBCMD_LIST, "lists all users"),
                buildCommandUsage(SUBCMD_ADD + " <userId> <password> <role>",
                        "adds a new user with the specified role"),
                buildCommandUsage(SUBCMD_REMOVE + " <userId>", "removes the given user"),
                buildCommandUsage(SUBCMD_LISTROLES + " <userId>", "list the roles of the userID"),
                buildCommandUsage(SUBCMD_CHANGEROLE + " <userId> <oldRole> <newRole>",
                        "Change the specific role of a user with a new one"),
                buildCommandUsage(SUBCMD_ADDROLE + " <userId> <role>", "Add the specified role to the specified user"),
                buildCommandUsage(SUBCMD_REMOVEROLE + " <userId> <role>", "Remove the specified role of the user"),
                buildCommandUsage(SUBCMD_CHANGEPASSWORD + " <userId> <newPassword>", "changes the password of a user"),
                buildCommandUsage(SUBCMD_LISTAPITOKENS, "lists the API tokens for all users"),
                buildCommandUsage(SUBCMD_ADDAPITOKEN + " <userId> <tokenName> <scope>",
                        "adds a new API token on behalf of the specified user for the specified scope"),
                buildCommandUsage(SUBCMD_RMAPITOKEN + " <userId> <tokenName>",
                        "removes (revokes) the specified API token"),
                buildCommandUsage(SUBCMD_CLEARSESSIONS + " <userId>",
                        "clear the refresh tokens associated with the user (will sign the user out of all sessions)"));
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_LIST:
                    userRegistry.getAll().forEach(user -> console.println(user.toString()));
                    break;
                case SUBCMD_ADD:
                    if (args.length == 4) {
                        User existingUser = userRegistry.get(args[1]);
                        if (existingUser == null) {
                            User newUser = userRegistry.register(args[1], args[2], Set.of(args[3]));
                            console.println(newUser.toString());
                            console.println("User created.");
                        } else {
                            console.println("The user already exists.");
                        }
                    } else {
                        console.printUsage(findUsage(SUBCMD_ADD));
                    }
                    break;
                case SUBCMD_REMOVE:
                    if (args.length == 2) {
                        User user = userRegistry.get(args[1]);
                        if (user != null) {
                            userRegistry.remove(user.getName());
                            console.println("User removed.");
                        } else {
                            console.println("User not found.");
                        }
                    } else {
                        console.printUsage(findUsage(SUBCMD_REMOVE));
                    }
                    break;
                case SUBCMD_LISTROLES:
                    if (args.length == 1) {
                        Collection<User> usersRegistry = userRegistry.getAll();
                        for (User user : usersRegistry) {
                            Set<String> roles = user.getRoles();
                            String out = "";
                            if (roles.size() == 1) {
                                out = "The username " + user.toString() + " has the role: ";
                                for (String role : roles) {
                                    out = out + role;
                                }
                            } else {
                                out = "The username " + user.toString() + " has these roles: - ";
                                for (String role : roles) {
                                    out = out + role + " - ";
                                }
                            }
                            console.println(out);
                        }
                    } else {
                        console.printUsage(findUsage(SUBCMD_LISTROLES));
                    }
                    break;
                case SUBCMD_CHANGEROLE:
                    /*
                     * if (args.length == 4) {
                     * User existingUser = userRegistry.get(args[1]);
                     * if (existingUser == null) {
                     * console.println("The user doesn't exist here you can find the available users:");
                     * userRegistry.getAll().forEach(user -> console.println(user.toString()));
                     * return;
                     * } else {
                     * try {
                     * if (args[2].equals("administrator") || args[3].equals("administrator")) {
                     * if (checkAdministratorCredential(console)) {
                     * userRegistry.changeRole(existingUser, args[2], args[3]);
                     * console.println("The role (" + args[2] + ") of the user " + args[1]
                     * + " has been changed to the role (" + args[3] + ")");
                     * }
                     * } else {
                     * userRegistry.changeRole(existingUser, args[2], args[3]);
                     * console.println("The role (" + args[2] + ") of the user " + args[1]
                     * + " has been changed to the role (" + args[3] + ")");
                     * }
                     * } catch (IllegalArgumentException ie) {
                     * logger.warn("IllegalArgumentException: ", ie);
                     * }
                     * }
                     * } else {
                     * console.printUsage(findUsage(SUBCMD_CHANGEROLE));
                     * }
                     */

                    break;

                case SUBCMD_ADDROLE:
                    if (args.length == 3) {
                        User existingUser = userRegistry.get(args[1]);
                        if (existingUser == null) {
                            console.println("The user doesn't exist here you can find the available users:");
                            userRegistry.getAll().forEach(user -> console.println(user.toString()));
                            return;
                        } else {
                            if (userRegistry.addRole(existingUser, args[2])) {
                                console.println(
                                        "The role (" + args[2] + ") of the user " + args[1] + " has been added.");
                            } else {
                                console.println(
                                        "The role (" + args[2] + ") of the user " + args[2] + " already exist.");
                            }
                        }
                    } else {
                        console.printUsage(findUsage(SUBCMD_ADDROLE));
                    }
                    break;
                case SUBCMD_REMOVEROLE:
                    if (args.length == 3) {
                        User existingUser = userRegistry.get(args[1]);
                        if (existingUser == null) {
                            console.println("The user doesn't exist here you can find the available users:");
                            userRegistry.getAll().forEach(user -> console.println(user.toString()));
                            return;
                        } else {
                            if (userRegistry.removeRole(existingUser, args[2])) {
                                console.println(
                                        "The role (" + args[2] + ") of the user " + args[1] + " has been removed.");
                            } else {
                                console.println(
                                        "The role (" + args[2] + ") of the user " + args[2] + " doesn't exist.");
                            }
                        }
                    } else {
                        console.printUsage(findUsage(SUBCMD_REMOVEROLE));
                    }
                    break;
                case SUBCMD_CHANGEPASSWORD:
                    if (args.length == 3) {
                        User user = userRegistry.get(args[1]);
                        if (user != null) {
                            userRegistry.changePassword(user, args[2]);
                            console.println("Password changed.");
                        } else {
                            console.println("User not found.");
                        }
                    } else {
                        console.printUsage(findUsage(SUBCMD_CHANGEPASSWORD));
                    }
                    break;
                case SUBCMD_LISTAPITOKENS:
                    userRegistry.getAll().forEach(user -> {
                        ManagedUser managedUser = (ManagedUser) user;
                        if (!managedUser.getApiTokens().isEmpty()) {
                            managedUser.getApiTokens()
                                    .forEach(t -> console.println("user=" + user.toString() + ", " + t.toString()));
                        }
                    });
                    break;
                case SUBCMD_ADDAPITOKEN:
                    if (args.length == 4) {
                        ManagedUser user = (ManagedUser) userRegistry.get(args[1]);
                        if (user != null) {
                            Optional<UserApiToken> userApiToken = user.getApiTokens().stream()
                                    .filter(t -> args[2].equals(t.getName())).findAny();
                            if (userApiToken.isEmpty()) {
                                String tokenString = userRegistry.addUserApiToken(user, args[2], args[3]);
                                console.println(tokenString);
                            } else {
                                console.println("Cannot create API token: another one with the same name was found.");
                            }
                        } else {
                            console.println("User not found.");
                        }
                    } else {
                        console.printUsage(findUsage(SUBCMD_ADDAPITOKEN));
                    }
                    break;
                case SUBCMD_RMAPITOKEN:
                    if (args.length == 3) {
                        ManagedUser user = (ManagedUser) userRegistry.get(args[1]);
                        if (user != null) {
                            Optional<UserApiToken> userApiToken = user.getApiTokens().stream()
                                    .filter(t -> args[2].equals(t.getName())).findAny();
                            if (userApiToken.isPresent()) {
                                userRegistry.removeUserApiToken(user, userApiToken.get());
                                console.println("API token revoked.");
                            } else {
                                console.println("No matching API token found.");
                            }
                        } else {
                            console.println("User not found.");
                        }
                    } else {
                        console.printUsage(findUsage(SUBCMD_RMAPITOKEN));
                    }
                    break;
                case SUBCMD_CLEARSESSIONS:
                    if (args.length == 2) {
                        User user = userRegistry.get(args[1]);
                        if (user != null) {
                            userRegistry.clearSessions(user);
                            console.println("User sessions cleared.");
                        } else {
                            console.println("User not found.");
                        }
                    } else {
                        console.printUsage(findUsage(SUBCMD_CLEARSESSIONS));
                    }
                    break;
                default:
                    console.println("Unknown command '" + subCommand + "'");
                    printUsage(console);
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    /**
     * Ask for the credential of a user who has the role administrator and check if the credential is correct.
     *
     * @return return true if the credential of the user is correct and false otherwise.
     */
    private boolean checkAdministratorCredential(Console console) {
        /*
         * String WHITELIST = "A-Za-z";
         * String[] logArgs = null;
         * int in = 0;
         * Scanner scanner = new Scanner(System.in);
         * console.println(
         * "To manage the administrator role you have to run the command line: log <userId with administrator role> <password> or the command <exit> to quit"
         * );
         * String scanArgs = scanner.nextLine();
         * while (scanner.hasNext()) {
         * // check if the command contains only letter of the alphabet.
         * Pattern p = Pattern.compile(WHITELIST);
         * Matcher m = p.matcher(scanArgs);
         * if (m.find()) {
         * console.println(
         * "The input contains invalid characters, please run the command: log <userId with administrator role> <password> or the command <exit> to quit"
         * );
         * } else {
         * logArgs = scanArgs.split(" ");
         * if (logArgs.length == 3 || logArgs.length == 1) {
         * console.println(
         * "Invalid input, please run the command: log <userId with administrator role> <password> or the command <exit> to quit"
         * );
         * } else {
         * if (logArgs[0].equals("log")) {
         * User adminUser = userRegistry.get(logArgs[1]);
         * if (adminUser == null) {
         * console.println("the user " + logArgs[1] + " does not exist");
         * } else {
         * if (userRegistry.checkAdministratorCredential(adminUser, logArgs[2])) {
         * return true;
         * } else {
         * console.println("The password of the user " + logArgs[1]
         * + " is not correct. You can write the command <exit> to quit");
         * }
         * }
         * } else if (logArgs[0].equals("exit")) {
         * return false;
         * } else {
         * console.println(
         * "Invalid input, please run the command: log <userId with administrator role> <password> or the command <exit> to quit"
         * );
         * }
         * }
         * }
         * scanArgs = scanner.nextLine();
         * }
         * return false;
         */
        return true;
    }

    private String findUsage(String cmd) {
        return getUsages().stream().filter(u -> u.contains(cmd)).findAny().get();
    }
}
