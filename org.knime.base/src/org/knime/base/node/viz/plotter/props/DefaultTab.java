/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2010
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
 *   03.10.2006 (Fabian Dill): created
 */
package org.knime.base.node.viz.plotter.props;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
    
    private final JCheckBox m_antialias;

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
        m_antialias = new JCheckBox("Use anti-aliasing");
        
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
        compositeBox.add(Box.createHorizontalStrut(SPACE));
        compositeBox.add(m_antialias);
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
    
    /**
     * 
     * @return the check box for anti-aliasing
     */
    public JCheckBox getAntiAliasButton() {
        return m_antialias;
    }

}
