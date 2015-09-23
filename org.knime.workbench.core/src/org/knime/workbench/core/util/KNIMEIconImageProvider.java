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

import java.io.IOException;
import java.net.URL;

import org.eclipse.swt.graphics.ImageData;
import org.knime.core.node.KNIMEConstants;

/**
 * ImageDataProvider for icon images. Always scales provided image to 16x16 or 24/32px depending on system/display zoom.
 * If higher resolution is required the filename must contain the image size (like /tmp/foo_24x24.png and
 * /tmp/foo_32x32.png). If no higher resolution file is provided the 16x16 image is scaled up.
 *
 * @author ohl
 * @since 3.0
 */
public class KNIMEIconImageProvider extends KNIMEImageProvider {

    /**
     * @param baseName provide full path with extension of image file name. Don't include size info if you have
     *            different sizes. E.g. specify /tmp/foo.png if your image is stored in files /tmp/foo@1x.png and
     *            /tmp/foo@1.5x.png and /tmp/foo@2x.png. If you have only one size, then just specify that one file.
     * @throws IOException if the specified image file (or/and derived file names) doesn't exist.
     */
    public KNIMEIconImageProvider(final String baseName) throws IOException {
        super(baseName);
    }

    /**
     * See {@link #KNIMEIconImageProvider(String)}.
     * @param baseName
     * @throws IOException
     */
    public KNIMEIconImageProvider(final URL baseName) throws IOException {
        super(baseName);
    }

    @Override
    public ImageData getImageData(final int zoom) {
        ImageData img = super.getImageData(zoom);
        // icons have a fixed size
        if (zoom < 150 || !Boolean.getBoolean(KNIMEConstants.PROPERTY_HIGH_DPI_SUPPORT)) {
            return scaleImageTo(16, img);
        } else if (zoom < 200) {
            return scaleImageTo(24, img);
        } else {
            return scaleImageTo(32, img);
        }
    }

}
