/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * -------------------------------------------------------------------
 * 
 * History
 *   26.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.columns;

import java.util.Set;

import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * This class adds to the default properties 
 * ({@link org.knime.base.node.viz.plotter.AbstractPlotterProperties}) a column
 * filter and provides the possibility to update the columns displayed in this
 * column filter. If the default constructor is used all columns (no matter what
 * {@link org.knime.core.data.DataValue}s they have are displayed). 
 * But the displayed columns can be restricted for certain 
 * {@link org.knime.core.data.DataValue}s. To get informed about changes in the 
 * selected columns the {@link org.knime.core.node.util.ColumnFilterPanel} can
 * be retrieved and listeners can be added to it. The 
 * {@link org.knime.core.node.util.ColumnFilterPanel} gets the information 
 * about the available columns and their {@link org.knime.core.data.DataValue}s
 * from a {@link org.knime.core.data.DataTableSpec}.       
 * 
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class MultiColumnPlotterProperties extends AbstractPlotterProperties {

    
    private final ColumnFilterPanel m_columnSelection;
    
    /** The index for the column filter tab. */
    public static final int COLUMN_FILTER_IDX = 1;
    
    /**
     * Creates the default properties and a tab for mutli column selection. 
     * All {@link org.knime.core.data.DataValue}s are allowed.
     * 
     */
    @SuppressWarnings("unchecked")
    public MultiColumnPlotterProperties() {
        this(NominalValue.class, DoubleValue.class);
    }
    
    
    /**
     * Creates the default properties and a tab for mutli column selection. Only
     * those columns are displayed, which are compatible with the passed
     * <code>allowedDataTypes</code>.
     * 
     * @param allowedDataTypes allowed data types
     */
    public MultiColumnPlotterProperties(
            final Class<? extends DataValue>...allowedDataTypes) {
        super();
        m_columnSelection = new ColumnFilterPanel(allowedDataTypes);
        addTab("Column Selection", m_columnSelection);
    }
    
    /**
     * Updates the column filtering with a new 
     * {@link org.knime.core.data.DataColumnSpec}. After this only those columns
     * existent in the {@link org.knime.core.data.DataColumnSpec} and 
     * compatible with the {@link org.knime.core.data.DataValue}s defined in
     * the constructor asre displayed.
     * 
     * @param spec the data table spec.
     * @param selected the former selected columns.
     */
    public void updateColumnSelection(final DataTableSpec spec, 
            final Set<String> selected) {
        m_columnSelection.update(spec, false, selected);
    }
    
    /**
     * Returns the column filter panel so, for example, listeners can be added.
     * @return the column filter panel.
     */
    public ColumnFilterPanel getColumnFilter() {
        return m_columnSelection;
    }
}
