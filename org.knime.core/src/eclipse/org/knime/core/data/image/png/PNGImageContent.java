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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.data.image.png;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataType;
import org.knime.core.data.image.ImageContent;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.util.FileUtil;

/**
 * Content of a PNG image. It only wraps a byte[] which is supposed to be
 * PNG content. The rendering methods will delegate all work to
 * {@link BufferedImage}.
 * @author Thomas Gabriel, KNIME.com, Zurich, Switzerland
 */
public class PNGImageContent implements ImageContent {

    /** Type for PNG cells. */
    public static final DataType TYPE = DataType.getType(PNGImageCell.class);

    /** PNG image content as byte array. */
    private byte[] m_imageBytes;

    private SoftReference<Image> m_imageRef;

    /** Framework constructor for restoring content. <b>Do not use!</b> */
    public PNGImageContent() {
        // no-arg, required by ImageContent
    }

    /** Creates PNG image content from byte array.
     * @param imageBytes The image bytes.
     * @throws NullPointerException If the argument is null.
     * @throws IllegalArgumentException If the argument does not represent a
     * valid png byte stream (according to {@link ImageIO#read(InputStream)}.
     */
    public PNGImageContent(final byte[] imageBytes) {
        if (imageBytes == null) {
            throw new NullPointerException("Argument must not be null.");
        }
        m_imageBytes = imageBytes;
        m_imageRef = new SoftReference<Image>(getImageInternal(imageBytes));
    }

    /** Reads image content from a stream. The reader will read content
     * until the end of the stream, it will not close the stream.
     *
     * @param is The input stream.
     * @throws IOException If reading from the stream fails.
     * @throws NullPointerException If the argument is null;
     * @throws IllegalArgumentException If the argument does not represent a
     * valid png byte stream (according to {@link ImageIO#read(InputStream)}.
     */
    public PNGImageContent(final InputStream is) throws IOException {
        this(toByteArray(is));
    }

    private static final byte[] toByteArray(final InputStream in)
        throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.copy(in, out);
        out.close();
        // do not close in stream, see constructor doc
        return out.toByteArray();
    }

    /** Get a reference to the underlying byte array. The caller must not
     * modify the returned array but should use the {@link #getByteArray()}
     * if necessary.
     * @return Reference to the underlying byte array.
     */
    public byte[] getByteArrayReference() {
        return m_imageBytes;
    }

    /** Get a copy of the underlying byte array.
     * @return A new copy.
     * @see #getByteArrayReference()
     */
    public byte[] getByteArray() {
        return Arrays.copyOf(m_imageBytes, m_imageBytes.length);
    }

    /** Get the image represented by this object.
     * @return The image.
     * @throws IllegalStateException If the image can't be read from the
     *         internal memory representation (the Image is not actually stored
     *         as part of this cell but kept in a SoftReference)
     */
    public Image getImage() {
        Image image = m_imageRef.get();
        if (image != null) {
            return image;
        }
        try {
            image = getImageInternal(m_imageBytes);
            m_imageRef = new SoftReference<Image>(image);
            return image;
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Image can't be re-read", e);
        }
    }

    /** Read image from byte[] array.
     * @return A new image
     */
    private static Image getImageInternal(final byte[] array) {
        try {
            BufferedImage bufImage =
                ImageIO.read(new ByteArrayInputStream(array));
            if (bufImage == null) {
                throw new IllegalArgumentException(
                        "ImageIO returned null while reading image bytes");
            } else {
                return bufImage;
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Image can't be read", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void paint(final Graphics2D g, final int width, final int height) {
        BufferedImage image = null;
        String error = null;
        try {
            image = ImageIO.read(new ByteArrayInputStream(m_imageBytes));
            if (image == null) {
                error = "ImageIO returned null";
            }
        } catch (IOException e) {
            error = e.getMessage();
        }
        if (error != null) {
            g.drawString(error, 0, 0);
        } else {
            g.drawImage(image, 0, 0, width, height, null);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void save(final OutputStream output) throws IOException {
        output.write(m_imageBytes);
    }

    /** Deserialize method for DataCell implementation.
     * @param input To read from.
     * @return A new image content.
     * @throws IOException If that fails.
     */
    static PNGImageContent deserialize(final DataCellDataInput input)
        throws IOException {
        int length = input.readInt();
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new PNGImageContent(bytes);
    }

    /** Serialize method for image content.
     * @param output To save to.
     * @throws IOException If that fails for any reason.
     */
    public void serialize(final DataCellDataOutput output) throws IOException {
        output.writeInt(m_imageBytes.length);
        output.write(m_imageBytes);
    }

    /** Minimum size for blobs in bytes. That is, if a given byte[] is at least
     * as large as this value, it will be represented by a blob cell */
    private static final long BLOB_SIZE_THRESHOLD =
        ConvenienceMethods.readSizeSystemProperty(
                "org.knime.pngminblobsize", 40 * 1024);

    /** {@inheritDoc} */
    @Override
    public DataCell toImageCell() {
        if (m_imageBytes.length < BLOB_SIZE_THRESHOLD) {
            return new PNGImageBlobCell(this);
        } else {
            return new PNGImageCell(this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Dimension getPreferredSize() {
        Image image;
        try {
            image = getImage();
        } catch (IllegalStateException ise) {
            return new Dimension(16, 16);
        }
        return new Dimension(image.getWidth(null), image.getHeight(null));
    }

    /** {@inheritDoc} */
    @Override
    public String getSummary() {
        Dimension dim = getPreferredSize();
        String summary = "PNG Image " + dim.width + " x "
            + dim.height + " with ";
        if (m_imageBytes.length < 1000) {
            return summary + m_imageBytes.length + " B";
        } else {
            return summary + (m_imageBytes.length / 1024) + " KB";
        }
    }

}
