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
