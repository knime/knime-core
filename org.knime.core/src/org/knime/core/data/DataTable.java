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
 * --------------------------------------------------------------------- *
 * 
 * History
 *   09.01.2006(all): reviewed
 *   29.10.2006(tm, cs): reviewed
 */
package org.knime.core.data;

/**
 * Most general data interface in table structure with a fixed number of columns
 * and iterable rows (no random access).
 * 
 * <p>
 * Each <code>DataTable</code> is a read-only container of {@link DataRow}
 * elements which are returned by the {@link RowIterator}. Each row must have
 * the same number of {@link DataCell} elements (columns), is read-only, and
 * must provide a unique row identifier. A table also contains a
 * {@link DataTableSpec} member which provides information about the structure
 * of the table. The {@link DataTableSpec} consists of {@link DataColumnSpec}s
 * which contain information about the column, e.g. name, type, and possible
 * values etc.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see DataCell
 * @see DataRow
 * @see RowIterator
 */
public interface DataTable extends Iterable<DataRow> {

    /**
     * Returns the {@link DataTableSpec} object of this table which gives
     * information about the structure of this data table.
     * 
     * @return the DataTableSpec of this table
     */
    DataTableSpec getDataTableSpec();

    /**
     * Returns a row iterator which returns each row one-by-one from the table.
     * 
     * @return row iterator
     * 
     * @see org.knime.core.data.DataRow
     */
    RowIterator iterator();

}
