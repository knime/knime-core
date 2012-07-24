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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 */
package org.knime.base.node.viz.histogram.impl.interactive;

import java.util.Comparator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValueComparator;


/**
 * Comparator used to sort {@link DataRow}s by the value of the row with the
 * given index.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class RowByColumnComparator implements Comparator<DataRow> {

    private int m_o1Greater = 1;

    private int m_o1Smaller = -1;

    private int m_isEquals = 0;

    private final int m_colIdx;

    private final DataValueComparator m_nativeCellComparator;

    /**
     * Constructor for class RowByColumnComparator.
     *
     * @param colIndx the index of the column which should be used to compare
     *            the given <code>DataRow</code> objects
     * @param cellComp the <code>DataValueComparator</code> used to compare
     *            the cells of the column with the given index
     */
    public RowByColumnComparator(final int colIndx,
            final DataValueComparator cellComp) {
        this.m_colIdx = colIndx;
        this.m_nativeCellComparator = cellComp;
    }

    /**
     * @return the <code>DataValueComparator</code> used to compare the cells
     *         with the index set in the constructor
     */
    public DataValueComparator getBasicComparator() {
        return this.m_nativeCellComparator;
    }

    /**
     * @param o1 row 1
     * @param o2 row 2
     * @return the result of the default comparator for the table cell with
     *         index set in the constructor
     */
    @Override
    public int compare(final DataRow o1, final DataRow o2) {
        if (o1 == o2) {
            return m_isEquals;
        }
        if (o1 == null) {
            return m_o1Smaller;
        }
        if (o2 == null) {
            return m_o1Greater;
        }
        if (o1.getNumCells() < this.m_colIdx
                || o2.getNumCells() < this.m_colIdx) {
            throw new IllegalArgumentException("Column index set in "
                    + "constructor is greater then the row length.");
        }
        DataCell c1 = o1.getCell(this.m_colIdx);
        DataCell c2 = o2.getCell(this.m_colIdx);
        int result = this.m_nativeCellComparator.compare(c1, c2);
        return result;
    }
}
