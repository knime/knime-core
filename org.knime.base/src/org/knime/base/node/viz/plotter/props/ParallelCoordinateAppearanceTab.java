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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.props;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EtchedBorder;


/**
 * Adds a box with "Show / Hide Dots" check box and a checkbox to draw curves
 * and a checkbox for the line thickness.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class ParallelCoordinateAppearanceTab extends PropertiesTab {
    
    private JCheckBox m_showDotsBox;
    private JCheckBox m_drawCurves;
    private JSpinner m_lineThickness;
    
    /**
     * Adds a box with "Show / Hide Dots" check box and a checkbox
     * to draw curves and a checkbox for the line thickness.
     *
     */
    public ParallelCoordinateAppearanceTab() {
        m_showDotsBox = new JCheckBox("Show / Hide Dots", true);
        m_drawCurves = new JCheckBox("Draw Curves instead of lines", false);
        m_lineThickness = new JSpinner(
                new SpinnerNumberModel(2, 1, 999, 1));
        Box appBox = Box.createHorizontalBox();
        appBox.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_showDotsBox);
        appBox.add(Box.createHorizontalStrut(SPACE));
        appBox.add(m_drawCurves);
        appBox.add(Box.createHorizontalStrut(SPACE));
        appBox.add(new JLabel("Line thickness:"));
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_lineThickness);
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        add(appBox);
    }
    
    /**
     * 
     * @return the checkbox whether the dots should be painted.
     */
    public JCheckBox getShowDotsBox() {
        return m_showDotsBox;
    }
    
    /**
     * 
     * @return the chekbox whether to draw the lines as curves.
     */
    public JCheckBox getDrawCurvesBox() {
        return m_drawCurves;
    }
    
    /**
     * 
     * @return the spinner for the line thickness adjustment.
     */
    public JSpinner getThicknessSpinner() {
        return m_lineThickness;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultName() {
        return "Appearance";
    }

}
