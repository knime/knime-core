/* -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   04.07.2005 (ohl): created
 */
package de.unikn.knime.base.node.filter.row.rowfilter;

import de.unikn.knime.core.data.DataRow;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.InvalidSettingsException;
import de.unikn.knime.core.node.NodeSettingsRO;
import de.unikn.knime.core.node.NodeSettingsWO;

/**
 * Row filter that doesn't match any row. Not really usefull - but used if the
 * user absolutly wants it.
 * 
 * @author ohl, University of Konstanz
 */
public class FalseRowFilter extends RowFilter {

    /**
     * @see RowFilter#matches(DataRow, int)
     */
    @Override
    public boolean matches(final DataRow row, final int rowIndex)
            throws EndOfTableException {
        // we can immediately tell that the end of the table is reached.
        throw new EndOfTableException();
    }

    /**
     * @see RowFilter#loadSettingsFrom(NodeSettingsRO)
     */
    @Override
    public void loadSettingsFrom(final NodeSettingsRO cfg)
            throws InvalidSettingsException {
        // no settings to load.
    }

    /**
     * @see RowFilter#saveSettings(NodeSettingsWO)
     */
    @Override
    protected void saveSettings(final NodeSettingsWO cfg) {
        // no settings to save.
    }

    /**
     * @see de.unikn.knime.base.node.filter.row.rowfilter.RowFilter
     *      #configure(de.unikn.knime.core.data.DataTableSpec)
     */
    @Override
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        return inSpec;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "FALSE-Filter";
    }

}
