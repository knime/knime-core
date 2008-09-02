/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 * 
 * History
 *   11.07.2008 (thiel): created
 */
package org.knime.base.node.preproc.filter.row.rowfilter;

import java.util.Iterator;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * A simple {@link org.knime.base.node.preproc.filter.row.rowfilter.RowFilter}
 * implementation that filters rows containing missing cells.
 * 
 * @author Kilian Thiel, University of Konstanz
 */
public class MissingCellRowFilter extends RowFilter {

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        return inSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        // Nothing to do ...
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException, IncludeFromNowOn {
        if (!hasMissingCells(row)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if given row contains missing cells and returns <code>true</code>,
     * otherwise <code>false</code>.
     * 
     * @param row The row to check for missing cells
     * @return <code>true</code> if row contains missing cells, otherwise 
     * <code>false</code>
     */
    public static final boolean hasMissingCells(final DataRow row) {
        Iterator<DataCell> i = row.iterator();
        while (i.hasNext()) {
            if (i.next().isMissing()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        // Nothing to do ...
    }
}
