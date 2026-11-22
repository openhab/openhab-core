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
package org.openhab.core.io.console.karaf.internal;

import static org.apache.karaf.shell.support.ansi.SimpleAnsi.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.wrapper.WrapperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Karaf shell command 'openhab:install-service' that installs openHAB as a
 * system service . This command will first call the default Karaf wrapper
 * service installation to install a Java Service Wrapper. After installation,
 * it will inject the current JVM arguments into the wrapper.conf. This will
 * make sure that all necessary properties like openhab.home, etc are available
 * when running as a system service. The service binaries are installed into
 * karaf.home rather than karaf.base, which means the service can only support a
 * single Karaf instance.
 *
 * @author Davy Vanherbergen - Initial contribution
 */
@Command(scope = "openhab", name = "install-service", description = "Install openHAB as a system service.")
@Service
public class InstallServiceCommand implements Action {

    private final Logger logger = LoggerFactory.getLogger(InstallServiceCommand.class);

    private static final String SERVICE_NAME = "openHAB";

    private static final String SERVICE_DISPLAY_NAME = "openHAB runtime";

    private static final String SERVICE_DESCRIPTION = "System service for openHAB.";

    private static final String SERVICE_START_TYPE = "AUTO_START";

    private static final String[] ARG_EXCLUSION_LIST = new String[] { "-Djava.endorsed", "-Djava.ext", "-Dkaraf.home",
            "-Dkaraf.base", "-Dkaraf.data", "-Dkaraf.etc", "-Dkaraf.start", "-agentlib" };

    @Reference
    private WrapperService service;

    @Override
    public Object execute() throws Exception {
        printLine("");
        printLine("Starting openHAB system service installation...");
        printLine("");

        // get current JVM arguments
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> jvmArguments = runtimeMxBean.getInputArguments();
        List<String> argumentsToAdd = new ArrayList<>();

        // filter out standard karaf and debug arguments
        for (String arg : jvmArguments) {
            boolean excluded = false;
            for (String exclude : ARG_EXCLUSION_LIST) {
                if (arg.toLowerCase().startsWith(exclude.toLowerCase())) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded) {
                argumentsToAdd.add(arg);
            }
        }

        // we want to install to karaf.runtime, rather than karaf.base
        // to prevent the installed binaries from being deleted with karaf
        // clean.
        // so we need to temporarily change our environment to trick the install
        String karafBase = System.getProperty("karaf.base");
        String karafHome = System.getProperty("karaf.home");

        if (karafBase == null) {
            printLine("System property karaf.base is not set. Install aborted.");
            return null;
        } else if (karafHome == null) {
            printLine("System property karaf.home is not set. Install aborted.");
            return null;
        } else if (!new File(karafHome).canWrite()) {
            printLine("Cannot write to " + karafHome + ". Install aborted.");
            return null;
        }

        // override karaf.base to trick install
        System.setProperty("karaf.base", karafHome);

        // install Karaf service
        File[] files = service.install(SERVICE_NAME, SERVICE_DISPLAY_NAME, SERVICE_DESCRIPTION, SERVICE_START_TYPE);
        File wrapperConf = files[0];
        File serviceFile = files[1];
        File systemdFile = files[2];

        // restore karaf base location
        System.setProperty("karaf.base", karafBase);

        // determine os
        String os = Objects.requireNonNullElse(System.getProperty("os.name"), "Unknown");

        // read contents of config file.
        List<String> content = new ArrayList<>();
        InputStream is = null;
        try {
            is = new FileInputStream(wrapperConf);
            Scanner scanner = new Scanner(is);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                // update log location
                if (line.indexOf("/log/") > 0) {
                    line = line.replace("/log/", "/logs/");
                }

                // restore karaf.base location
                if (line.startsWith("set.default.KARAF_BASE")) {
                    line = "set.default.KARAF_BASE=" + karafBase;
                }

                // disable default max mem
                if (line.startsWith("wrapper.java.maxmemory")) {
                    line = "#" + line;
                }

                content.add(line);
            }
            scanner.close();
        } finally {
            safeClose(is);
        }

        // find max index of java properties
        int maxIndex = 1;
        for (String line : content) {
            if (line.startsWith("wrapper.java.additional.") && line.indexOf('=') > 0) {
                String key = line.substring(0, line.indexOf('='));
                String value = line.substring(line.indexOf('='));
                try {
                    int propIndex = Integer.parseInt(key.substring(key.lastIndexOf('.') + 1));
                    if (propIndex > maxIndex) {
                        maxIndex = propIndex;
                    }
                } catch (NumberFormatException e) {
                    logger.error("Config file contains invalid property {}", key);
                }
                // let's not add duplicates
                argumentsToAdd.remove(value);
            }
        }

        // overwrite wrapper.conf file with new contents
        PrintStream out = null;
        try {
            out = new PrintStream(new FileOutputStream(wrapperConf));
            for (String line : content) {
                out.println(line);
                if (line.startsWith("wrapper.java.additional." + maxIndex)) {
                    // inject our JVM arguments here
                    for (String arg : argumentsToAdd) {
                        out.println("wrapper.java.additional." + ++maxIndex + "=" + arg);
                    }
                }
            }
        } finally {
            safeClose(out);
        }

        // print post installation steps to be done by user
        printLine("");
        if (os.startsWith("Win")) {
            printPostInstallStepsWindows(serviceFile);
        } else if (os.startsWith("Mac OS X")) {
            printPostInstallStepsOsX(createOpenHabPList());
        } else if (os.startsWith("Linux")) {
            File debianVersion = new File("/etc/debian_version");
            File redhatRelease = new File("/etc/redhat-release");
            if (redhatRelease.exists()) {
                printPostInstallStepsRedHat(serviceFile);
            } else if (debianVersion.exists()) {
                printPostInstallStepsDebian(serviceFile);
            } else {
                printPostInstallStepsRedHat(serviceFile);
                printLine("");
                printPostInstallStepsDebian(serviceFile);
            }
            if (systemdFile != null) {
                printLine("");
                printPostInstallStepsSystemd(systemdFile);
            }
        }
        printLine("");

        return null;
    }

    private static void printLine(String message) {
        Objects.requireNonNull(System.out, "Standard output is unavailable").println(message);
    }

    /**
     * Print the necessary next steps to complete the installation on Red Hat to
     * the console.
     *
     * @param serviceFile
     *            wrapper service file
     */
    private void printPostInstallStepsRedHat(File serviceFile) {
        printLine(INTENSITY_BOLD + "RedHat/Fedora/CentOS Linux system detected (SystemV):" + INTENSITY_NORMAL);
        printLine("  To install the service:");
        printLine("    $ ln -s " + serviceFile.getPath() + " /etc/init.d/");
        printLine("    $ chkconfig " + serviceFile.getName() + " --add");
        printLine("");
        printLine("  To start the service when the machine is rebooted:");
        printLine("    $ chkconfig " + serviceFile.getName() + " on");
        printLine("");
        printLine("  To disable starting the service when the machine is rebooted:");
        printLine("    $ chkconfig " + serviceFile.getName() + " off");
        printLine("");
        printLine("  To start the service:");
        printLine("    $ service " + serviceFile.getName() + " start");
        printLine("");
        printLine("  To stop the service:");
        printLine("    $ service " + serviceFile.getName() + " stop");
        printLine("");
        printLine("  To uninstall the service:");
        printLine("    $ chkconfig " + serviceFile.getName() + " --del");
        printLine("    $ rm /etc/init.d/" + serviceFile.getPath());
    }

    /**
     * Print the necessary next steps to complete the installation on Debian to
     * the console.
     *
     * @param serviceFile
     *            wrapper service file
     */
    private void printPostInstallStepsDebian(File serviceFile) {
        printLine(INTENSITY_BOLD + "Ubuntu/Debian Linux system detected (SystemV):" + INTENSITY_NORMAL);
        printLine("  To install the service:");
        printLine("    $ ln -s " + serviceFile.getPath() + " /etc/init.d/");
        printLine("");
        printLine("  To start the service when the machine is rebooted:");
        printLine("    $ update-rc.d " + serviceFile.getName() + " defaults");
        printLine("");
        printLine("  To disable starting the service when the machine is rebooted:");
        printLine("    $ update-rc.d -f " + serviceFile.getName() + " remove");
        printLine("");
        printLine("  To start the service:");
        printLine("    $ /etc/init.d/" + serviceFile.getName() + " start");
        printLine("");
        printLine("  To stop the service:");
        printLine("    $ /etc/init.d/" + serviceFile.getName() + " stop");
        printLine("");
        printLine("  To uninstall the service:");
        printLine("    $ rm /etc/init.d/" + serviceFile.getName());
    }

    /**
     * Print the necessary next steps to complete the installation on Systemd
     * compliant linux to the console.
     *
     * @param systemdFile
     *            systemd file
     */
    private void printPostInstallStepsSystemd(File systemdFile) {
        printLine(INTENSITY_BOLD + "For systemd compliant Linux: " + INTENSITY_NORMAL);
        printLine("  To install the service (and enable at system boot):");
        printLine("   $ systemctl enable " + systemdFile.getPath());
        printLine("");
        printLine("  To start the service:");
        printLine("   $ systemctl start " + SERVICE_NAME);
        printLine("");
        printLine("  To stop the service:");
        printLine("   $ systemctl stop " + SERVICE_NAME);
        printLine("");
        printLine("  To check the current service status:");
        printLine("   $ systemctl status " + SERVICE_NAME);
        printLine("");
        printLine("  To see service activity journal:");
        printLine("   $ journalctl -u " + SERVICE_NAME);
        printLine("");
        printLine("  To uninstall the service (and disable at system boot):");
        printLine("   $ systemctl disable " + SERVICE_NAME);
    }

    /**
     * Print the necessary next steps to complete the installation on Windows to
     * the console.
     *
     * @param serviceFile
     *            wrapper service file
     */
    private void printPostInstallStepsWindows(File serviceFile) {
        printLine(INTENSITY_BOLD + "MS Windows system detected:" + INTENSITY_NORMAL);
        printLine("To install the service, run: ");
        printLine("  C:> " + serviceFile.getPath() + " install");
        printLine("");
        printLine("Once installed, to start the service run: ");
        printLine("  C:> net start \"" + SERVICE_NAME + "\"");
        printLine("");
        printLine("Once running, to stop the service run: ");
        printLine("  C:> net stop \"" + SERVICE_NAME + "\"");
        printLine("");
        printLine("Once stopped, to remove the installed the service run: ");
        printLine("  C:> " + serviceFile.getPath() + " remove");
    }

    /**
     * Print the necessary next steps to complete the installation on OsX to the
     * console.
     *
     * @param serviceFile
     *            openHAB pList file
     */
    private void printPostInstallStepsOsX(File serviceFile) {
        printLine(INTENSITY_BOLD + "Mac OS X system detected:" + INTENSITY_NORMAL);
        printLine("");
        printLine("To configure openHAB as a " + INTENSITY_BOLD + "user service" + INTENSITY_NORMAL
                + " execute the following file move:");
        printLine("> mv " + serviceFile.getPath() + " ~/Library/LaunchAgents/");
        printLine("");
        printLine("To configure openHAB as a " + INTENSITY_BOLD + "system service" + INTENSITY_NORMAL
                + " execute the following file move and authorisation changes:");
        printLine("> sudo mv " + serviceFile.getPath() + " /Library/LaunchDaemons/");
        printLine("> sudo chown root:wheel /Library/LaunchDaemons/" + serviceFile.getName());
        printLine("> sudo chmod u=rw,g=r,o=r /Library/LaunchDaemons/" + serviceFile.getName());
        printLine("");
        printLine("To test your service: ");
        printLine("> launchctl load ~/Library/LaunchAgents/" + serviceFile.getName());
        printLine("> launchctl start openHAB");
        printLine("> launchctl stop openHAB");
        printLine("");
        printLine(
                "After restart of your session or system you can use the launchctl command to start and stop your service");
        printLine("");
        printLine("For removing the service call:");
        printLine("> launchctl remove openHAB");
    }

    /**
     * Convert the default generated karaf.plist to the openHAB.plist.
     *
     * @return File openHAB.plist
     * @throws FileNotFoundException
     */
    private File createOpenHabPList() throws FileNotFoundException {
        // tweak plist file
        String plistPath = System.getProperty("karaf.home") + "/bin/";
        File karafPlist = new File(plistPath + "org.apache.karaf.openHAB.plist");
        File openHabPlist = new File(plistPath + "openHAB.plist");

        InputStream is = null;
        PrintStream out = null;
        try {
            is = new FileInputStream(karafPlist);
            out = new PrintStream(new FileOutputStream(openHabPlist));
            Scanner scanner = new Scanner(is);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.indexOf("org.apache.karaf.KARAF") > 0) {
                    line = line.replace("org.apache.karaf.KARAF", "openHAB");
                }
                out.println(line);
            }
            scanner.close();
        } finally {
            safeClose(is);
            safeClose(out);
        }
        karafPlist.delete();
        return openHabPlist;
    }

    /**
     * Silently close inputstream
     *
     * @param is
     *            inputstream
     */
    private void safeClose(InputStream is) {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (IOException e) {
            logger.error("Error closing inputstream", e);
        }
    }

    /**
     * Silently close outputstream
     *
     * @param os
     *            outputstream
     */
    private void safeClose(OutputStream os) {
        if (os == null) {
            return;
        }
        try {
            os.close();
        } catch (IOException e) {
            logger.error("Error closing outputstream", e);
        }
    }
}
