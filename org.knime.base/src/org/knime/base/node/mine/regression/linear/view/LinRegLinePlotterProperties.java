/* 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
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
 */
package org.knime.base.node.mine.regression.linear.view;

import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.List;

import org.knime.base.node.viz.plotter.scatter.ScatterPlotterProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NotConfigurableException;

/**
 * Properties for the {@link LinRegLinePlotter} which ensures
 * that the y axis is fixed to the target column and the x column selection box
 * only contains the columns used for the model calculation.
 * 
 * @author Fabian Dill, University of Konstanz
 * 
 */
public class LinRegLinePlotterProperties extends ScatterPlotterProperties {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(LinRegLinePlotterProperties.class);

    private String m_targetColumn;

    private String[] m_includs;

    /**
     * Creates the same properties as the {@link ScatterPlotterProperties} but
     * with different functionality. Only {@link DoubleValue}s are allowed and
     * the y column selection box is fixed and the x column selection box
     * contains only the columns used for the model calulation.
     * 
     */
    @SuppressWarnings("unchecked")
    public LinRegLinePlotterProperties() {
        super(new Class[]{DoubleValue.class}, new Class[]{DoubleValue.class});
    }

    /**
     * Sets the target column.
     * 
     * @param targetColumnName the name of the target column
     */
    public void setTargetColumn(final String targetColumnName) {
        m_targetColumn = targetColumnName;
    }

    /**
     * Sets the columns which were used for model calculation.
     * 
     * @param includedColumns columns used for model calculation
     */
    public void setIncludedColumns(final String[] includedColumns) {
        m_includs = includedColumns;
    }

    /**
     * Updates the selection boxes with the passed
     * {@link org.knime.core.data.DataTableSpec} and sets 0 as x and the y
     * column to the target column.
     * 
     * @param spec the new {@link org.knime.core.data.DataTableSpec}
     */
    @Override
    public void update(final DataTableSpec spec) {
        int xIdx = -1;
        int yIdx = -1;
        if (m_xSelector != null && m_xSelector.getSelectedColumn() != null) {
            xIdx = spec.findColumnIndex(m_xSelector.getSelectedColumn());
        }
        // set the target column as y axis
        m_ySelector.setEnabled(true);
        if (m_ySelector != null && m_ySelector.getSelectedColumn() != null) {
            yIdx = spec.findColumnIndex(m_targetColumn);
        }
        if (xIdx == -1) {
            xIdx = 0;
        }
        if (yIdx == -1) {
            yIdx = 1;
        }
        update(spec, xIdx, yIdx);
    }

    /**
     * Updates the selection boxes with the new
     * {@link org.knime.core.data.DataTableSpec} and selects the passed indices.
     * Takes care, that the x column selection box only contains the columns
     * used for model calculation. For this purpose the ItemListeners of this
     * box are removed and afterwards added again in order to avoid event loops.
     * 
     * @param spec the new data table spec.
     * @param xPreSelect the x column index (-1 if unknown)
     * @param yPreSelect the y column (-1 if unknown)
     */
    @Override
    public void update(final DataTableSpec spec, final int xPreSelect,
            final int yPreSelect) {
        try {
            m_xSelector.update(spec, spec.getColumnSpec(xPreSelect).getName(),
                    true);
            m_ySelector.update(spec, spec.getColumnSpec(yPreSelect).getName(),
                    true);
            // store the old selected one
            Object oldSelected = m_xSelector.getSelectedItem();
            // suppress events
            ItemListener[] listeners = m_xSelector.getItemListeners();
            for (ItemListener listener : listeners) {
                m_xSelector.removeItemListener(listener);
            }
            if (m_includs != null) {              
                // cleanup -> remove all items and add only the included
                m_xSelector.removeAllItems();
                List<String> survivors = Arrays.asList(m_includs);
                for (DataColumnSpec colSpec : spec) {
                    if (!colSpec.getName().equals(m_targetColumn)
                            && survivors.contains(colSpec.getName())) {
                        m_xSelector.addItem(colSpec);
                    }
                }
                // restore the previously selected
                m_xSelector.setSelectedItem(oldSelected);
                for (ItemListener listener : listeners) {
                    m_xSelector.addItemListener(listener);
                }
            }
        } catch (NotConfigurableException e) {
            LOGGER.warn(e.getMessage(), e);
        }
        DataColumnSpec x = (DataColumnSpec)m_xSelector.getSelectedItem();
        DataColumnSpec y = (DataColumnSpec)m_ySelector.getSelectedItem();
        m_ySelector.setEnabled(false);
        updateRangeSpinner(x, y);
    }

}
