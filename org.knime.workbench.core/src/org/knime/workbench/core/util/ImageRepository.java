/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2012
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
 * ---------------------------------------------------------------------
 *
 * History
 *   20.06.2012 (meinl): created
 */
package org.knime.workbench.core.util;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.workbench.core.KNIMECorePlugin;

/**
 * Central repository for all KNIME-related images. It stores images for nodes,
 * categories, and images used in the GUI. All images are stored in a central
 * {@link ImageRegistry} so that they get disposed automatically when the
 * plug-in is deactivated (which it probably never will).
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.6
 */
public class ImageRepository {
    private static final NodeLogger logger = NodeLogger
            .getLogger(ImageRepository.class);

    private static final ImageRegistry registry = KNIMECorePlugin.getDefault()
            .getImageRegistry();

    /**
     * Enumeration for shared images.
     *
     * @author Thorsten Meinl, University of Konstanz
     * @since 2.6
     */
    public enum SharedImages {
        /** Small icon for export wizards. */
        ExportSmall("icons/knime_export16.png"),
        /** Big icon for export wizards. */
        ExportBig("icons/knime_export55.png"),
        /** Big icon for import wizards. */
        ImportBig("icons/knime_import55.png"),
        /** The default node icon. */
        DefaultNodeIcon(NodeFactory.getDefaultIcon()),
        /** The default metanode icon. */
        DefaultMetaNodeIcon("icons/meta_nodes/metanode_template.png"),
        /** Disabled icon for metanodes. */
        MetanodeDisabled("icons/meta_nodes/metanode_template_disabled.png"),
        /** Icon for a metanode in the node repository or navigator. */
        MetanodeRepository("icons/meta_nodes/metanode_template_repository.png"),
        /** The default category icon. */
        DefaultCategoryIcon(NodeFactory.getDefaultIcon()),
        /** Icon with a lock. */
        Lock("icons/lockedstate.gif"),
        /** Icon for canceling node or workflow execution. */
        CancelExecution("icons/actions/cancel.gif"),
        /** Icon for configuring a node. */
        ConfigureNode("icons/actions/configure.gif"),
        /** Icon for executing a node or workflow. */
        Execute("icons/actions/execute.gif"),
        /** Icon for opening a node view. */
        OpenNodeView("icons/actions/openView.gif"),
        /** Icon for reseting a node or workflow. */
        Reset("icons/actions/reset.gif"),
        /** Icon for collapsing all levels in a tree. */
        CollapseAll("icons/collapseall.png"),
        /** Icon for expanding all levels in a tree. */
        ExpandAll("icons/expandall.png"),
        /** Icon for the favorite nodes view. */
        FavoriteNodesFolder("icons/fav/folder_fav.png"),
        /** Icon for the most frequently used nodes category. */
        FavoriteNodesFrequentlyUsed("icons/fav/folder_freq.png"),
        /** Icon for the last used nodes category. */
        FavoriteNodesLastUsed("icons/fav/folder_last.png");


        private final URL m_url;

        private SharedImages(final String path) {
            m_url =
                    FileLocator.find(KNIMECorePlugin.getDefault().getBundle(),
                            new Path(path), null);
            if (m_url == null) {
                logger.coding("Cannot find icon for '" + toString() + "' at "
                        + path);
            }
        }

        private SharedImages(final URL url) {
            m_url = url;
        }

        URL getUrl() {
            return m_url;
        }
    }

    /**
     * Returns a shared image.
     *
     * @param image the image
     * @return an image
     */
    public static Image getImage(final SharedImages image) {
        final String key = image.name();
        Image img = registry.get(key);
        if (img != null) {
            return img;
        }
        ImageDescriptor desc = ImageDescriptor.createFromURL(image.getUrl());
        registry.put(key, desc);
        return registry.get(key);
    }

    /**
     * Returns the descriptor for a shared image.
     *
     * @param image the image
     * @return an image descriptor
     */
    public static ImageDescriptor getImageDescriptor(final SharedImages image) {
        final String key = image.name();
        ImageDescriptor desc = registry.getDescriptor(key);
        if (desc != null) {
            return desc;
        }
        desc = ImageDescriptor.createFromURL(image.getUrl());
        registry.put(key, desc);
        return desc;
    }

    /**
     * Returns the icon for the node identified by its factory. If no icon is
     * specified by the node factory the default node icon is returned. If the
     * icon specified by the factory does not exist, the Eclipse default missing
     * icon is returned.
     *
     * @param nodeFactory a node factory
     * @return the node icon
     */
    public static Image getImage(
            final NodeFactory<? extends NodeModel> nodeFactory) {
        final String key = nodeFactory.getClass().getCanonicalName();
        Image img = registry.get(key);
        if (img != null) {
            return img;
        }
        URL iconURL = nodeFactory.getIcon();
        if (iconURL == null) {
            return getImage(SharedImages.DefaultNodeIcon);
        }
        ImageDescriptor desc =
                ImageDescriptor.createFromURL(nodeFactory.getIcon());
        registry.put(key, desc);
        return registry.get(key);
    }

    /**
     * Returns the image descriptor for the node identified by its factory.
     *
     * @param nodeFactory a node factory
     * @return a descriptor for the node icon
     */
    public static ImageDescriptor getImageDescriptor(
            final NodeFactory<? extends NodeModel> nodeFactory) {
        final String key = nodeFactory.getClass().getCanonicalName();
        ImageDescriptor desc = registry.getDescriptor(key);
        if (desc != null) {
            return desc;
        }
        URL iconURL = nodeFactory.getIcon();
        if (iconURL == null) {
            return getImageDescriptor(SharedImages.DefaultNodeIcon);
        }
        desc = ImageDescriptor.createFromURL(nodeFactory.getIcon());
        registry.put(key, desc);
        return desc;

    }

    /**
     * Returns a scaled version of a node icon. If no icon is specified by the
     * node factory the default node icon is returned. If the icon specified by
     * the factory does not exist, the Eclipse default missing icon is returned.
     *
     * @param nodeFactory a node factory
     * @param width the desired icon width
     * @param height the desired icon height
     * @return a potentially scaled image
     */
    public static Image getScaledImage(
            final NodeFactory<? extends NodeModel> nodeFactory,
            final int width, final int height) {
        final String key =
                nodeFactory.getClass().getCanonicalName() + "@" + width + "x"
                        + height;
        Image img = registry.get(key);
        if (img != null) {
            return img;
        }
        if (nodeFactory.getIcon() == null) {
            return getScaledImage(SharedImages.DefaultNodeIcon, width, height);
        }
        Image unscaled = getImage(nodeFactory);
        Image scaled;
        if ((unscaled.getImageData().width != width)
                || (unscaled.getImageData().height != height)) {
            scaled =
                    new Image(Display.getDefault(), unscaled.getImageData()
                            .scaledTo(width, height));
            registry.put(key, scaled);
        } else {
            scaled = unscaled;
        }
        return scaled;
    }

    /**
     * Returns a scaled version of a shared image.
     *
     * @param image the requested image
     * @param width the desired icon width
     * @param height the desired icon height
     * @return a potentially scaled image
     */
    public static Image getScaledImage(final SharedImages image,
            final int width, final int height) {
        final String key = image.name() + "@" + width + "x" + height;
        Image img = registry.get(key);
        if (img != null) {
            return img;
        }
        Image unscaled = getImage(image);
        Image scaled;
        if ((unscaled.getImageData().width != width)
                || (unscaled.getImageData().height != height)) {
            scaled =
                    new Image(Display.getDefault(), unscaled.getImageData()
                            .scaledTo(width, height));
            registry.put(key, scaled);
        } else {
            scaled = unscaled;
        }
        return scaled;
    }

    /**
     * Returns an image descriptor for an "external" image. The image is given
     * by the plug-in an the path relative to the plug-in root.
     *
     * @param pluginID the plug-in's id
     * @param path the path of the image
     * @return an image descriptor
     */
    public static ImageDescriptor getImageDescriptor(final String pluginID,
            final String path) {
        if (path == null) {
            logger.error("Null path passed to getImageDescriptor (pluginID: "
                    + pluginID + ")");
            return ImageDescriptor.getMissingImageDescriptor();
        }

        final String key = "bundle://" + pluginID + "/" + path;
        ImageDescriptor desc = registry.getDescriptor(key);
        if (desc != null) {
            return desc;
        }
        URL url =
                FileLocator.find(Platform.getBundle(pluginID), new Path(path),
                        null);
        desc = ImageDescriptor.createFromURL(url);
        registry.put(key, desc);
        return desc;
    }

    /**
     * Returns an image for an "external" image. The image is given by the
     * plug-in an the path relative to the plug-in root.
     *
     * @param pluginID the plug-in's id
     * @param path the path of the image
     * @return an image
     */
    public static Image getImage(final String pluginID, final String path) {
        if (path == null) {
            logger.error("Null path passed to getImage (pluginID: " + pluginID
                    + ")");
            return registry.get("###MISSING_IMAGE###");
        }

        final String key = "bundle://" + pluginID + "/" + path;
        Image img = registry.get(key);
        if (img != null) {
            return img;
        }
        URL url =
                FileLocator.find(Platform.getBundle(pluginID), new Path(path),
                        null);
        ImageDescriptor desc = ImageDescriptor.createFromURL(url);
        registry.put(key, desc);
        return registry.get(key);
    }
}
