/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
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
 *   24.08.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
    
    /**
     * 
     * @return the check box for antialiasing
     */
    public JCheckBox getAntialiasButton() {
        return m_defaultTab.getAntiAliasButton();
    }
    

}
