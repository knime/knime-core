/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   19.08.2015 (ohl): created
 */
package org.knime.workbench.core.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.util.ViewUtils;

/**
 * ImageDataProvider for images. Scales provided image up if system/display zoom requires. If files with appropriate
 * names are provided the content of these is returned for higher resolutions: Eclipse conventions is to add the
 * zoom level like e.g. /tmp/foo.png for the original size (zoom 100%), /tmp/foo@1.5x.png for zoomlevel 150% and
 * /tmp/foo@2xpng for 200%. If no higher resolution file is provided the original file image is scaled up. If higher
 * resolution files exist, they will not be scaled (but assumed that they have the appropriate size).
 *
 * @author ohl
 * @since 3.0
 */
public class KNIMEImageProvider implements ImageDataProvider {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(KNIMEImageProvider.class);

    private final URL m_img_100;

    private URL m_img_150;

    private URL m_img_200;

    /** can be used as a placeholder. */
    public static final MissingImageProvider MISSING_IMAGE_DATA = new MissingImageProvider();

    /**
     * a small red icon.
     */
    public static final class MissingImageProvider implements ImageDataProvider {
        /**
         * {@inheritDoc}
         */
        @Override
        public ImageData getImageData(final int zoom) {
            return new ImageData(6 * zoom / 100, 6 * zoom / 100, 1, new PaletteData(new RGB[]{new RGB(255, 0, 0)}));
        }
    }

    /**
     * @param baseName provide full path with extension of image file name. Don't include size info if you have
     *            different sizes. E.g. specify /tmp/foo.png if your image is stored in files /tmp/foo@1x.png and
     *            /tmp/foo@1.5x.png and /tmp/foo@2x.png. If you have only one size, then just specify that one file.
     * @throws IOException if the specified image file (or/and derived file names) doesn't exist.
     */
    public KNIMEImageProvider(final String baseName) throws IOException {
        this(new File(baseName).toURI().toURL());
    }

    /**
     * See {@link #KNIMEImageProvider(String)}.
     * @param baseName
     * @throws IOException
     */
    public KNIMEImageProvider(final URL baseName) throws IOException {
        m_img_100 = baseName;
    }

    /**
     * Scales it so that the largest side (width or height) is of specified size
     *
     * @param size
     * @param img
     * @return scaled image data
     */
    protected ImageData scaleImageTo(final int size, final ImageData img) {
        int max = Math.max(img.width, img.height);
        if (max == size) {
            return img;
        }
        double f = size / (double)max;
        return img.scaledTo((int)(img.width * f), (int)(img.height * f));
    }

    /**
     * @param oldURL
     * @param newPath
     * @return replace the path in the url with the provided new path
     * @throws MalformedURLException
     */
    protected URL replacePath(final URL oldURL, final String newPath) throws MalformedURLException {
        String path = newPath;
        if (oldURL.getQuery() != null) {
            path += "?" + oldURL.getQuery();
        }
        if (oldURL.getRef() != null) {
            path += "#" + oldURL.getRef();
        }
        return new URL(oldURL.getProtocol(), oldURL.getHost(), oldURL.getPort(), path);
    }

    /**
     * @return the stream to the provided image file or the file with the 100% zoom extension.
     * @throws IOException if non of the files exist
     */
    protected InputStream open100() throws IOException {
        URL loc = m_img_100;
        URL f = FileLocator.find(m_img_100);
        if (f != null) {
            loc = f;
        }
        try {
            return new BufferedInputStream(loc.openStream());
        } catch (IOException e) {
            // try adding the 100% extension
            String path100 = ViewUtils.getImageFile100(loc.getPath());
            try {
                loc = replacePath(loc, path100);
            } catch (MalformedURLException mfe) {
                LOGGER.coding("Error creating the URL for the image " + path100, mfe);
                throw new IOException(mfe);
            }
            return new BufferedInputStream(loc.openStream());
        }
    }

    /**
     * @return the stream to the image file for system zoom level 150% - or 100% if the 150% doesn't exist. 100% image
     * must be scaled up then.
     * @throws IOException if non of the files exist
     */
    protected InputStream open150() throws IOException {
        if (m_img_150 == null) {
            String path = ViewUtils.getImageFile150(m_img_100.getPath());
            URL loc;
            try {
                loc = replacePath(m_img_100, path);
            } catch (MalformedURLException mfe) {
                LOGGER.coding("Error creating the URL for the image " + path, mfe);
                throw new IOException(mfe);
            }
            URL f = FileLocator.find(loc);
            if (f != null) {
                loc = f;
            }
            m_img_150 = loc;
            try {
                return new BufferedInputStream(m_img_150.openStream());
            } catch (IOException e) {
                // no 150% image available - use the 100% and scale it up
                m_img_150 = m_img_100;
            }
        }
        return new BufferedInputStream(m_img_150.openStream());
    }

    /**
     * @return the stream to the image file for system zoom level 200% - or 100% if the 200% doesn't exist. 100% image
     * must be scaled up then.
     * @throws IOException if non of the files exist
     */
    protected InputStream open200() throws IOException {
        if (m_img_200 == null) {
            String path = ViewUtils.getImageFile200(m_img_100.getPath());
            URL loc;
            try {
                loc = replacePath(m_img_100, path);
            } catch (MalformedURLException mfe) {
                LOGGER.coding("Error creating the URL for the image " + path, mfe);
                throw new IOException(mfe);
            }
            URL f = FileLocator.find(loc);
            if (f != null) {
                loc = f;
            }
            m_img_200 = loc;
            try {
                return new BufferedInputStream(m_img_200.openStream());
            } catch (IOException e) {
                // no 200% image available - use the 100% and scale it up
                m_img_200 = m_img_100;
            }
        }
        return new BufferedInputStream(m_img_200.openStream());
    }

    @Override
    public ImageData getImageData(final int zoom) {
        if (zoom < 150 || !Boolean.getBoolean(KNIMEConstants.PROPERTY_HIGH_DPI_SUPPORT)) {
            try (InputStream i = open100()) {
                ImageData img = new ImageData(i);
                return img;
            } catch (IOException e) {
                LOGGER.coding("Error reading image: " + e.getMessage(), e);
                int z =  Boolean.getBoolean(KNIMEConstants.PROPERTY_HIGH_DPI_SUPPORT) ? zoom : 100;
                return MISSING_IMAGE_DATA.getImageData(z);
            }
        } else if (zoom < 200) {
            try (InputStream i = open150()) {
                ImageData img = new ImageData(i);
                if (m_img_150 != m_img_100) {
                    return img;
                } else {
                    return scaleImageTo((int)(Math.max(img.width, img.height) * 1.5), img);
                }
            } catch (IOException e) {
                LOGGER.coding("Error reading image: " + e.getMessage(), e);
                return MISSING_IMAGE_DATA.getImageData(zoom);
            }
        } else {
            try (InputStream i = open200()) {
                ImageData img = new ImageData(i);
                if (m_img_200 != m_img_100) {
                    return img;
                } else {
                    return scaleImageTo((int)(Math.max(img.width, img.height) * 2.0), img);
                }
            } catch (IOException e) {
                LOGGER.coding("Error reading image: " + e.getMessage(), e);
                return MISSING_IMAGE_DATA.getImageData(zoom);
            }
        }
    }
}
