/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2009
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
 *
 * History
 *    22.11.2007 (Tobias Koetter): created
 */

package org.knime.base.node.preproc.setoperator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.def.StringCell;

import java.util.Comparator;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class SingleColRowComparator implements Comparator<DataRow> {

    private int m_colIdx = -1;
    private Comparator<DataCell> m_comp;

    /**Constructor for class SingleColRowComparator.
     * @param colIdx the index of the column to compare or -1 if the RowID
     * should be used
     * @param comp the {@link Comparator} to use
     */
    public SingleColRowComparator(final int colIdx,
            final DataValueComparator comp) {
        setColumnIndex(colIdx);
        setComparator(comp);

    }

    /**
     * @param comp the {@link Comparator} to use
     */
    public void setComparator(final Comparator<DataCell> comp) {
        if (comp == null) {
            throw new NullPointerException("Comparator must not be null");
        }
        m_comp = comp;
    }

    /**
     * @return the used {@link Comparator}
     */
    public Comparator<DataCell> getComparator() {
        return m_comp;
    }

    /**
     * @param colIdx the colIdx to set or -1 if the RowID should be used
     */
    public void setColumnIndex(final int colIdx) {
//        if (colIdx < 0) {
//            throw new IllegalArgumentException("Row index must be positive");
//        }
        m_colIdx = colIdx;
    }

    /**
     * {@inheritDoc}
     */
    public int compare(final DataRow r1, final DataRow r2) {
        if (m_colIdx < 0) {
            final String key1 = r1.getKey().getString();
            final String key2 = r2.getKey().getString();
            return compare(key1, key2, m_comp);
        } else {
            final DataCell cell1 = r1.getCell(m_colIdx);
            final DataCell cell2 = r2.getCell(m_colIdx);
            return compare(cell1, cell2, m_comp);
        }
    }

    /**
     * @param key1
     * @param key2
     * @param comp
     * @return
     */
    private int compare(final String c1, final String c2,
            final Comparator<DataCell> comp) {
        if (c1 == c2) {
            return 0;
        }
        if (c1 == null) {
            return 1;
        }
        if (c2 == null) {
            return -1;
        }
        return comp.compare(new StringCell(c1), new StringCell(c2));
    }

    /**
     * @param c1 first cell to compare
     * @param c2 second cell to compare
     * @param comp the comparator to use
     * @return a negative integer, zero, or a positive integer as the
     *         first argument is less than, equal to, or greater than the
     *         second.
     */
    public static int compare(final DataCell c1, final DataCell c2,
            final Comparator<DataCell> comp) {
        if (c1 == c2) {
            return 0;
        }
        if (c1 == null) {
            return 1;
        }
        if (c2 == null) {
            return -1;
        }
        return comp.compare(c1, c2);
    }
}
