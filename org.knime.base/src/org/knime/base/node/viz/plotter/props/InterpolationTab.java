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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.props;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.border.EtchedBorder;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class InterpolationTab extends PropertiesTab {
    
    private final JCheckBox m_interpolate;
    
    /**
     * Creates a box with a checkbox (interpolate missing values).
     *
     */
    public InterpolationTab() {
        m_interpolate = new JCheckBox("Interpolate missing values", false);
        Box missBox = Box.createHorizontalBox();
        missBox.setBorder(BorderFactory.createEtchedBorder(
                EtchedBorder.RAISED));
        missBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        missBox.add(m_interpolate);
        missBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        add(missBox);
    }
    
    /**
     * 
     * @return the checkbox for interpolation.
     */
    public JCheckBox getInterpolationCheckBox() {
        return m_interpolate;
    }
    
    /**
     * 
     * @see org.knime.base.node.viz.plotter.props.PropertiesTab#getDefaultName()
     */
    @Override
    public String getDefaultName() {
        return "Missing Values";
    }

}
