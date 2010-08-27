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
