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
 *   24.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;

import org.knime.base.node.viz.plotter.props.DefaultTab;

/**
 * The properties contains elements to interact with the view. 
 * It is organized in tabs, i.e. the properties can be grouped into different 
 * tabs and added using the {@link #addTab(String, java.awt.Component)} method.
 * This class adds the {@link org.knime.base.node.viz.plotter.props.DefaultTab} 
 * which contains the elements for selecting the mouse mode, fit the view to 
 * the viewport and change the background color of the drawing pane.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class AbstractPlotterProperties extends JTabbedPane {
    
    private final DefaultTab m_defaultTab;
    
    /**
     * 
     *
     */
    public AbstractPlotterProperties() {
        m_defaultTab = new DefaultTab();
        addTab(m_defaultTab.getDefaultName(), m_defaultTab);
    }
    
    /**
     * 
     * @return the button triggering the color chooser dialog.
     */
    public JButton getChooseBackgroundButton() {
        return m_defaultTab.getChooseBackgroundButton();
    }
    
    /**
     * 
     * @return the color chooser for the background color.
     */
    public JColorChooser getColorChooser() {
        return m_defaultTab.getColorChooser();
    }
    
    /**
     * 
     * @return the combo box for the mouse mode.
     */
    public JComboBox getMouseSelectionBox() {
        return m_defaultTab.getMouseSelectionBox();
    }
    
    /**
     * 
     * @return the button to trigger fit to screen
     */
    public JButton getFitToScreenButton() {
        return m_defaultTab.getFitToScreenButton();
    }
    

}
