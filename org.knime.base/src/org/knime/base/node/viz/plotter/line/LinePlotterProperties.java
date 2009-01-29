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
 *   15.09.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.line;

import java.awt.Color;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;

import org.knime.base.node.viz.plotter.columns.MultiColumnPlotterProperties;
import org.knime.base.node.viz.plotter.props.ColorLegendTab;
import org.knime.base.node.viz.plotter.props.InterpolationTab;
import org.knime.base.node.viz.plotter.props.LinePlotterAppearanceTab;
import org.knime.core.data.DoubleValue;

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
 * @author Fabian Dill, University of Konstanz
 */
public class LinePlotterProperties extends MultiColumnPlotterProperties {

    private final ColorLegendTab m_colorLegend;
    
    private final InterpolationTab m_missingValues;
    
    private final LinePlotterAppearanceTab m_appearance;
    
    /**
     * 
     *
     */
    @SuppressWarnings("unchecked")
    public LinePlotterProperties() {
        super(new Class[]{DoubleValue.class});
        m_colorLegend = new ColorLegendTab();
        addTab(m_colorLegend.getDefaultName(), m_colorLegend);
        m_missingValues = new InterpolationTab();
        addTab(m_missingValues.getDefaultName(), m_missingValues);
        m_appearance = new LinePlotterAppearanceTab();
        addTab(m_appearance.getDefaultName(), m_appearance);
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
