/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.Semaphore;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.workbench.core.EclipseClassCreator;
import org.knime.workbench.core.WorkbenchErrorLogger;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.osgi.framework.Bundle;

/**
 * Manages the (global) KNIME Repository. This class collects all the
 * contributed extensions from the extension points and creates an arbitrary
 * model. Additionally, you can ask this to load/save workflows using the
 * appropriate eclipse classloaders
 * 
 * @author Florian Georg, University of Konstanz
 */
public final class RepositoryManager {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(RepositoryManager.class);

    /** The singleton instance. */
    public static final RepositoryManager INSTANCE = new RepositoryManager();

    // ID of "node" extension point
    private static final String ID_NODE = "org.knime.workbench.repository"
            + ".nodes";

    // ID of "category" extension point
    private static final String ID_CATEGORY = "org.knime.workbench."
            + "repository.categories";
    
    private static final String ID_META_NODE 
        = "org.knime.workbench.repository.metanode";

    // set the eclipse class creator into the static global class creator class
    static {
        GlobalClassCreator.setClassCreator(new EclipseClassCreator(ID_NODE));
    }
    
    private static Semaphore m_lock = new Semaphore(1);

    private Root m_root;

    private RepositoryManager() {
        // Singleton constructor

    }

    /**
     * Returns the extensions for a given extension point.
     * 
     * @param pointID
     *            The extension point ID
     * 
     * @return The extensions
     */
    private IExtension[] getExtensions(final String pointID) {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint(pointID);
        if (point == null) {
            throw new IllegalStateException("Invalid extension point : "
                    + pointID);

        }
        IExtension[] extensions = point.getExtensions();

        return extensions;
    }

    private static void removeDuplicatesFromCategories(
            final ArrayList<IConfigurationElement> allElements,
            final StringBuffer errorString) {

        // brut force search
        for (int i = 0; i < allElements.size(); i++) {
            for (int j = allElements.size() - 1; j > i; j--) {

                String pathOuter = allElements.get(i).getAttribute("path");
                String levelIdOuter = allElements.get(i).getAttribute(
                        "level-id");
                String pathInner = allElements.get(j).getAttribute("path");
                String levelIdInner = allElements.get(j).getAttribute(
                        "level-id");

                if (pathOuter.equals(pathInner)
                        && levelIdOuter.equals(levelIdInner)) {

                    String nameI = allElements.get(i).getAttribute("name");
                    String nameJ = allElements.get(j).getAttribute("name");

                    // the removal is only reported in case the names
                    // are not equal (if they are equal,the user will not
                    // notice any difference (except possibly the picture))
                    if (!nameI.equals(nameJ)) {
                        String pluginI = allElements.get(i)
                                .getDeclaringExtension()
                                .getNamespaceIdentifier();
                        String pluginJ = allElements.get(j)
                                .getDeclaringExtension()
                                .getNamespaceIdentifier();

                        String message = "Category '" + pathOuter + "/"
                                + levelIdOuter
                                + "' was found twice. Names are '" + nameI
                                + "'(Plugin: " + pluginI + ") and '" + nameJ
                                + "'(Plugin: " + pluginJ
                                + "). The category with name '" + nameJ
                                + "' is ignored.";

                        LOGGER.warn(message);
                        errorString.append(message + "\n");
                    }

                    // remove from the end of the list
                    allElements.remove(j);

                }
            }
        }
    }
    

    /**
     * Creates the repository model. This instantiates all contributed
     * category/node extensions found in the global Eclipse PluginRegistry, and
     * attaches them to the repository tree. This method normally should need
     * only be called once at the (plugin) startup.
     */
    public void create() {
        try {
            m_lock.acquire();
        } catch (InterruptedException e1) {
            LOGGER.error("Root in use by another thread", e1);
        }
        m_root = new Root();
        IExtension[] nodeExtensions = this.getExtensions(ID_NODE);
        IExtension[] categoryExtensions = this.getExtensions(ID_CATEGORY);
        
        boolean isInExpertMode = 
            Boolean.getBoolean(KNIMEConstants.ENV_VARIABLE_EXPERT_MODE);

        IExtension[] metanodeExtensions = getExtensions(ID_META_NODE);
        //
        // First, process the contributed categories
        //
        ArrayList<IConfigurationElement> allElements 
            = new ArrayList<IConfigurationElement>();

        for (int i = 0; i < categoryExtensions.length; i++) {

            IExtension ext = categoryExtensions[i];

            // iterate through the config elements and create 'Category' objects
            IConfigurationElement[] elements = ext.getConfigurationElements();
            allElements.addAll(Arrays.asList(elements));
        }

        // holds error string for possibly not instantiable nodes and
        // categories
        StringBuffer errorString = new StringBuffer();
        // remove duplicated categories
        removeDuplicatesFromCategories(allElements, errorString);

        // sort first by path-depth, so that everything is there in the
        // right order
        IConfigurationElement[] categoryElements = allElements
                .toArray(new IConfigurationElement[allElements.size()]);

        Arrays.sort(categoryElements, new Comparator<IConfigurationElement>() {

            public int compare(final IConfigurationElement o1,
                    final IConfigurationElement o2) {
                String element1 = o1.getAttribute("path");
                if (element1 == null || element1.equals("/")) {
                    return -1;
                }
                String element2 = o2.getAttribute("path");
                if (element2 == null || element2.equals("/")) {
                    return +1;
                }

                int countSlashes1 = element1.length()
                        - element1.replaceAll("/", "").length();

                int countSlashes2 = element2.length()
                        - element2.replaceAll("/", "").length();

                return countSlashes1 - countSlashes2;
            }

        });

        for (int j = 0; j < categoryElements.length; j++) {
            IConfigurationElement e = categoryElements[j];
            
            try {
                Category category = RepositoryFactory.createCategory(m_root, e);
                LOGGER.debug("Found category extension '" + category.getID()
                        + "' on path '" + category.getPath() + "'");
                LOGGER.info("Found category: " + category.getID());

            } catch (Exception ex) {
                String message = "Category '" + e.getAttribute("level-id")
                        + "' from plugin '"
                        + e.getDeclaringExtension().getNamespaceIdentifier()
                        + "' could not be created in parent path '"
                        + e.getAttribute("path") + "'.";
                LOGGER.error(message, ex);
                errorString.append(message + "\n");
            }

        } // for

        //
        // Second, process the contributed nodes
        //

        for (int i = 0; i < nodeExtensions.length; i++) {

            IExtension ext = nodeExtensions[i];

            // iterate through the config elements and create 'NodeTemplate'
            // objects
            IConfigurationElement[] elements = ext.getConfigurationElements();
            for (int j = 0; j < elements.length; j++) {
                IConfigurationElement e = elements[j];

                try {
                    NodeTemplate node = RepositoryFactory.createNode(e);
                    boolean skip = !isInExpertMode && node.isExpertNode();
                    if (skip) {
                        LOGGER.debug("Skipping node extension '" + node.getID()
                                + "': " + node.getName() 
                                + " (not in expert mode)");
                        continue;
                    } else {
                        LOGGER.debug("Found node extension '" + node.getID()
                                + "': " + node.getName());
                    }
                    String nodeName = node.getID();
                    nodeName = nodeName
                            .substring(nodeName.lastIndexOf('.') + 1);
                    // LOGGER.info("Found node: " + node.getName());

                    // Ask the root to lookup the category-container located at
                    // the given path
                    IContainerObject parentContainer = m_root
                            .findContainer(node.getCategoryPath());

                    // If parent category is illegal, log an error and append
                    // the node to the repository root.
                    if (parentContainer == null) {
                        WorkbenchErrorLogger
                                .warning("Invalid category-path for node "
                                        + "contribution: '"
                                        + node.getCategoryPath()
                                        + "' - adding to root instead");
                        m_root.addChild(node);
                    } else {
                        // everything is fine, add the node to its parent
                        // category
                        parentContainer.addChild(node);
                    }

                } catch (Throwable t) {

                    String message = "Node " + e.getAttribute("id")
                            + "' from plugin '" + ext.getNamespaceIdentifier()
                            + "' could not be created.";
                    Plugin plugin = Platform.getPlugin(ext
                            .getNamespaceIdentifier());

                    if (plugin == null) {
                        // if the plugin is null, the plugin could not
                        // be activated maybe due to a not
                        // activateable plugin (plugin class can not be found)
                        message = message + " The corresponding plugin "
                                + "bundle could not be activated!";
                    }
                    
                    LOGGER.error(message, t);
                    errorString.append(message + "\n");
                }

            } // for configuration elements            
        } // for node extensions
        
        // iterate over the meta node config elements
        // and create meta node templates
        for (IExtension mnExt : metanodeExtensions) {
            IConfigurationElement[] mnConfigElems =
                mnExt.getConfigurationElements();
                for (IConfigurationElement mnConfig : mnConfigElems) {
                    try {
                        MetaNodeTemplate metaNode =
                            RepositoryFactory.createMetaNode(mnConfig);
                        boolean skip = !isInExpertMode 
                            && metaNode.isExpertNode();
                        if (skip) {
                            LOGGER.debug("Skipping meta node definition '" 
                                    + metaNode.getID() + "': " 
                                    + metaNode.getName() 
                                    + " (not in expert mode)");
                            continue;
                        } else {
                            LOGGER.debug("Found meta node definition '" 
                                    + metaNode.getID() + "': " 
                                    + metaNode.getName());
                        }
                        IContainerObject parentContainer =
                            m_root.findContainer(metaNode.getCategoryPath());
                        // If parent category is illegal, log an error and 
                        // append the node to the repository root.
                        if (parentContainer == null) {
                            WorkbenchErrorLogger
                            .warning("Invalid category-path for node "
                                    + "contribution: '"
                                    + metaNode.getCategoryPath()
                                    + "' - adding to root instead");
                            m_root.addChild(metaNode);
                        } else {
                            // everything is fine, add the node to its parent
                            // category
                            parentContainer.addChild(metaNode);
                        }
                    } catch (Throwable t) {
                        String message = "MetaNode " 
                            + mnConfig.getAttribute("id")
                        + "' from plugin '" + mnConfig.getNamespaceIdentifier()
                        + "' could not be created.";
                        Bundle plugin = Platform.getBundle(mnConfig
                                .getNamespaceIdentifier());
                        
                        if (plugin == null) {
                            // if the plugin is null, the plugin could not
                            // be activated maybe due to a not
                            // activateable plugin 
                            // (plugin class can not be found)
                            message = message + " The corresponding plugin "
                            + "bundle could not be activated!";
                        }
                        
                        LOGGER.error(message, t);
                        errorString.append(message + "\n");
                    }
                }
        }
        
        m_lock.release();

        // if errors occured show an information box
        if (errorString.length() > 0 
                && !"true".equals(System.getProperty("java.awt.headless"))) {

            
            // TODO: validate if the this works without getting the active shell
            // get an appropriate shell
            
            Display defaultDisplay = Display.getDefault();
            if (defaultDisplay != null) {
                /*
                Shell[] parent = new Shell[1];
                parent[0] = defaultDisplay.getActiveShell();
                if (parent[0] == null) {
                    parent = Display.getDefault().getShells();
                }

                if (parent[0] != null) {
                    showErrorMessage(parent[0], errorString.toString());
                }
            }
            */
            showErrorMessage(errorString.toString());
            WorkbenchErrorLogger
                    .warning("Could not load all contributed nodes: \n"
                            + errorString);
            }
        }

    }

    private void showErrorMessage(final String errorMessage) {
        // create a dummy shell, to force the message box to the top
        // otherwise it could be lost in the background of the
        // desktop
        // instead of taking the parent simply take the Display.getDefault();
        // TODO: validate this
        try {
            new Thread() {
                @Override
                public void run() {
                    Display.getDefault().syncExec(new Runnable() {
                        public void run() {
                            Shell dummy =
                                    new Shell(Display.getDefault(),
                                            SWT.DIALOG_TRIM | SWT.ON_TOP);
                            MessageBox mb =
                                    new MessageBox(dummy, SWT.ICON_WARNING
                                            | SWT.OK | SWT.ON_TOP);
                            mb.setText("KNIME extension(s) could not be " 
                                    + "created or are duplicates!");
                            mb.setMessage("Some contributed KNIME extensions"
                                            + " could not be created or are " 
                                            + "duplicates, they will be "
                                            + "skipped: \n\n'"
                                            + errorMessage.toString());
                            mb.open();
                        }
                    });
                }
            }.start();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Returns the repository root.
     * 
     * @return The root object 
     */
    public Root getRoot() {
        return m_root;
    }
    
    public void releaseRoot() {
        m_lock.release();
    }
    
    
    
    public boolean isRootAvailable() {
        return m_lock.tryAcquire();
    }

    /**
     * Loads a WFM from a config object.
     * 
     * @param settings
     *            NodeSettings to load from.
     * @return The workflow manager.s
     */
    // public WorkflowManager loadWorkflowFromConfig(final NodeSettings
    // settings) {
    // assert settings != null;
    //
    // WorkflowManager manager = new WorkflowManager();
    // try {
    // manager = new WorkflowManager(settings);
    // } catch (InvalidSettingsException e) {
    // LOGGER.error("Could not load workflow.");
    // LOGGER.debug("Could not load workflow\n" + settings, e);
    // }
    //
    // return manager;
    // }
}
