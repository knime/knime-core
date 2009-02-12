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

import org.knime.base.node.viz.plotter.line.LinePlotter;

/**
 * Provides three checkboxes, one to show or hide the dots, one for the line
 * thickness and one for the dot size.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class LinePlotterAppearanceTab extends PropertiesTab {
    
    private final JSpinner m_thickness;
    
    private final JSpinner m_dotSize;
    
    private final JCheckBox m_showDots;
    
    /**
     * Creates the tab with a show/hide dots checkbox, a dot size spinner
     * and a line thickness spinner.
     *
     */
    public LinePlotterAppearanceTab() {
        m_showDots = new JCheckBox("Show / Hide dots", true);
        m_thickness = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        m_dotSize = new JSpinner(new SpinnerNumberModel(LinePlotter.SIZE,
                1, 1000, 1));
        Box appBox = Box.createHorizontalBox();
        appBox.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_showDots);
        appBox.add(Box.createHorizontalStrut(SPACE));
        appBox.add(new JLabel("Thickness:"));
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_thickness);
        appBox.add(Box.createHorizontalStrut(SPACE));
        appBox.add(new JLabel("Dot Size:"));
        appBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        appBox.add(m_dotSize);
        appBox.add(Box.createHorizontalStrut(SPACE));
        add(appBox);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultName() {
        return "Appearance";
    }
    
    
    /**
     * 
     * @return the check box for the show/ hide dots option.
     */
    public JCheckBox getShowDotsBox() {
        return m_showDots;
    }
    

    /**
     * 
     * @return the spinner for the line thickness.
     */
    public JSpinner getThicknessSpinner() {
        return m_thickness;
    }
    
    /**
     * 
     * @return the spinner for the dot size.
     */
    public JSpinner getDotSizeSpinner() {
        return m_dotSize;
    }

}
