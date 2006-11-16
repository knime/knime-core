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
 *   15.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.line;

import java.awt.Color;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;

import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.base.node.viz.plotter.props.ColorLegendTab;
import org.knime.base.node.viz.plotter.props.ColumnFilterTab;
import org.knime.base.node.viz.plotter.props.InterpolationTab;
import org.knime.base.node.viz.plotter.props.LinePlotterAppearanceTab;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.util.ColumnFilterPanel;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LinePlotterProperties extends AbstractPlotterProperties {

    private final ColorLegendTab m_colorLegend;
    
    private final InterpolationTab m_missingValues;
    
    private final LinePlotterAppearanceTab m_appearance;
    
    private final ColumnFilterTab m_columnFilter;
    

    
    /**
     * 
     *
     */
    public LinePlotterProperties() {
        super();
        m_columnFilter = new ColumnFilterTab(new Class[]{DoubleValue.class});
        addTab(m_columnFilter.getDefaultName(), m_columnFilter);
        m_colorLegend = new ColorLegendTab();
        addTab(m_colorLegend.getDefaultName(), m_colorLegend);
        m_missingValues = new InterpolationTab();
        addTab(m_missingValues.getDefaultName(), m_missingValues);
        m_appearance = new LinePlotterAppearanceTab();
        addTab(m_appearance.getDefaultName(), m_appearance);
    }
    
    /**
     * Updates the column filtering with a new {@link DataColumnSpec}.
     * @param spec the data table spec.
     * @param selected the former selected columns.
     */
    public void updateColumnSelection(final DataTableSpec spec, 
            final Set<String> selected) {
        m_columnFilter.updateColumnSelection(spec, selected);
    }
    
    /**
     * 
     * @return the column filter.
     */
    public ColumnFilterPanel getColumnFilter() {
        return m_columnFilter.getColumnFilter();
    }
    
    /**
     * 
     * @return the check box for the show/ hide dots option.
     */
    public JCheckBox getShowDotsBox() {
        return m_appearance.getShowDotsBox();
    }
    
    /**
     * 
     * @return the checkbox for interpolation.
     */
    public JCheckBox getInterpolationCheckBox() {
        return m_missingValues.getInterpolationCheckBox();
    }

    /**
     * 
     * @return the spinner for the line thickness.
     */
    public JSpinner getThicknessSpinner() {
        return m_appearance.getThicknessSpinner();
    }
    
    /**
     * 
     * @return the spinner for the dot size.
     */
    public JSpinner getDotSizeSpinner() {
        return m_appearance.getDotSizeSpinner();
    }
    
    /**
     * Updates the colorLegend.
     * @param colorMapping column name, color
     */
    public void updateColorLegend(final Map<String, Color>colorMapping) {
        // update the color legend here
        m_colorLegend.update(colorMapping);
    }
    
    
    /**
     * 
     * @return the color legend.
     */
    public ColorLegendTab getColorLegend() {
        return m_colorLegend;
    }
}
