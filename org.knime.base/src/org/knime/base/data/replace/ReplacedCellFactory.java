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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 * 
 * History
 *   16.01.2006 (cebron): created
 */
package org.knime.base.data.replace;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;

/**
 * Convenience class that should be used if your
 * {@link ReplacedCellsFactory} replaces only
 * one column.
 * 
 * @author Nicolas Cebron, University of Konstanz
 */
public abstract class ReplacedCellFactory implements ReplacedCellsFactory {
    /**
     * Returns an array of length 1 containing the replacement of the data cell
     * at the position given in the first element of the columns array.
     * 
     * @see ReplacedCellsFactory
     *      #getReplacement(org.knime.core.data.DataRow, int[])
     */
    public final DataCell[] getReplacement(final DataRow row,
            final int[] columns) {
        return new DataCell[]{getReplacement(row, columns[0])};
    }

    /**
     * Computes the data cell that should replace the <code>column</code>-th
     * column in the given row. The replacing procedure itself is done in the
     * calling class.
     * 
     * @param row the row carrying the "obsolete" cell
     * @param column the column that is to be replaced
     * @return the value that the serves as replacement
     * @throws IndexOutOfBoundsException if int argument is out of range
     * @throws NullPointerException if row is <code>null</code>
     */
    public abstract DataCell getReplacement(final DataRow row, 
                final int column);
}
