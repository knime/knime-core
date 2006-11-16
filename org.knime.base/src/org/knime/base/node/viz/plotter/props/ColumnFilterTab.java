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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.props;

import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ColumnFilterTab extends PropertiesTab {
    private final ColumnFilterPanel m_columnSelection;
    
    
    /**
     * Creates the default properties and a tab for mutli column selection.
     * @param allowedDataTypes allowed data types
     */
    public ColumnFilterTab(
            final Class<? extends DataValue>...allowedDataTypes) {
        super();
        m_columnSelection = new ColumnFilterPanel(allowedDataTypes);
        add(m_columnSelection);
    }
    
    /**
     * Updates the column filtering with a new {@link DataColumnSpec}.
     * @param spec the data table spec.
     * @param selected the former selected columns.
     */
    public void updateColumnSelection(final DataTableSpec spec, 
            final Set<String> selected) {
        m_columnSelection.update(spec, false, selected);
    }
    
    /**
     * Returns the column filter panel.
     * @return the column filter panel.
     */
    public ColumnFilterPanel getColumnFilter() {
        return m_columnSelection;
    }
    
    /**
     * 
     * @see org.knime.base.node.viz.plotter.props.PropertiesTab#getDefaultName()
     */
    @Override
    public String getDefaultName() {
        return "Column Selection";
    }

}
