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
 * This is the default tab added by the 
 * {@link org.knime.base.node.viz.plotter.AbstractPlotterProperties} that makes 
 * the current mouse mode selectable, provides a "fit to screen" button
 * (which fits the drawing pane into the viewport) and a 
 * "change background color" button. For normal functionality this tab should 
 * be always present in the plotter. It is thus recommended to always extend the
 * {@link org.knime.base.node.viz.plotter.AbstractPlotterProperties} and only
 * add the additional functionality.
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
        m_fitToScreenBtn = new JButton("Fit to size");
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
     * {@inheritDoc}
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
