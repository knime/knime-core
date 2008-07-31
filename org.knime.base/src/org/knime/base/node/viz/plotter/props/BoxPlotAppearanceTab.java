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
import javax.swing.JCheckBox;
import javax.swing.border.EtchedBorder;

/**
 * Provides a checkbox if the visualization of the data should be normalized 
 * or not. DefaultName is "Appearance".
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
        m_normalize = new JCheckBox("Normalize " 
                + "(with respect to min/max values of the domain)", false);
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
     * {@inheritDoc}
     */
    @Override
    public String getDefaultName() {
        return "Appearance";
    }
}
