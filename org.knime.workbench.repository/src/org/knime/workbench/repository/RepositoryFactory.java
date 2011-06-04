/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   ${date} (${user}): created
 */
package org.knime.workbench.repository;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.workflow.WorkflowLoadHelper;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.node.workflow.WorkflowPersistorVersion1xx;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.MetaNodeTemplate;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;
import org.osgi.framework.Bundle;

/**
 * Factory for creation of repository objects from
 * <code>IConfigurationElement</code> s from the Plugin registry.
 *
 * @author Florian Georg, University of Konstanz
 */
public final class RepositoryFactory {
    private RepositoryFactory() {
        // hidden constructor (utility class)
    }


    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            RepositoryFactory.class);

    private static final String META_NODE_ICON
        = "icons/meta_nodes/metanode_template.png";

    private static ImageDescriptor defaultIcon;

    /**
     * Workflow manager instance loading and administering
     * the predefined meta nodes.
     */
    public static final WorkflowManager META_NODE_ROOT;

    static {
        META_NODE_ROOT = WorkflowManager.ROOT.createAndAddProject(
                "KNIME MetaNode Repository");
    }


    /**
     * Creates a new node repository object. Throws an exception, if this fails
     *
     * @param element Configuration element from the contributing plugin
     * @return NodeTemplate object to be used within the repository.
     * @throws IllegalArgumentException If the element is not compatible (e.g.
     *             wrong attributes, or factory class not found)
     */
    @SuppressWarnings("unchecked")
    public static NodeTemplate createNode(final IConfigurationElement element) {
        String id = element.getAttribute("id");

        NodeTemplate node = new NodeTemplate(id);

        node.setAfterID(str(element.getAttribute("after"), ""));
        boolean b = Boolean.parseBoolean(element.getAttribute("expert-flag"));
        node.setExpertNode(b);

        // Try to load the node factory class...
        NodeFactory<? extends NodeModel> factory;
        try {

            // this ensures that the class is loaded by the correct eclipse
            // classloaders
            factory =
                    (NodeFactory<? extends NodeModel>)element
                            .createExecutableExtension("factory-class");

            node.setFactory(
                    (Class<NodeFactory<? extends NodeModel>>)
                    factory.getClass());
        } catch (Throwable e) {
            throw new IllegalArgumentException(
                    "Can't load factory class for node: "
                            + element.getAttribute("factory-class"), e);

        }

        node.setName(factory.getNodeName());
        node.setType(
                str(element.getAttribute("type"), NodeTemplate.TYPE_OTHER));

        String pluginID = element.getDeclaringExtension()
            .getNamespaceIdentifier();
        node.setPluginID(pluginID);

        if (!Boolean.valueOf(
                System.getProperty("java.awt.headless", "false"))) {
            // Load images from declaring plugin
            Image icon = ImageRepository.getScaledImage(
                    factory.getIcon(), 16, 16);
            // get default image if null
            if (icon == null) {
                icon = ImageRepository.getScaledImage(
                        NodeFactory.getDefaultIcon(), 16, 16);
            }
            // FIXME dispose this somewhere !!
            node.setIcon(icon);
        }

        node.setCategoryPath(str(element.getAttribute("category-path"), "/"));

        return node;
    }

    /**
     *
     * @param configuration content of the extension
     * @return a meta node template
     */
    public static MetaNodeTemplate createMetaNode(
            final IConfigurationElement configuration) {
        String id = configuration.getAttribute("id");
        String name = configuration.getAttribute("name");
        String workflowDir = configuration.getAttribute("workflowDir");
        String after = configuration.getAttribute("after");
        String iconPath = configuration.getAttribute("icon");
        String categoryPath = configuration.getAttribute("category-path");
        boolean isExpertNode = Boolean.parseBoolean(
                configuration.getAttribute("expert-flag"));
        String pluginId = configuration.getDeclaringExtension()
            .getNamespaceIdentifier();
        String description = configuration.getAttribute("description");

        WorkflowManager manager = loadMetaNode(pluginId, workflowDir);
        if (manager == null) {
            LOGGER.error("MetaNode  " + name + " could not be loaded. "
                    + "Skipped.");
            return null;
        }
        MetaNodeTemplate template = new MetaNodeTemplate(
                id, name, categoryPath, manager);
        template.setPluginID(configuration.getContributor().getName());
        if (after != null && !after.isEmpty()) {
            template.setAfterID(after);
        }
        if (description != null) {
            template.setDescription(description);
        }
        template.setExpertNode(isExpertNode);
        if (!Boolean.valueOf(
                System.getProperty("java.awt.headless", "false"))) {
            // Load images from declaring plugin
            ImageDescriptor descriptor = null;
            Image icon = null;
            if (iconPath != null) {
                descriptor = AbstractUIPlugin
                    .imageDescriptorFromPlugin(
                            pluginId, iconPath);
            }
            if (descriptor != null) {
                icon = descriptor.createImage();
            }
            // get default image if null
            if (icon == null) {
                icon = ImageRepository.getImage(META_NODE_ICON);
            }
            // FIXME dispose this somewhere !!
            template.setIcon(icon);
        }
        return template;
    }


    private static WorkflowManager loadMetaNode(final String pluginId,
            final String workflowDir) {
        LOGGER.debug("found pre-installed template " + workflowDir);

        Bundle bundle = Platform.getBundle(pluginId);
        URL url = FileLocator.find(bundle, new Path(workflowDir), null);

        if (url != null) {
            try {
                File f = new File(FileLocator.toFileURL(url).getFile());
                LOGGER.debug("meta node template name: " + f.getName());
                WorkflowLoadHelper loadHelper = new WorkflowLoadHelper(true) {
                    /** {@inheritDoc} */
                    @Override
                    public String getDotKNIMEFileName() {
                        return WorkflowPersistorVersion1xx.WORKFLOW_FILE;
                    }
                };
                // don't lock workflow dir
                WorkflowPersistorVersion1xx persistor =
                    WorkflowManager.createLoadPersistor(f, loadHelper);

                WorkflowManager metaNode = META_NODE_ROOT.load(persistor,
                        new ExecutionMonitor(), false).getWorkflowManager();
                return metaNode;
            } catch (CanceledExecutionException cee) {
                LOGGER.error("Unexpected canceled execution exception",
                        cee);
            } catch (Exception e) {
                LOGGER.error(
                        "Failed to load meta workflow repository", e);
            }
        }
        return null;
    }



    /**
     * Creates a new category object. Throws an exception, if this fails
     *
     * @param root The root to insert the category in
     * @param element Configuration element from the contributing plugin
     * @return Category object to be used within the repository.
     * @throws IllegalArgumentException If the element is not compatible (e.g.
     *             wrong attributes)
     */
    public static Category createCategory(final Root root,
            final IConfigurationElement element) {
        String id = element.getAttribute("level-id");

        // get the id of the contributing plugin
        String pluginID = element.getDeclaringExtension()
            .getNamespaceIdentifier();

        Category cat = new Category(id);
        cat.setPluginID(pluginID);
        cat.setDescription(str(element.getAttribute("description"), ""));
        cat.setName(str(element.getAttribute("name"), "!name is missing!"));
        cat.setAfterID(str(element.getAttribute("after"), ""));
        if (!Boolean.valueOf(System.getProperty(
                "java.awt.headless", "false"))) {
            ImageDescriptor descriptor = getIcon(pluginID,
                    element.getAttribute("icon"));
            cat.setIcon(descriptor.createImage(true));
            cat.setIconDescriptor(descriptor);
        }
        String path = str(element.getAttribute("path"), "/");
        cat.setPath(path);

        //
        // Insert in proper location, create all categories on the path
        // if not already there
        //
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        // split the path
        String[] segments = path.split("/");
        // path so far
        String pathSoFar = "";
        // start at root
        IContainerObject container = root;
        IContainerObject child = null;

        for (int i = 0; i < segments.length; i++) {

            pathSoFar += "/" + segments[i];

            IRepositoryObject obj = container.getChildByID(segments[i], false);
            if (obj == null) {
                throw new IllegalArgumentException("The segment '"
                        + segments[i] + "' in path '" + path
                        + "' does not exist!");
            }

            child = (IContainerObject)obj;

            // if we have found a category (root will be skipped) ....
            if (child instanceof Category) {
                Category category = (Category)child;
                if (category == null) {

                    // should not be null. unknown paths are not allowed!!
                    throw new IllegalArgumentException("The segment '"
                            + segments[i] + "' in path '" + path
                            + "' does not exist!");
                    // // ASSERT: the segment is not empty
                    // assert (segments[i] != null)
                    // && (!segments[i].trim().equals(""));
                    //
                    // //
                    // // Create a new category, set all fields to defaults
                    // where
                    // // appropriate.
                    //
                    // // the segment is the id of this new category
                    // category = new Category(segments[i]);
                    // category.setName(segments[i]);
                    // category.setPath(pathSoFar);
                    // // this loads the default icon
                    // category.setIcon(KNIMERepositoryPlugin.getDefault()
                    // .getImage(pluginID, ""));
                    //
                    // // add this category to the current container
                    // container.addChild(category);
                }
            }
            // continue at this level
            container = child;
        }

        // append the newly created category to the container
        container.addChild(cat);

        return cat;
    }

    //
    // little helper, returns a default if s==null
    private static String str(final String s, final String defaultString) {
        return s == null ? defaultString : s;
    }

    private static ImageDescriptor getIcon(final String pluginID,
            final String path) {
        if (path != null && pluginID != null) {
            ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(
                    pluginID, path);
            if (desc != null) {
                return desc;
            }
        }
        // if we have not returned an image yet we have to return the default
        // icon. lazy initialization
        if (defaultIcon == null) {
            defaultIcon = AbstractUIPlugin.imageDescriptorFromPlugin(
                    KNIMERepositoryPlugin.PLUGIN_ID, "icons/knime_default.png");
        }
        return defaultIcon;
    }

}
