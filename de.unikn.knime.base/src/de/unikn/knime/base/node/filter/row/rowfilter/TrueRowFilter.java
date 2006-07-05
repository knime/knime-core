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
import de.unikn.knime.core.node.NodeSettings;

/**
 * Row filter that always matches all rows. Not really usefull - but used if the
 * user absolutly wants it.
 * 
 * @author ohl, University of Konstanz
 */
public class TrueRowFilter extends RowFilter {

    /**
     * @see RowFilter#matches(DataRow, int)
     */
    public boolean matches(final DataRow row, final int rowIndex)
            throws IncludeFromNowOn {
        // we can immediately tell that we wanna include all rows.
        throw new IncludeFromNowOn();
    }

    /**
     * @see RowFilter#loadSettingsFrom(NodeSettings)
     */
    public void loadSettingsFrom(final NodeSettings cfg)
            throws InvalidSettingsException {
        // no settings to load.
    }

    /**
     * @see RowFilter#saveSettings(NodeSettings)
     */
    protected void saveSettings(final NodeSettings cfg) {
        // no settings to save.
    }

    /**
     * @see de.unikn.knime.base.node.filter.row.rowfilter.RowFilter
     *      #configure(de.unikn.knime.core.data.DataTableSpec)
     */
    public DataTableSpec configure(final DataTableSpec inSpec)
            throws InvalidSettingsException {
        return inSpec;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "TRUE-Filter";
    }

}
