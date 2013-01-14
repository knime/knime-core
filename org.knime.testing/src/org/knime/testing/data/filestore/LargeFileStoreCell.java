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
 *   Jun 27, 2012 (wiswedel): created
 */
package org.knime.testing.data.filestore;

import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellDataInput;
import org.knime.core.data.DataCellDataOutput;
import org.knime.core.data.DataCellSerializer;
import org.knime.core.data.DataType;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStoreCell;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class LargeFileStoreCell extends FileStoreCell implements LargeFileStoreValue {

    public static final DataType TYPE = DataType.getType(LargeFileStoreCell.class);

    private final long m_seed;

    public static final DataCellSerializer<LargeFileStoreCell> getCellSerializer() {
        return new DataCellSerializer<LargeFileStoreCell>() {

            /** {@inheritDoc} */
            @Override
            public LargeFileStoreCell deserialize(final DataCellDataInput input)
                    throws IOException {
                long seed = input.readLong();
                return new LargeFileStoreCell(seed);
            }

            /** {@inheritDoc} */
            @Override
            public void serialize(final LargeFileStoreCell cell, final DataCellDataOutput output)
                    throws IOException {
                output.writeLong(cell.m_seed);
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public LargeFile getLargeFile() {
        FileStore fs = getFileStore();
        return LargeFile.restore(fs, m_seed);
    }

    /** {@inheritDoc} */
    @Override
    public long getSeed() {
        return m_seed;
    }

    /**
     * @param input
     * @throws IOException */
    LargeFileStoreCell(final long seed) throws IOException {
        m_seed = seed;
    }

    /**
     * @param largeFile */
    public LargeFileStoreCell(final LargeFile largeFile, final long seed) {
        super(largeFile.getFileStore());
        m_seed = seed;
    }


    /** {@inheritDoc} */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        final LargeFileStoreCell odc = (LargeFileStoreCell)dc;
        if (odc.m_seed != m_seed) {
            return false;
        }
        return super.equalsDataCell(odc);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
