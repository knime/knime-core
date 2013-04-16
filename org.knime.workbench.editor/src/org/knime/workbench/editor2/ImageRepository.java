/* 
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
package org.knime.workbench.editor2;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.knime.workbench.KNIMEEditorPlugin;

/**
 * This manages all the (internal) images of the Editor-Plugin. Images are
 * registered given their path relative to the plugin's base. The getter methods
 * automatically try to add an image first, if it's not already registered - so
 * there is no need to explicitly add them before ;-) In fact, you can also
 * cache images that are *not* located in the editor-plugin by adding valid
 * image-descriptors. They must, however, explicitly registered and can only be
 * retrieved using the identical descriptor as a key !
 * 
 * Some common images are listed as public constants for convinience.
 * 
 * @author Florian Georg, University of Konstanz
 */
public final class ImageRepository {
    private ImageRepository() {
        // hidden
    }

    /** Image: editor. */
    public static final String IMAGE_EDITOR = "icons/editor.gif";

    /** Image: connection. */
    public static final String IMAGE_PALETTE_CONNECTION = "icons/"
            + "connection.gif";

    /** Image: description. */
    public static final String IMAGE_PALETTE_DESCRIPTION = "icons/"
            + "description.gif";

    /** Image: in port. */
    public static final String IMAGE_PORT_IN = "icons/port_in.gif";

    /** Image: out port. */
    public static final String IMAGE_PORT_OUT = "icons/port_in.gif";

    /** Image: reader. */
    public static final String IMAGE_TYPE_READER = "icons/palette/reader.gif";

    /** Image: filter. */
    public static final String IMAGE_TYPE_FILTER = "icons/palette/filter.gif";

    /** Image: learner. */
    public static final String IMAGE_TYPE_LEARNER = "icons/palette/learner.gif";

    /** Image: scorer. */
    public static final String IMAGE_TYPE_SCORER = "icons/palette/scorer.gif";

    /** Image: writer. */
    public static final String IMAGE_TYPE_WRITER = "icons/palette/writer.gif";

    /** Image: handler. */
    public static final String IMAGE_TYPE_HANDLER = "icons/palette/handler.gif";

    /** Image: viewer. */
    public static final String IMAGE_TYPE_VIEWER = "icons/palette/viewer.gif";

    /** Image: default for an algorithm. */
    public static final String IMAGE_DEFAULT_ALGORITHM = "icons/alg/"
            + "default.gif";

    // The internal registry
    private static ImageRegistry registry = KNIMEEditorPlugin.getDefault()
            .getImageRegistry();

    // default plugin ID, used to lookup the default "scope"
    private static final String PLUGIN_ID = KNIMEEditorPlugin.PLUGIN_ID;

    /**
     * Adds an image(-descriptor) to the registry.
     * 
     * @param relPath The plugin-relative path to image
     */
    public static void addImage(final String relPath) {
        registry.put(relPath, AbstractUIPlugin.imageDescriptorFromPlugin(
                PLUGIN_ID, relPath));
    }

    /**
     * Adds an image to the registry.
     * 
     * @param iconURL the url of the icon
     */
    public static void addImage(final URL iconURL) {

        Image image = createImageFromURL(iconURL);

        // only add if not null
        if (image != null) {
            registry.put(iconURL.toString(), image);
        }

    }

    /**
     * Returns an Image. If the cache doesn't contain this image, it is added
     * first.
     * 
     * @param key The key
     * @return The cached image,
     */
    public static Image getImage(final String key) {
        if (registry.get(key) == null) {
            addImage(key);
        }

        return registry.get(key);
    }

    /**
     * Returns an scaled Image. If the cache doesn't contain this image, it is
     * added first.
     * 
     * @param iconURL the url of the icon
     * @param width width to which the image should be scaled to
     * @param height height to which the image should be scaled to
     * @return The cached image, should never return <code>null</code>
     */
    public static Image getScaledImage(final URL iconURL, final int width,
            final int height) {

        if (iconURL == null) {
            return null;
        }

        String key = iconURL.toString();
        String finalKey = key + "_y" + width + "_x" + height;

        if (registry.get(finalKey) == null) {
            Image unscaledImg = getImage(iconURL);
            if (unscaledImg != null) {
                Image scaledImage = new Image(Display.getDefault(), unscaledImg
                        .getImageData().scaledTo(width, height));
                registry.put(finalKey, scaledImage);
            }

        }

        return registry.get(finalKey);
    }

    /**
     * Returns an Image.
     * 
     * @param iconURL the url of the icon
     * @return The cached image, or <code>null</code>
     */
    public static Image getImage(final URL iconURL) {
        if (registry.get(iconURL.toString()) == null) {
            addImage(iconURL);
        }

        return registry.get(iconURL.toString());
    }

    private static Image createImageFromURL(final URL iconURL) {
        try {
            return new Image(Display.getDefault(), iconURL.openStream());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns an ImageDescriptor for an relative path.
     * 
     * @param relPath The plugin-relative path to the image
     * @return The cached descriptor, or <code>null</code>
     */
    public static ImageDescriptor getImageDescriptor(final String relPath) {
        if (registry.getDescriptor(relPath) == null) {
            addImage(relPath);
        }

        return registry.getDescriptor(relPath);
    }
}
