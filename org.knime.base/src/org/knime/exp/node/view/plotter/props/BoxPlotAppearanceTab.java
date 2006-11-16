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
package org.knime.exp.node.view.plotter.props;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.border.EtchedBorder;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class BoxPlotAppearanceTab extends PropertiesTab {

    private final JCheckBox m_normalize;
    
    /**
     * Creates a box with a checkbox (normalize).
     *
     */
    public BoxPlotAppearanceTab() {
        m_normalize = new JCheckBox("Normalize", false);
        javax.swing.Box appBox = javax.swing.Box.createHorizontalBox();
        appBox.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        appBox.add(javax.swing.Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_normalize);
        appBox.add(javax.swing.Box.createHorizontalStrut(SMALL_SPACE)); 
        add(appBox);
    }
    
    /**
     * 
     * @return the checkbox to force normalized presentation.
     */
    public JCheckBox getNormalizeCheckBox() {
        return m_normalize;
    }
    
    /**
     * 
     * @see org.knime.exp.node.view.plotter.props.PropertiesTab#getDefaultName()
     */
    @Override
    public String getDefaultName() {
        return "Appearance";
    }
}
