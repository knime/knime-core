/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.data.image.png;

import java.awt.Graphics2D;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.image.ImageContent;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Thomas Gabriel, KNIME.com, Zurich, Switzerland
 */
public class PNGImageContent implements ImageContent {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(PNGImageContent.class);

    private final byte[] m_imageBytes;

    /** Type for PNG cells. */
    public static final DataType TYPE = PNGImageBlobCell.TYPE;

    public PNGImageContent(final byte[] imageBytes) {
        m_imageBytes = imageBytes;
    }

    public PNGImageContent(final InputStream is) {
        m_imageBytes = null; // TODO
    }

    public static PNGImageContent loadImage(final DataInput input)
            throws IOException {
        byte[] bytes = new byte[input.readInt()];
        input.readFully(bytes);
        return new PNGImageContent(bytes);
    }

    public void saveImage(final DataOutput output) throws IOException {
        output.writeInt(m_imageBytes.length);
        output.write(m_imageBytes);
    }

    /**
     * {@inheritDoc}
     */
    public void paint(final Graphics2D g, final int width, final int height) {

    }

    /**
     * {@inheritDoc}
     */
    public DataCell toImageCell() {
        // TODO Auto-generated method stub
        return null;
    }

    /** Minimum size for blobs in bytes. That is, if a given string is at least
     * as large as this value, it will be represented by a blob cell */
    public static final int DEF_MIN_BLOB_SIZE_IN_BYTES = 8 * 1024;

    private static final int MIN_BLOB_SIZE_IN_BYTES;

    /** System's line separator character.
     * System.getProperty("line.separator"); */
    public static final String LINE_SEP;

    static {
        String lineSep;
        try {
            lineSep = System.getProperty("line.separator");
            if (lineSep == null || lineSep.isEmpty()) {
                throw new RuntimeException("line separator must not be empty");
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to get \"line.separator\" from system, "
                    + "using \"\\n\"", e);
            lineSep = "\n";
        }
        LINE_SEP = lineSep;
        int size = DEF_MIN_BLOB_SIZE_IN_BYTES;
        String envVar = "org.knime.sdfminblobsize";
        String property = System.getProperty(envVar);
        if (property != null) {
            String s = property.trim();
            int multiplier = 1;
            if (s.endsWith("m") || s.endsWith("M")) {
                s = s.substring(0, s.length() - 1);
                multiplier = 1024 * 1024;
            } else if (s.endsWith("k") || s.endsWith("K")) {
                s = s.substring(0, s.length() - 1);
                multiplier = 1024;
            }
            try {
                int newSize = Integer.parseInt(s);
                if (newSize < 0) {
                    throw new NumberFormatException("Size < 0" + newSize);
                }
                size = newSize * multiplier;
                LOGGER.debug("Setting min blob size for SDF cells to "
                        + size + " bytes");
            } catch (NumberFormatException e) {
                LOGGER.warn("Unable to parse property " + envVar
                        + ", using default", e);
            }
        }
        MIN_BLOB_SIZE_IN_BYTES = size;
    }

    /** Factory method to create {@link DataCell} representing PNGImage structures.
     * The returned cell is either of type {@link PNGImageCell} (for small strings)
     * or {@link PNGImageBlobCell} (otherwise, default threshold is
     * {@value #DEF_MIN_BLOB_SIZE_IN_BYTES} bytes or larger).
     * @param string String representing the SDF content.
     * @return DataCell representing PNGImage content.
     * @throws NullPointerException If argument is null. */
    public static DataCell create(final byte[] bytes) {
        PNGImageContent content = new PNGImageContent(bytes);
        if (bytes.length >= MIN_BLOB_SIZE_IN_BYTES) {
            return new PNGImageBlobCell(content);
        } else {
            return null; // new PNGImageCell(sdfString, /*ignored*/ true);
        }
    }

}
