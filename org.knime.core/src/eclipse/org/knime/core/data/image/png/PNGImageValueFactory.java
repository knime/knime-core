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

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.data.v2.ReadValue;
import org.knime.core.data.v2.ValueFactory;
import org.knime.core.data.v2.WriteValue;
import org.knime.core.data.v2.filestore.TableOrFileStoreValueFactory;
import org.knime.core.data.xml.XMLBlobCell;
import org.knime.core.table.access.StructAccess.StructReadAccess;
import org.knime.core.table.access.StructAccess.StructWriteAccess;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectDeserializer;
import org.knime.core.table.schema.VarBinaryDataSpec.ObjectSerializer;

/**
 * A {@link ValueFactory} to (de-)serialize {@link PNGImageValue}s - in the form of {@link PNGImageCell} or
 * {@link PNGImageBlobCell} - in the columnar backend.
 *
 * {@link XMLBlobCell} instances will be written to a {@link FileStore} so that the table serialized by the columnar
 * backend does not become huge immediately. Smaller {@link PNGImageValue}s (= not {@link PNGImageBlobCell}s) are
 * immediately written into the table.
 *
 * @since 5.1
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 */
public class PNGImageValueFactory extends TableOrFileStoreValueFactory<PNGImageValue> {

    /**
     * Create an instance of the ({@link PNGImageValueFactory}
     */
    public PNGImageValueFactory() {
        super(SERIALIZER, DESERIALIZER);
    }

    static ObjectSerializer<PNGImageValue> SERIALIZER = (out, value) -> {
        out.writeInt(value.getImageContent().getByteArrayReference().length);
        out.write(value.getImageContent().getByteArrayReference());
    };

    static ObjectDeserializer<PNGImageValue> DESERIALIZER = (in) -> {

        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return (PNGImageValue)new PNGImageContent(bytes).toImageCell();

    };

    private class PNGImageReadValue extends TableOrFileStoreReadValue implements PNGImageValue {

        /**
         * @param access
         */
        protected PNGImageReadValue(final StructReadAccess access) {
            super(access);
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
        @SuppressWarnings("unchecked")
        @Override
        public PNGImageContent getImageContent() {
            return ((PNGImageValue)getDataCell()).getImageContent();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected DataCell createCell(final PNGImageValue value) {
            return new PNGImageCell(value.getImageContent());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected FileStoreCell createFileStoreCell() {
            return new PNGImageFileStoreCell();
        }
    }

    private class PNGImageWriteValue extends TableOrFileStoreWriteValue {

        protected PNGImageWriteValue(final StructWriteAccess access) {
            super(access);
        }

        @Override
        protected boolean isCorrespondingReadValue(final PNGImageValue value) {
            return value instanceof PNGImageReadValue;
        }

        @Override
        protected FileStoreCell getFileStoreCell(final PNGImageValue value) throws IOException {
            if (value instanceof PNGImageFileStoreCell fsCell) {
                return fsCell;
            }
            return new PNGImageFileStoreCell(createFileStore(), value);
        }

    }

    @Override
    public ReadValue createReadValue(final StructReadAccess access) {
        return new PNGImageReadValue(access);
    }

    @Override
    public WriteValue<PNGImageValue> createWriteValue(final StructWriteAccess access) {
        return new PNGImageWriteValue(access);
    }

    @Override
    protected boolean shouldBeStoredInFileStore(final PNGImageValue value) {
        return (value instanceof PNGImageBlobCell) || (value instanceof PNGImageFileStoreCell);
    }
}
