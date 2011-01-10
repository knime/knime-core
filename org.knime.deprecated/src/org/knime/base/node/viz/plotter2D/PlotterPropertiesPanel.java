/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.viz.plotter2D;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * This implements a panel with the basis elements allowing the user to set the
 * properties of the plot view. It can be extended if more property functions
 * must be available.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class PlotterPropertiesPanel extends JPanel {
    
//    private static final NodeLogger LOGGER = NodeLogger.getLogger(
//            PlotterPropertiesPanel.class); 

    private AbstractPlotter2D m_plotter;

    private JToggleButton m_zoomToggleButton;

    
    private JColorChooser m_chooser;
    
    

    /**
     * Creates a property pane for the plot view.
     * 
     */
    public PlotterPropertiesPanel() {
        // the button to fit the plotter in the whole window
        JButton fitScreen = new JButton("Fit to window");
        fitScreen.setMaximumSize(new Dimension(100, 20));
        fitScreen.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                m_plotter.fitToScreen(false);
            }
        });
        final JCheckBox crossHair = new JCheckBox("Use crosshair cursor", true);
        crossHair.addItemListener(new ItemListener() {
            /**
             * {@inheritDoc}
             */
            public void itemStateChanged(final ItemEvent arg0) {
                m_plotter.setCrosshairCursorEnabled(
                        crossHair.isSelected());
            }
            
        });
        
        m_chooser = new JColorChooser();
        m_chooser.setPreviewPanel(new JPanel());
        
        final ActionListener okListener = new ActionListener() {
            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent arg0) {
                Color newBackground = m_chooser.getColor();
                if (newBackground != null) {
                    m_plotter.getDrawingPane().setBackground(newBackground);
                }
                
            }
        };
        JButton chooseBackground = new JButton("Choose Background Color");
        chooseBackground.addActionListener(new ActionListener() {

            /**
             * {@inheritDoc}
             */
            public void actionPerformed(final ActionEvent arg0) {
                JDialog dialog = JColorChooser.createDialog(
                        PlotterPropertiesPanel.this,
                        "Select background color", true, m_chooser, 
                        okListener, null);
                dialog.setVisible(true);
                
            }
            
        });
        
        
        // create a toggle button as alternative for the radio buttons
        m_zoomToggleButton = new JToggleButton(AbstractPlotter2D.getZoomIcon());
//        mouseModePanel.add(m_zoomToggleButton);

        m_zoomToggleButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                if (m_zoomToggleButton.isSelected()) {
                    m_plotter.setZoomMode(true);
                } else {
                    m_plotter.setZoomMode(false);
                }
            }
        }); 
              
        
        // first box: fit to window and zoom
        Box zoomBox = Box.createVerticalBox();
        zoomBox.add(fitScreen);
        zoomBox.add(m_zoomToggleButton);
        zoomBox.add(Box.createVerticalGlue());
        zoomBox.add(Box.createHorizontalGlue());
        // second box: background color and crosshair cursor
        Box colorAndCursor = Box.createVerticalBox();
        colorAndCursor.add(chooseBackground);
        colorAndCursor.add(crossHair);
        colorAndCursor.add(Box.createVerticalGlue());
        colorAndCursor.add(Box.createHorizontalGlue());
        
        Box defaultBox = Box.createHorizontalBox();
        defaultBox.setBorder(BorderFactory.createTitledBorder(
                "Display Settings:"));
        defaultBox.add(zoomBox);
        defaultBox.add(colorAndCursor);

        // create overall panel
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(defaultBox);
    }

    /**
     * Adds an additional component to the properties panel.
     * 
     * @param component the additional component to add
     * 
     */
    public void addPropertiesComponent(final Component component) {
        add(Box.createHorizontalGlue());
        add(component);
        // freeze the properties
        Dimension dim = new Dimension(Integer.MAX_VALUE, 
                getPreferredSize().height);
        setMaximumSize(dim);
    }


    /**
     * Sets the scatterplot for which these properties panel is intended.
     * 
     * @param plotter the scatterplot
     */
    protected void setPlotter(final AbstractPlotter2D plotter) {
        m_plotter = plotter;
    }

    /**
     * @return the scatterplot for which these properties panel is intended.
     */
    public AbstractPlotter2D getPlotter() {

        return m_plotter;
    }
    
    /*
     * 
     * @param outerSize the new size the properites panel should be adapted to.
     *
    public final void updateLayout(final Dimension outerSize) {
        LOGGER.debug("avail.: " + outerSize.width
                + " props.: " + getPreferredSize().width);
        if (outerSize.width < getPreferredSize().width) {
            LOGGER.debug("laying out at Y");
            Component[] comps = getComponents();
            removeAll();
            setLayout(new BoxLayout(m_compositePanel, 
                    BoxLayout.Y_AXIS));
            for (Component comp : comps) {
                m_compositePanel.add(comp);
            }
        } else {
            LOGGER.debug("laying out at X");
            Component[] comps = m_compositePanel.getComponents();
            m_compositePanel.removeAll();
            m_compositePanel.setLayout(new BoxLayout(m_compositePanel, 
                    BoxLayout.X_AXIS));
            for (Component comp : comps) {
                m_compositePanel.add(comp);
            }            
        }
        revalidate(); 
    }
    */
    

    /**
     * Sets the zoom mode.
     * 
     * @param zoomMode true if the zoom mode should be activated
     */
    void setZoomMode(final boolean zoomMode) {

        if (zoomMode) {
            m_zoomToggleButton.setSelected(true);
            m_plotter.setZoomMode(true);
        } else {
            m_zoomToggleButton.setSelected(false);
            m_plotter.setZoomMode(false);
        }
        validate();
        repaint();
    }
    
}
