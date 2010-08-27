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
 */
package org.knime.base.node.parallel.appender;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;

/**
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public interface ExtendedCellFactory {
    /**
     * Get the new cells for a given row. These cells are incorporated into the 
     * existing row. The way it is done is defined through the ColumnRearranger
     * using this object.
     * @param row The row of interest.
     * @return The new cells to that row.
     * @throws IllegalArgumentException  If there is no mapping available.
     */
    DataCell[] getCells(final DataRow row);
    
    /**
     * The column specs for the cells that are generated in the getCells()
     * method. This method is only called once, there is no need to cache
     * the return value. The length of the returned array must match the 
     * length of the array returned by the getCells(DataRow) method and also
     * the types must match, i.e. the type of the respective DataColumnSpec
     * must be of the same type or a syper type of the cell as returned
     * by getCells(DataRow).
     * @return The specs to the newly created cells.
     */
    DataColumnSpec[] getColumnSpecs();    
    
    /**
     * Returns an array of column actions that describe for each column that
     * is returned by {@link #getCells(org.knime.core.data.DataRow)}
     * where is should be inserted into the output table.
     * 
     * @return an array of column actions
     */
    public ColumnDestination[] getColumnDestinations();
}
