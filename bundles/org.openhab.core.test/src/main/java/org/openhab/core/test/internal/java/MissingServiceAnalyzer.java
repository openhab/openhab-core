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
package org.openhab.core.test.internal.java;

import static java.util.stream.Collectors.*;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.component.runtime.dto.ReferenceDTO;

/**
 * Utility class to analyze and print possible reasons for a service being not present.
 *
 * @author Simon Kaufmann - Initial contribution
 */
public class MissingServiceAnalyzer {

    private final PrintStream ps;
    private final BundleContext bundleContext;

    public MissingServiceAnalyzer(PrintStream ps, BundleContext bundleContext) {
        this.ps = ps;
        this.bundleContext = bundleContext;
    }

    public <T> void printMissingServiceDetails(Class<T> clazz) {
        ServiceReference<ServiceComponentRuntime> scrReference = bundleContext
                .getServiceReference(ServiceComponentRuntime.class);
        if (scrReference != null) {
            ServiceComponentRuntime scr = bundleContext.getService(scrReference);
            if (scr != null) {
                ps.println("Components implementing " + clazz.getName() + ":");
                printUnsatisfiedServices(scr, clazz.getName(), "");
            }
        } else {
            ps.println("SCR is not started! Add the SCR bundle to your launch config.");
        }
    }

    private <T> void printUnsatisfiedServices(ServiceComponentRuntime scr, String interfaceName, String prefix) {
        Bundle[] allBundlesArrays = getAllBundles();
        List<ComponentDescriptionDTO> descriptions = getComponentDescriptions(scr, interfaceName, allBundlesArrays);
        if (descriptions.isEmpty()) {
            ps.println(prefix + "No component implementing " + interfaceName + " is currently registered.");
            ps.println(
                    "Make sure to add the appropriate bundle and set 'Default Auto-Start=true' in the launch config.");
        } else {
            for (ComponentDescriptionDTO description : descriptions) {
                Collection<ComponentConfigurationDTO> configurations = scr.getComponentConfigurationDTOs(description);
                for (ComponentConfigurationDTO configuration : configurations) {
                    ps.println(prefix + configuration.id + " [" + getState(configuration.state) + "] "
                            + description.implementationClass + " in " + description.bundle.symbolicName);
                    for (ReferenceDTO ref : getUnsatisfiedReferences(description, configuration)) {
                        ps.println(prefix + "\t" + ref.name + " (" + ref.interfaceName + ")");
                        printUnsatisfiedServices(scr, ref.interfaceName, prefix + "\t\t");
                    }
                }
            }
        }
    }

    private List<ReferenceDTO> getUnsatisfiedReferences(ComponentDescriptionDTO description,
            ComponentConfigurationDTO configuration) {
        Set<String> unsatisfiedRefNames = Stream.of(configuration.unsatisfiedReferences)//
                .map(ref -> ref.name) //
                .collect(toSet());
        return Stream.of(description.references) //
                .filter(ref -> unsatisfiedRefNames.contains(ref.name)) //
                .collect(toList());
    }

    private List<ComponentDescriptionDTO> getComponentDescriptions(ServiceComponentRuntime scr, String interfaceName,
            Bundle[] allBundlesArrays) {
        return scr.getComponentDescriptionDTOs(allBundlesArrays).stream()
                .filter(description -> Stream.of(description.serviceInterfaces).anyMatch(s -> s.equals(interfaceName)))
                .collect(toList());
    }

    private Bundle[] getAllBundles() {
        List<Bundle> allBundles = Arrays.stream(bundleContext.getBundles())
                .filter(b -> b.getHeaders().get(Constants.FRAGMENT_HOST) == null).collect(toList());
        return allBundles.toArray(new Bundle[allBundles.size()]);
    }

    private String getState(int state) {
        switch (state) {
            case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
                return "UNSATISFIED_CONFIGURATION";
            case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
                return "UNSATISFIED_REFERENCE";
            case ComponentConfigurationDTO.SATISFIED:
                return "SATISFIED";
            case ComponentConfigurationDTO.ACTIVE:
                return "ACTIVE";
            default:
                return state + "";
        }
    }
}
