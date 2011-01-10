/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
package org.knime.base.node.viz.plotter.parcoord;

import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;

import org.knime.base.node.viz.plotter.columns.MultiColumnPlotterProperties;
import org.knime.base.node.viz.plotter.props.ParallelCoordinateAppearanceTab;
import org.knime.base.node.viz.plotter.props.ParallelCoordinatesMissingValueTab;
import org.knime.core.data.DataValue;

/**
 * In addition to the 
 * {@link org.knime.base.node.viz.plotter.columns.MultiColumnPlotterProperties}
 * a tab to select how to handle missing values and a tab to adjust dot size,
 * line thickness and select whether to show or hide dots is provided.
 * 
 * @see org.knime.base.node.viz.plotter.props.ParallelCoordinatesMissingValueTab
 * @see org.knime.base.node.viz.plotter.props.ParallelCoordinateAppearanceTab
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ParallelCoordinatePlotterProperties extends
        MultiColumnPlotterProperties {
    
    private final ParallelCoordinateAppearanceTab m_appearance;
    
    private final ParallelCoordinatesMissingValueTab m_missingValues;
    

    

    /**
     * Default tab, column selection tab, missing value handling and appearance.
     */
    @SuppressWarnings("unchecked")
    public ParallelCoordinatePlotterProperties() {
        super(new Class[]{DataValue.class});
        m_missingValues = new ParallelCoordinatesMissingValueTab();
        addTab(m_missingValues.getDefaultName(), m_missingValues);

        m_appearance = new ParallelCoordinateAppearanceTab();
        addTab(m_appearance.getDefaultName(), m_appearance);
        
    }
    
    /**
     * 
     * @return the checkbox whether the dots should be painted.
     */
    public JCheckBox getShowDotsBox() {
        return m_appearance.getShowDotsBox();
    }
    
    /**
     * 
     * @return the chekbox whether to draw the lines as curves.
     */
    public JCheckBox getDrawCurvesBox() {
        return m_appearance.getDrawCurvesBox();
    }
    
    /**
     * 
     * @return checkbox for skipping the whole row
     */
    public JRadioButton getSkipRowButton() {
        return m_missingValues.getSkipRowButton();
    }
    
    /**
     * 
     * @return the checkbox for skipping only the missing value
     */
    public JRadioButton getSkipValueButton() {
        return m_missingValues.getSkipValueButton();
    }
    
    /**
     * 
     * @return checkbox for adding a missing value point on coordinate
     */
    public JRadioButton getShowMissingValsButton() {
        return m_missingValues.getShowMissingValsButton();
    }
    
    /**
     * 
     * @return the spinner for the line thickness adjustment.
     */
    public JSpinner getThicknessSpinner() {
        return m_appearance.getThicknessSpinner();
    }

            

}
