/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.auth.Action;
import org.openhab.core.auth.Actions;
import org.openhab.core.auth.Permission;
import org.openhab.core.auth.ResourceType;
import org.openhab.core.auth.RoleDefinition;
import org.openhab.core.auth.RoleRegistry;
import org.openhab.core.auth.Selector;
import org.openhab.core.auth.SelectorParser;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Console command extension to manage roles and permissions.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@Component(service = ConsoleCommandExtension.class)
@NonNullByDefault
public class RoleConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_LIST = "list";
    private static final String SUBCMD_ADD = "add";
    private static final String SUBCMD_REMOVE = "remove";
    private static final String SUBCMD_ADDPERMISSION = "addPermission";
    private static final String SUBCMD_REMOVEPERMISSION = "removePermission";
    private static final String SUBCMD_SHOW = "show";

    private final RoleRegistry roleRegistry;

    @Activate
    public RoleConsoleCommandExtension(final @Reference RoleRegistry roleRegistry) {
        super("roles", "Manage roles and permissions.");
        this.roleRegistry = roleRegistry;
    }

    @Override
    public List<String> getUsages() {
        return List.of(buildCommandUsage(SUBCMD_LIST, "lists all roles with their permissions"),
                buildCommandUsage(SUBCMD_ADD + " <name> [description]", "creates a custom role"),
                buildCommandUsage(SUBCMD_REMOVE + " <name>", "removes a custom role"),
                buildCommandUsage(SUBCMD_ADDPERMISSION + " <role> <resourceType> <selector> <action>",
                        "adds a permission to a role"),
                buildCommandUsage(SUBCMD_REMOVEPERMISSION + " <role> <resourceType> <selector> <action>",
                        "removes a permission from a role"),
                buildCommandUsage(SUBCMD_SHOW + " <role>", "shows role details with all permissions"));
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            String subCommand = args[0];
            switch (subCommand) {
                case SUBCMD_LIST:
                    listRoles(console);
                    break;
                case SUBCMD_ADD:
                    addRole(args, console);
                    break;
                case SUBCMD_REMOVE:
                    removeRole(args, console);
                    break;
                case SUBCMD_ADDPERMISSION:
                    addPermission(args, console);
                    break;
                case SUBCMD_REMOVEPERMISSION:
                    removePermission(args, console);
                    break;
                case SUBCMD_SHOW:
                    showRole(args, console);
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

    private void listRoles(Console console) {
        for (RoleDefinition role : roleRegistry.getAll()) {
            printRoleWithPermissions(role, console);
        }
    }

    private void addRole(String[] args, Console console) {
        if (args.length >= 2) {
            String roleName = args[1];
            String description = args.length >= 3 ? args[2] : "";
            RoleDefinition newRole = new RoleDefinition(roleName, description, Set.of(), false);
            try {
                roleRegistry.add(newRole);
                console.println("Role '" + roleName + "' created.");
            } catch (IllegalArgumentException e) {
                console.println("A role with name '" + roleName + "' already exists.");
            }
        } else {
            console.printUsage(findUsage(SUBCMD_ADD));
        }
    }

    private void removeRole(String[] args, Console console) {
        if (args.length == 2) {
            String roleName = args[1];
            RoleDefinition role = roleRegistry.get(roleName);
            if (role == null) {
                console.println("Role not found.");
                return;
            }
            if (roleRegistry.isDefault(role)) {
                console.println("Cannot remove built-in role '" + roleName + "'.");
                return;
            }
            roleRegistry.remove(roleName);
            console.println("Role '" + roleName + "' removed.");
        } else {
            console.printUsage(findUsage(SUBCMD_REMOVE));
        }
    }

    private void addPermission(String[] args, Console console) {
        if (args.length == 5) {
            String roleName = args[1];
            RoleDefinition role = roleRegistry.get(roleName);
            if (role == null) {
                console.println("Role not found.");
                return;
            }
            if (!roleRegistry.isEditable(role)) {
                console.println("Cannot modify built-in role '" + roleName + "'.");
                return;
            }
            ResourceType type;
            try {
                type = ResourceType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                console.println("Unknown resource type: " + args[2] + ". Valid types: "
                        + Arrays.toString(ResourceType.values()));
                return;
            }
            Selector selector;
            try {
                selector = SelectorParser.parse(args[3]);
            } catch (IllegalArgumentException e) {
                console.println("Invalid selector: " + e.getMessage());
                return;
            }
            Action action;
            try {
                action = Actions.parse(type, args[4]);
            } catch (IllegalArgumentException e) {
                console.println("Invalid action '" + args[4] + "' for resource type " + type + ".");
                return;
            }
            Permission permission = new Permission(type, selector, action);
            Set<Permission> newPermissions = new HashSet<>(role.getPermissions());
            newPermissions.add(permission);
            RoleDefinition updated = new RoleDefinition(role.getName(), role.getDescription(), newPermissions, false);
            roleRegistry.update(updated);
            console.println("Permission added to role '" + roleName + "'.");
        } else {
            console.printUsage(findUsage(SUBCMD_ADDPERMISSION));
        }
    }

    private void removePermission(String[] args, Console console) {
        if (args.length == 5) {
            String roleName = args[1];
            RoleDefinition role = roleRegistry.get(roleName);
            if (role == null) {
                console.println("Role not found.");
                return;
            }
            if (!roleRegistry.isEditable(role)) {
                console.println("Cannot modify built-in role '" + roleName + "'.");
                return;
            }
            ResourceType type;
            try {
                type = ResourceType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                console.println("Unknown resource type: " + args[2] + ". Valid types: "
                        + Arrays.toString(ResourceType.values()));
                return;
            }
            Selector selector;
            try {
                selector = SelectorParser.parse(args[3]);
            } catch (IllegalArgumentException e) {
                console.println("Invalid selector: " + e.getMessage());
                return;
            }
            Action action;
            try {
                action = Actions.parse(type, args[4]);
            } catch (IllegalArgumentException e) {
                console.println("Invalid action '" + args[4] + "' for resource type " + type + ".");
                return;
            }
            Permission target = new Permission(type, selector, action);
            Set<Permission> newPermissions = new HashSet<>(role.getPermissions());
            if (newPermissions.remove(target)) {
                RoleDefinition updated = new RoleDefinition(role.getName(), role.getDescription(), newPermissions,
                        false);
                roleRegistry.update(updated);
                console.println("Permission removed from role '" + roleName + "'.");
            } else {
                console.println("Permission not found on role '" + roleName + "'.");
            }
        } else {
            console.printUsage(findUsage(SUBCMD_REMOVEPERMISSION));
        }
    }

    private void showRole(String[] args, Console console) {
        if (args.length == 2) {
            String roleName = args[1];
            RoleDefinition role = roleRegistry.get(roleName);
            if (role == null) {
                console.println("Role not found.");
                return;
            }
            printRoleWithPermissions(role, console);
        } else {
            console.printUsage(findUsage(SUBCMD_SHOW));
        }
    }

    private void printRoleWithPermissions(RoleDefinition role, Console console) {
        String label = role.isBuiltIn() ? "built-in" : "custom";
        console.println(role.getName() + " (" + label + ") — " + role.getDescription());
        if (role.getPermissions().isEmpty()) {
            if (role.isBuiltIn()) {
                console.println("  (no explicit permissions — allow-all)");
            } else {
                console.println("  (no permissions)");
            }
        } else {
            for (Permission p : role.getPermissions()) {
                console.println("  " + p.resourceType() + " " + p.selector().expression() + " " + p.action().name());
            }
        }
    }

    private String findUsage(String cmd) {
        return getUsages().stream().filter(u -> u.contains(cmd)).findAny().get();
    }
}
