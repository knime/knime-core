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
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   Jul 11, 2012 (wiswedel): created
 */
package org.knime.core.data.filestore.internal;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;

/**
 * A data cell used internally to save the file store keys generated in a loop to a buffered data table.
 * This cell is not used in client code.
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class FileStoreKeyDataCell extends DataCell implements FileStoreKeyDataValue {

    public static final DataType TYPE = DataType.getType(FileStoreKeyDataCell.class);

    private final FileStoreKey m_key;

    public static final DataCellSerializer<FileStoreKeyDataCell> getCellSerializer() {
        return new DataCellSerializer<FileStoreKeyDataCell>() {

            @Override
            public void serialize(final FileStoreKeyDataCell cell, final DataCellDataOutput output) throws IOException {
                cell.getKey().save(output);
            }

            @Override
            public FileStoreKeyDataCell deserialize(final DataCellDataInput input) throws IOException {
                return new FileStoreKeyDataCell(FileStoreKey.load(input));
            }
        };
    }

    /**
     * @param key */
    FileStoreKeyDataCell(final FileStoreKey key) {
        m_key = key;
    }

    /** {@inheritDoc} */
    @Override
    public FileStoreKey getKey() {
        return m_key;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return m_key.toString();
    }

    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return ((FileStoreKeyDataCell)dc).m_key.equals(m_key);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return m_key.hashCode();
    }

}
