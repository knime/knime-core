/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 *   May 26, 2023 (Jonas Klotz, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.core.data.image.png;

import java.io.DataOutput;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.v2.filestore.AbstractFileStoreSerializableValueFactory.AbstractFileStoreSerializableCell;
import org.knime.core.table.io.ReadableDataInput;

/**
 * @since 5.1
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("serial")
public final class PNGImageFileStoreCell extends AbstractFileStoreSerializableCell implements PNGImageValue {

    private PNGImageContent m_content;

    // cache the hash
    private Integer m_hash = null;

    /**
     * Initialize with an empty file store and content
     *
     * @param fs {@link FileStore} to which the content will be written
     * @param value PNG image value
     */
    PNGImageFileStoreCell(final FileStore fs, final PNGImageValue value) {
        super(fs);
        m_content = contentFromValue(value);

    }

    /**
     * Deserialization constructor, FileStore will be provided by framework
     */
    PNGImageFileStoreCell() {

    }

    /**
     * @param value value for png
     * @return content of image
     */
    private static PNGImageContent contentFromValue(final PNGImageValue value) {
        return value.getImageContent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PNGImageContent getImageContent() {
        return getContentLazily();
    }

    private PNGImageContent getContentLazily() {
        // TODO: don't load immediately
        return m_content;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_content.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        PNGImageFileStoreCell ic = (PNGImageFileStoreCell)dc;
        return m_content.equals(ic.m_content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalContent(final DataValue otherValue) {
        return PNGImageValue.equalContent(this, (PNGImageValue)otherValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if (null == m_hash) {
            m_hash = m_content.hashCode();
        }

        return m_hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getImageExtension() {
        return "png";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void serialize(final DataOutput output) {
        try {
            PNGImageValueFactory.SERIALIZER.serialize(output, (PNGImageValue)m_content.toImageCell());
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save PNG", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void deserialize(final ReadableDataInput input) {
        try {
            m_content = PNGImageValueFactory.DESERIALIZER.deserialize(input).getImageContent();
        } catch (IOException ex) {
            throw new IllegalStateException("Couldn't read PNG", ex);
        }
    }

    /**
     * Serializer for {@link PNGImageFileStoreCell}s
     *
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class PNGImageSerializer implements DataCellSerializer<PNGImageFileStoreCell> {
        @Override
        public void serialize(final PNGImageFileStoreCell cell, final DataCellDataOutput output) throws IOException {
            // Nothing to do, all data is in FileStore
            // TODO: maybe store the hash?
        }

        @Override
        public PNGImageFileStoreCell deserialize(final DataCellDataInput input) throws IOException {
            // Nothing to do, all data is in FileStore
            // TODO: maybe store the hash?
            return new PNGImageFileStoreCell();

        }

    }
}
