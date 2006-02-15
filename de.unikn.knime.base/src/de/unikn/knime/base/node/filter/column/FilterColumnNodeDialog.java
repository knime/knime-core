/*
 * @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * --------------------------------------------------------------------- *
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

import de.unikn.knime.base.node.util.FilterColumnNodeDialogPanel;
import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.node.NodeDialogPane;
import de.unikn.knime.core.node.NodeSettings;

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
        super.addTab(TAB, new FilterColumnNodeDialogPanel());
    }

    /**
     * Calls the update method of the underlying filter panel using the input 
     * data table spec from this <code>FilterColumnNodeModel</code>.
     *  
     * @param settings The <code>NodeSettings</code> to read from.
     * @param specs The input specs.
     */
    protected void loadSettingsFrom(
            final NodeSettings settings, final DataTableSpec[] specs) {
        assert (settings != null && specs.length == 1);
        if (specs[FilterColumnNodeModel.INPORT] == null) {
            // settings can't be evaluated against the spec
            return;
        }
        DataCell[] columns = settings.getDataCellArray(
                FilterColumnNodeModel.KEY, new DataCell[0]); 
        HashSet<DataCell> list = new HashSet<DataCell>();
        for (int i = 0; i < columns.length; i++) {
            if (specs[FilterColumnNodeModel.INPORT].containsName(columns[i])) {
                list.add(columns[i]);
            }
        }
        // set exclusion list on the panel
        FilterColumnNodeDialogPanel p = 
            (FilterColumnNodeDialogPanel) getTab(TAB);
        p.update(specs[FilterColumnNodeModel.INPORT], list, true);
    } 
    
    /**
     * Sets the list of columns to exclude inside the underlying
     * <code>FilterColumnNodeModel</code> retrieving them from the filter panel.
     * @param settings The <code>NodeSettings</code> to write into.
     *
     * @see NodeDialogPane#saveSettingsTo(NodeSettings)
     */
    protected void saveSettingsTo(final NodeSettings settings) {
        FilterColumnNodeDialogPanel p = 
            (FilterColumnNodeDialogPanel) getTab(TAB);
        Set<DataCell> list = p.getExcludedColumnList();
        settings.addDataCellArray(FilterColumnNodeModel.KEY, 
                list.toArray(new DataCell[0]));
    }
    
}   // FilterColumnNodeDialog
