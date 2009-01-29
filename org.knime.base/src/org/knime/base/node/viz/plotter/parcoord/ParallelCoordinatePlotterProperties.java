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
