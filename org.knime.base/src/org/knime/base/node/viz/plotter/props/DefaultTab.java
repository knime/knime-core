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

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class DefaultTab extends PropertiesTab {

    private final JComboBox m_mouseModeSelection;

    private final JButton m_fitToScreenBtn;

    private final JColorChooser m_chooser;

    private final JButton m_chooseBackground;

    /**
     * Default Tab with mouse mode selection, fit to screen button and
     * background color chooser.
     *
     */
    public DefaultTab() {
        m_mouseModeSelection = new JComboBox();
        m_mouseModeSelection.setPreferredSize(new Dimension(COMPONENT_WIDTH,
                m_mouseModeSelection.getPreferredSize().height));
        m_fitToScreenBtn = new JButton("Fit to screen");
        m_chooser = new JColorChooser();
        m_chooser.setPreviewPanel(new JPanel());
        m_chooseBackground = new JButton("Background Color");
        Box compositeBox = Box.createHorizontalBox();
        compositeBox.setBorder(BorderFactory
                .createEtchedBorder(EtchedBorder.RAISED));
        compositeBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        compositeBox.add(new JLabel("Mouse Mode"));
        compositeBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        compositeBox.add(m_mouseModeSelection);
        compositeBox.add(Box.createHorizontalStrut(SPACE));
        compositeBox.add(m_fitToScreenBtn);
        compositeBox.add(Box.createHorizontalStrut(SPACE));
        compositeBox.add(m_chooseBackground);
        compositeBox.add(Box.createHorizontalStrut(SMALL_SPACE));
        add(compositeBox);
    }
    
    /**
     * 
     * @see org.knime.base.node.viz.plotter.props.PropertiesTab#getDefaultName()
     */
    @Override
    public String getDefaultName() {
        return "Default Settings";
    }

    /**
     * 
     * @return the button triggering the color chooser dialog.
     */
    public JButton getChooseBackgroundButton() {
        return m_chooseBackground;
    }

    /**
     * 
     * @return the color chooser for the background color.
     */
    public JColorChooser getColorChooser() {
        return m_chooser;
    }

    /**
     * 
     * @return the combo box for the mouse mode.
     */
    public JComboBox getMouseSelectionBox() {
        return m_mouseModeSelection;
    }

    /**
     * 
     * @return the button to trigger fit to screen
     */
    public JButton getFitToScreenButton() {
        return m_fitToScreenBtn;
    }

}
