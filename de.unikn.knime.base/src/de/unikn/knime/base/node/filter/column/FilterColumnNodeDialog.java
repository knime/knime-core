/* --------------------------------------------------------------------- *
 *   This source code, its documentation and all appendant files         *
 *   are protected by copyright law. All rights reserved.                *
 *                                                                       *
 *   Copyright, 2003 - 2006                                              *
 *   Universitaet Konstanz, Germany.                                     *
 *   Lehrstuhl fuer Angewandte Informatik                                *
 *   Prof. Dr. Michael R. Berthold                                       *
 *                                                                       *
 *   You may not modify, publish, transmit, transfer or sell, reproduce, *
 *   create derivative works from, distribute, perform, display, or in   *
 *   any way exploit any of the content, in whole or in part, except as  *
 *   otherwise expressly permitted in writing by the copyright owner.    *
 * --------------------------------------------------------------------- *
 */
package de.unikn.knime.base.node.filter.column;

import java.util.HashSet;
import java.util.Set;

import de.unikn.knime.base.node.util.FilterColumnPanel;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeSettings;
import de.unikn.knime.core.node.NotConfigurableException;

/**
 * This is the dialog for the column filter. The user can specify
 * which columns should be excluded in the output table.
 * 
 * @author Christoph Sieb, University of Konstanz
 * @author Thomas Gabriel, University of Konstanz
 */
final class FilterColumnNodeDialog extends NodeDialogPane {

    /*
     * The tab's name.
     */
    private static final String TAB = "Column Filter";
    
    /**
     * Creates a new <code>NodeDialogPane</code> for the column filter in order
     * to set the desired columns. 
     */
    FilterColumnNodeDialog() {
        super("Column Filter Settings");
        super.addTab(TAB, new FilterColumnPanel());
    }

    /**
     * Calls the update method of the underlying filter panel using the input 
     * data table spec from this <code>FilterColumnNodeModel</code>.
     *  
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     * @throws NotConfigurableException If no columns are available for
     *         filtering.
     */
    protected void loadSettingsFrom(
            final NodeSettings settings, final DataTableSpec[] specs) 
            throws NotConfigurableException {
        assert (settings != null && specs.length == 1);
        if (specs[FilterColumnNodeModel.INPORT] == null
                || specs[FilterColumnNodeModel.INPORT].getNumColumns() == 0) {
            throw new NotConfigurableException("No columns available for "
                    + "selection.");
        }
        String[] columns = settings.getStringArray(
                FilterColumnNodeModel.KEY, new String[0]); 
        HashSet<String> list = new HashSet<String>();
        for (int i = 0; i < columns.length; i++) {
            if (specs[FilterColumnNodeModel.INPORT].containsName(columns[i])) {
                list.add(columns[i]);
            }
        }
        // set exclusion list on the panel
        FilterColumnPanel p = (FilterColumnPanel) getTab(TAB);
        p.update(specs[FilterColumnNodeModel.INPORT], true, list);
    } 
    
    /**
     * Sets the list of columns to exclude inside the underlying
     * <code>FilterColumnNodeModel</code> retrieving them from the filter panel.
     * @param settings The <code>NodeSettings</code> to write into.
     *
     * @see NodeDialogPane#saveSettingsTo(NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings) {
        FilterColumnPanel p = 
            (FilterColumnPanel) getTab(TAB);
        Set<String> list = p.getExcludedColumnList();
        settings.addStringArray(FilterColumnNodeModel.KEY, 
                list.toArray(new String[0]));
    }
    
}   // FilterColumnNodeDialog
