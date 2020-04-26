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
package org.openhab.core.model.lazygen;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.mwe.core.ConfigurationException;
import org.eclipse.emf.mwe.core.WorkflowContext;
import org.eclipse.emf.mwe.core.issues.Issues;
import org.eclipse.emf.mwe.core.lib.AbstractWorkflowComponent2;
import org.eclipse.emf.mwe.core.monitor.ProgressMonitor;
import org.eclipse.emf.mwe.core.resources.ResourceLoaderFactory;
import org.eclipse.emf.mwe.utils.GenModelHelper;

/**
 *
 * @author Holger Schill, Simon Kaufmann - Initial contribution
 */
public class LazyStandaloneSetup extends AbstractWorkflowComponent2 {

    private static ResourceSet resourceSet = GlobalResourceSet.getINSTANCE();
    private final Registry registry = EPackage.Registry.INSTANCE;

    Set<String> allgeneratedEPackages = new HashSet<>();
    Set<String> allGenModelFiles = new HashSet<>();
    Set<String> allEcoreFiles = new HashSet<>();

    public void addGeneratedPackage(String packageName) {
        allgeneratedEPackages.add(packageName);
    }

    public void addGenModelFile(String modelFile) {
        allGenModelFiles.add(modelFile);
    }

    public void addEcoreModelFile(String modelFile) {
        allEcoreFiles.add(modelFile);
    }

    @Override
    protected void invokeInternal(WorkflowContext ctx, ProgressMonitor monitor, Issues issues) {
        for (String generatedEPackage : allgeneratedEPackages) {
            addRegisterGeneratedEPackage(generatedEPackage);
        }
        for (String genModelFile : allGenModelFiles) {
            addRegisterGenModelFile(genModelFile);
        }
        for (String ecoreFile : allEcoreFiles) {
            addRegisterEcoreFile(ecoreFile);
        }
    }

    private final org.apache.commons.logging.Log log = LogFactory.getLog(getClass());

    private void addRegisterGeneratedEPackage(String interfacename) {
        Class<?> clazz = ResourceLoaderFactory.createResourceLoader().loadClass(interfacename);
        if (clazz == null) {
            throw new ConfigurationException("Couldn't find an interface " + interfacename);
        }
        try {
            EPackage pack = (EPackage) clazz.getDeclaredField("eINSTANCE").get(null);
            if (registry.get(pack.getNsURI()) == null) {
                registry.put(pack.getNsURI(), pack);
                log.info("Adding generated EPackage '" + interfacename + "'");
            }
        } catch (Exception e) {
            throw new ConfigurationException("Couldn't register " + interfacename
                    + ". Is it the generated EPackage interface? : " + e.getMessage());
        }
    }

    private void addRegisterEcoreFile(String fileName) throws IllegalArgumentException, SecurityException {
        Resource res = resourceSet.getResource(createURI(fileName), true);
        if (res == null) {
            throw new ConfigurationException("Couldn't find resource under  " + fileName);
        }
        if (!res.isLoaded()) {
            try {
                res.load(null);
            } catch (IOException e) {
                throw new ConfigurationException("Couldn't load resource under  " + fileName + " : " + e.getMessage());
            }
        }
        List<EObject> result = res.getContents();
        for (EObject object : result) {
            if (object instanceof EPackage) {
                registerPackage(fileName, object);
            }
            for (final TreeIterator<EObject> it = object.eAllContents(); it.hasNext();) {
                EObject child = it.next();
                if (child instanceof EPackage) {
                    registerPackage(fileName, child);
                }
            }
        }
    }

    private GenModelHelper createGenModelHelper() {
        return new GenModelHelper();
    }

    private void addRegisterGenModelFile(String fileName) {
        createGenModelHelper().registerGenModel(resourceSet, createURI(fileName));
    }

    private void registerPackage(String fileName, EObject object) {
        String nsUri = ((EPackage) object).getNsURI();
        if (registry.get(nsUri) == null) {
            registry.put(nsUri, object);
            log.info("Adding dynamic EPackage '" + nsUri + "' from '" + fileName + "'");
        } else if (log.isDebugEnabled()) {
            log.debug("Dynamic EPackage '" + nsUri + "' from '" + fileName + "' already in the registry!");
        }
    }

    private URI createURI(String path) {
        if (path == null) {
            throw new IllegalArgumentException();
        }

        URI uri = URI.createURI(path);
        if (uri.isRelative()) {
            URI resolvedURI = URI.createFileURI(new File(path).getAbsolutePath());
            return resolvedURI;
        }
        return uri;
    }
}
