/*
 * ------------------------------------------------------------------
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
 * 
 * History
 *   13.02.2007 (rs): created
 */
package org.knime.timeseries.node.display.barchart;

import java.awt.Color;
import java.util.Map;
import org.knime.base.node.viz.plotter.AbstractPlotterProperties;
import org.knime.base.node.viz.plotter.props.ColorLegendTab;

/**
 * One tab for the color legend with the possibility to change the color for 
 * each column, one tab to set whether missing values should be interpolated 
 * or not and one tab to adjust dot size and line thickness and to select
 * whether to show or hide the dots.
 * 
 * @see org.knime.base.node.viz.plotter.props.ColorLegendTab
 * @see org.knime.base.node.viz.plotter.props.InterpolationTab
 * @see org.knime.base.node.viz.plotter.props.LinePlotterAppearanceTab
 * 
 * @author Rosaria Silipo
 */
public class BarChartProperties extends AbstractPlotterProperties {

    private final ColorLegendTab m_colorLegend;
    
    /**
     * BarChart Properties include only a Tab for the color legend
     *
     */

    @SuppressWarnings("unchecked")
    public BarChartProperties() {
        super();
        m_colorLegend = new ColorLegendTab();
        addTab(m_colorLegend.getDefaultName(), m_colorLegend);
    }
        
    /**
     * Updates the colorLegend.
     * 
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
