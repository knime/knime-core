/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   16.03.2005 (georg): created
 */
package org.knime.workbench.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.knime.core.eclipseUtil.GlobalClassCreator;
import org.knime.core.node.NodeLogger;

import org.knime.workbench.core.EclipseClassCreator;
import org.knime.workbench.core.WorkbenchErrorLogger;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;

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

    // set the eclipse class creator into the static global class creator class
    static {

        GlobalClassCreator.setClassCreator(new EclipseClassCreator(ID_NODE));
    }

    private Root m_root;

    private RepositoryManager() {
        // Singleton constructor

    }

    /**
     * Returns the extensions for a given extension point.
     * 
     * @param pointID The extension point ID
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

    /**
     * Creates the repository model. This instantiates all contributed
     * category/node extensions found in the global Eclipse PluginRegistry, and
     * attaches them to the repository tree. This method normally should need
     * only be called once at the (plugin) startup.
     */
    public void create() {
        m_root = new Root();

        IExtension[] nodeExtensions = this.getExtensions(ID_NODE);
        IExtension[] categoryExtensions = this.getExtensions(ID_CATEGORY);

        //
        // First, process the contributed categories
        //
        ArrayList<IConfigurationElement> allElements =
            new ArrayList<IConfigurationElement>();

        for (int i = 0; i < categoryExtensions.length; i++) {

            IExtension ext = categoryExtensions[i];

            // iterate through the config elements and create 'Category' objects
            IConfigurationElement[] elements = ext.getConfigurationElements();
            allElements.addAll(Arrays.asList(elements));
        }

        // sort first by path-depth, so that everything is there in the
        // right order
        IConfigurationElement[] categoryElements = allElements
                .toArray(new IConfigurationElement[allElements.size()]);

        Arrays.sort(categoryElements, new Comparator<IConfigurationElement>() {

            public int compare(final IConfigurationElement o1,
                    final IConfigurationElement o2) {
                String element1 = o1.getAttribute("path");
                if (element1.equals("/")) {
                    return -1;
                }
                String element2 = o2.getAttribute("path");
                if (element2.equals("/")) {
                    return +1;
                }

                int countSlashes1 = element1 == null ? 0 : element1.length()
                        - element1.replaceAll("/", "").length();

                int countSlashes2 = element1 == null ? 0 : element2.length()
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
                LOGGER.error(ex); // <=== DON'T PRINT TO SYSTEM.OUT,
                // PLEASE
                WorkbenchErrorLogger.error("Could not load contributed "
                        + "extension, skipped: '" + e.getAttribute("id")
                        + "' from plugin '"
                        + e.getDeclaringExtension().getNamespace() + "'", ex);
            }

        } // for

        //
        // Second, process the contributed nodes
        //

        // holds error string for possibly not instantiable nodes
        StringBuffer errorString = new StringBuffer();
        for (int i = 0; i < nodeExtensions.length; i++) {

            IExtension ext = nodeExtensions[i];

            // iterate through the config elements and create 'NodeTemplate'
            // objects
            IConfigurationElement[] elements = ext.getConfigurationElements();
            for (int j = 0; j < elements.length; j++) {
                IConfigurationElement e = elements[j];

                try {
                    NodeTemplate node = RepositoryFactory.createNode(e);
                    LOGGER.debug("Found node extension '" + node.getID()
                            + "': " + node.getName());
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

                } catch (Exception ex) {

                    errorString.append(e.getAttribute("id") + "' from plugin '"
                            + ext.getNamespace() + "'\n");

                }

            } // for

        }

        // if errors occured show an information box
        if (errorString.length() > 0) {
            MessageBox mb = new MessageBox(Display.getDefault()
                    .getActiveShell(), SWT.ICON_WARNING | SWT.OK);
            mb.setText("Node(s) could not be loaded!");
            mb.setMessage("Could not load all contributed node "
                    + "extensions, skipped: '\n\n" + errorString.toString());
            mb.open();

            WorkbenchErrorLogger
                    .warning("Could not load all contributed nodes ");
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

    /**
     * Loads a WFM from a config object.
     * 
     * @param settings NodeSettings to load from.
     * @return The workflow manager.s
     */
//    public WorkflowManager loadWorkflowFromConfig(final NodeSettings settings) {
//        assert settings != null;
//
//        WorkflowManager manager = new WorkflowManager();
//        try {
//            manager = new WorkflowManager(settings);
//        } catch (InvalidSettingsException e) {
//            LOGGER.error("Could not load workflow.");
//            LOGGER.debug("Could not load workflow\n" + settings, e);
//        }
//
//        return manager;
//    }
}
