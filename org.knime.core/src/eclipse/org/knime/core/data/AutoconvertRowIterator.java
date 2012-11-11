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
 * Created on 09.11.2012 by meinl
 */
package org.knime.core.data;

import org.knime.core.data.container.BlobSupportDataRow;
import org.knime.core.data.container.CloseableRowIterator;

/**
 * Decorator for a row iterator that transparently converts a certain column using a {@link DataCellTypeConverter}.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @since 2.7
 */
public class AutoconvertRowIterator extends CloseableRowIterator {
    private final CloseableRowIterator m_iterator;

    private final int m_colIndex;

    /** The user-provided converter that should be used. */
    protected final DataCellTypeConverter m_converter;

    /**
     * Creates a new iterator.
     *
     * @param iterator the base iterator that should be decorated
     * @param colIndex the index of the column that should be auto-converted
     * @param converter the converter to use
     */
    public AutoconvertRowIterator(final CloseableRowIterator iterator, final int colIndex,
                                  final DataCellTypeConverter converter) {
        m_iterator = iterator;
        m_colIndex = colIndex;
        m_converter = converter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        m_iterator.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return m_iterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataRow next() {
        final DataRow inputRow = m_iterator.next();
        final DataCell toConvert = inputRow.getCell(m_colIndex);
        final DataCell converted;
        try {
            converted = m_converter.convert(toConvert);
        } catch (Exception ex) {
            throw new RuntimeException("Error while auto-converting row", ex);
        }

        final boolean blobRow = (inputRow instanceof BlobSupportDataRow);
        final DataCell[] cells = new DataCell[inputRow.getNumCells()];
        for (int i = 0; i < cells.length; i++) {
            if (i == m_colIndex) {
                cells[i] = converted;
            } else {
                cells[i] = blobRow ? ((BlobSupportDataRow)inputRow).getRawCell(i) : inputRow.getCell(i);
            }
        }
        return new BlobSupportDataRow(inputRow.getKey(), cells);
    }
}
