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
 *   ${date} (${user}): created
 */
package org.knime.workbench.repository;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.swt.graphics.Image;
import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeModel;
import org.knime.workbench.repository.model.Category;
import org.knime.workbench.repository.model.IContainerObject;
import org.knime.workbench.repository.model.IRepositoryObject;
import org.knime.workbench.repository.model.NodeTemplate;
import org.knime.workbench.repository.model.Root;

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

        // Try to load the node factory class...
        GenericNodeFactory<? extends GenericNodeModel> factory;
        try {

            // this ensures that the class is loaded by the correct eclipse
            // classloaders
            factory =
                    (GenericNodeFactory<? extends GenericNodeModel>)element
                            .createExecutableExtension("factory-class");

            node.setFactory(
                    (Class<GenericNodeFactory<? extends GenericNodeModel>>)
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
                        GenericNodeFactory.getDefaultIcon(), 16, 16);
            }
            // FIXME dispose this somewhere !!
            node.setIcon(icon);
        }

        node.setCategoryPath(str(element.getAttribute("category-path"), "/"));

        return node;
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
            cat.setIcon(KNIMERepositoryPlugin.getDefault().getImage(pluginID,
                    str(element.getAttribute("icon"), "")));
            cat.setIconDescriptor(KNIMERepositoryPlugin.getDefault()
                    .getImageDescriptor(pluginID,
                            str(element.getAttribute("icon"), "")));
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
}
